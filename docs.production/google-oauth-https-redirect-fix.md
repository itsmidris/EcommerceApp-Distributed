# Fix: Google OAuth HTTPS Redirect in Sellzon

**Date:** October 17, 2026  
**Component:** API Gateway and Identity Service  
**Status:** Fixed; verify complete Google login in the browser.

## 1. Issue

Google OAuth login initiated through the API Gateway generated an HTTP callback URL instead of HTTPS.

**Incorrect callback:**

`http://admin.sellzon.shop/login/oauth2/code/google`

**Expected callback:**

`https://admin.sellzon.shop/login/oauth2/code/google`

The direct Identity Service request generated the correct HTTPS callback, but the same request through the API Gateway generated HTTP.

## 2. Root Cause

The issue was caused by forwarded-header handling and proxy trust in Spring Cloud Gateway.

Nginx terminates public HTTPS and forwards requests to the API Gateway over internal HTTP. The original HTTPS scheme and hostname must be propagated correctly so that Identity Service can construct the correct OAuth callback URL.

The Gateway was stripping relevant forwarded headers because the incoming proxy was not configured as trusted. Consequently, Identity Service could not reliably determine the original HTTPS scheme and generated an HTTP callback.

Testing also demonstrated that a conflicting header,

`Forwarded: proto=http;host=admin.sellzon.shop`

could override the HTTPS information supplied through `X-Forwarded-Proto`.

## 3. Configuration Changes

File:

`api-gateway/src/main/resources/application.properties`

### 3.1 Preserve the hostname for OAuth requests

The OAuth initiation and callback routes use `PreserveHostHeader` to preserve the requested portal hostname.

```properties
spring.cloud.gateway.server.webflux.routes[17].filters[0]=PreserveHostHeader
spring.cloud.gateway.server.webflux.routes[17].filters[1]=SetRequestHeader=X-Forwarded-Proto, https

spring.cloud.gateway.server.webflux.routes[18].filters[0]=PreserveHostHeader
spring.cloud.gateway.server.webflux.routes[18].filters[1]=SetRequestHeader=X-Forwarded-Proto, https
```

The routes are:

- `/oauth2/**` — Google OAuth initiation.
- `/login/oauth2/**` — OAuth callback.

### 3.2 Configure forwarded headers

The following settings are retained:

```properties
spring.cloud.gateway.server.webflux.x-forwarded.enabled=true
spring.cloud.gateway.server.webflux.x-forwarded.for-enabled=true
spring.cloud.gateway.server.webflux.x-forwarded.host-enabled=true
spring.cloud.gateway.server.webflux.x-forwarded.port-enabled=true
spring.cloud.gateway.server.webflux.x-forwarded.proto-enabled=true
```

The Gateway is configured not to generate the conflicting RFC `Forwarded` header:

```properties
spring.cloud.gateway.server.webflux.forwarded.enabled=false
```

### 3.3 Trust the incoming proxy

The effective fix was configuring the Gateway's trusted-proxy setting:

```properties
spring.cloud.gateway.server.webflux.trusted-proxies=172\\..*|127\\..*
```

This pattern was used to match the Docker network and loopback addresses involved in the deployment.

**Security note:** This pattern is broad. Review and restrict it to the actual trusted proxy address ranges. Do not trust forwarded headers from arbitrary clients.

### 3.4 Gateway forwarded-header strategy

The following property was removed from the API Gateway configuration:

```properties
server.forward-headers-strategy=framework
```

Its removal was cleanup, not the confirmed primary fix.

Identity Service retains its forwarded-header strategy and OAuth redirect template:

```properties
server.forward-headers-strategy=framework
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

## 4. Deployment

Only the API Gateway was rebuilt for the configuration change:

```bash
cd ~/apps/Sellzon
docker compose up -d --build api-gateway
```

After restarting, allow Eureka service discovery to recover before testing requests.

## 5. Verification

Run the following command on the VPS to test all three portal hostnames:

```bash
for H in admin.sellzon.shop partner.sellzon.shop sellzon.shop; do
  echo "== $H"
  curl -sS -D - -o /dev/null \
    -H "Host: $H" \
    -H "X-Forwarded-Host: $H" \
    -H 'X-Forwarded-Proto: https' \
    -H 'X-Forwarded-Port: 443' \
    http://127.0.0.1:8000/oauth2/authorization/google \
    | grep -i '^location:' \
    | grep -oE 'redirect_uri=[^&]*'
done
```

Expected results:

```text
redirect_uri=https://admin.sellzon.shop/login/oauth2/code/google
redirect_uri=https://partner.sellzon.shop/login/oauth2/code/google
redirect_uri=https://sellzon.shop/login/oauth2/code/google
```

The successful test confirms that the generated callback scheme is HTTPS and that each portal hostname is preserved.

## 6. Browser Verification

The `curl` test verifies the generated redirect URL, but does not prove that the complete OAuth login flow succeeds.

Verify the following manually:

1. Open `https://admin.sellzon.shop`.
2. Initiate Google login.
3. Confirm the callback URL uses HTTPS.
4. Complete authentication and verify the user returns to the correct portal.
5. Repeat for customer and seller portals if they support Google login.

Ensure the relevant callback URLs are registered in the Google OAuth client configuration.

## 7. Security and Deployment Notes

- Keep the API Gateway bound to `127.0.0.1:8000` when Nginx is the intended public entry point.
- Restrict the trusted-proxy pattern to the actual trusted network ranges.
- Do not trust arbitrary client-supplied forwarding headers.
- Internal Docker services can continue communicating over HTTP; converting every service to HTTPS is not required to resolve this issue.
- Do not change Google OAuth credentials or unrelated services unless a separate verified problem requires it.

## 8. Troubleshooting

If the callback reverts to HTTP:

1. Check the effective `trusted-proxies` setting.
2. Verify `spring.cloud.gateway.server.webflux.forwarded.enabled=false`.
3. Confirm Nginx forwards the original host and HTTPS scheme.
4. Verify that Identity Service receives the expected forwarded headers.
5. Repeat the three-host verification test.
6. Inspect the Spring Cloud Gateway version and active configuration before changing additional properties.

Avoid speculative edits and unnecessary rebuilds.

## 9. Summary

The Google OAuth callback was incorrectly generated with HTTP because the original HTTPS scheme was not being propagated correctly through the Gateway-to-Identity request path.

Configuring trusted proxies and controlling forwarded-header behavior resolved the issue while preserving the admin, seller, and customer portal hostnames.
