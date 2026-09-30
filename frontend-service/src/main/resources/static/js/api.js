const SellzonAPI = (() => {
    /* =========================================================
        * Sellzon Microservice URLs
        *
        * Frontend UI:
        *   8080
        *
        * Backend services:
        *   Identity  : 8081
        *   Catalog   : 8082
        *   Order     : 8083
        *   Cart      : 8084
        *   Inventory : 8085
        *   Payment   : 8086
       ========================================================= */
    const SERVICE_URLS = {
        identity: "http://localhost:8081",
        catalog: "http://localhost:8082"
    };

    function resolveUrl(url) {
        if (!url || /^https?:\/\//i.test(url)) return url;

        // Identity Service - authentication and users
        if (
            url.startsWith("/api/auth/") ||
            url === "/api/users" ||
            url.startsWith("/api/users/")
        ) {
            return `${SERVICE_URLS.identity}${url}`;
        }

        // Catalog Service - customer catalog, seller catalog and admin catalog
        if (
            url.startsWith("/api/products") ||
            url.startsWith("/api/categories/tree") ||
            url.startsWith("/api/admin/categories") ||
            url.startsWith("/api/admin/attributes") ||
            url.startsWith("/api/admin/category-attributes") ||
            url.startsWith("/api/seller/products") ||
            url.startsWith("/api/seller/categories")
        ) {
            return `${SERVICE_URLS.catalog}${url}`;
        }

        // Other services will be moved one port at a time.
        return url;
    }

    function catalogAssetUrl(url) {

        if (!url) {
            return "";
        }

        if (/^https?:\/\//i.test(url)) {
            return url;
        }

        if (url.startsWith("/uploads/products/")) {
            return `${SERVICE_URLS.catalog}${url}`;
        }

        return url;
    }

    function identityUrl(path) {
        return `${SERVICE_URLS.identity}${path}`;
    }
    const ACCESS_TOKEN_KEY = "sellzonAccessToken";
    const REFRESH_TOKEN_KEY = "sellzonRefreshToken";
    const USER_KEY = "sellzonUser";

    function getPortal() {
        const host = window.location.hostname.toLowerCase();
        if (host === "admin.localhost") return "admin";
        if (host === "partner.localhost") return "partner";
        return "customer";
    }

    function getExpectedRole() {
        return { customer: "CUSTOMER", partner: "SELLER", admin: "ADMIN" }[getPortal()];
    }

    function getLoginPath() {
        return {
            customer: "/customer/login.html",
            partner: "/partner/login.html",
            admin: "/admin/login.html"
        }[getPortal()];
    }

    function getHomePath() {
        return {
            customer: "/customer/index.html",
            partner: "/partner/dashboard.html",
            admin: "/admin/dashboard.html"
        }[getPortal()];
    }

    function getForgotPasswordPath() {
        return {
            customer: "/customer/forgot-password.html",
            partner: "/partner/forgot-password.html",
            admin: "/admin/forgot-password.html"
        }[getPortal()];
    }

    function getResetPasswordPath() {
        return {
            customer: "/customer/reset-password.html",
            partner: "/partner/reset-password.html",
            admin: "/admin/reset-password.html"
        }[getPortal()];
    }

    async function request(url, options = {}, retry = true) {
        const headers = new Headers(options.headers || {});
        headers.set("Accept", "application/json");

        if (options.body && !headers.has("Content-Type") && !(options.body instanceof FormData)) {
            headers.set("Content-Type", "application/json");
        }

        /*
         * Do not send an old/expired access token to public authentication
         * endpoints. Spring Security's bearer filter can reject an expired
         * token before the login/refresh request reaches the controller.
         */
        const publicEndpoint =
            url.startsWith("/api/auth/") ||
            url === "/api/users" ||
            url.startsWith("/api/users/public-profile");

        const accessToken = getAccessToken();

        if (accessToken && !publicEndpoint) {
            headers.set("Authorization", `Bearer ${accessToken}`);
        } else {
            headers.delete("Authorization");
        }

        const response = await fetch(resolveUrl(url), { ...options, headers });

        if (response.status === 401 && retry && getRefreshToken()) {
            const refreshed = await refreshAccessToken();
            if (refreshed) return request(url, options, false);

            clearAuth();
            if (!isAuthPage()) window.location.href = `${getLoginPath()}?expired=true`;
            throw new Error("Your session has expired. Please login again.");
        }

        return response;
    }

    async function json(url, options = {}, retry = true) {
        const response = await request(url, options, retry);
        const contentType = response.headers.get("content-type") || "";
        let data = null;
        if (contentType.includes("application/json")) data = await response.json();
        return { response, data };
    }

    async function refreshAccessToken() {
        const refreshToken = getRefreshToken();
        if (!refreshToken) return false;

        try {
            const response = await fetch(identityUrl("/api/auth/refresh"), {
                method: "POST",
                headers: {"Content-Type": "application/json", "Accept": "application/json"},
                body: JSON.stringify({refreshToken})
            });
            if (!response.ok) { clearAuth(); return false; }
            saveAuth(await response.json());
            return true;
        } catch (_) { clearAuth(); return false; }
    }

    function saveAuth(data) {
        if (!data?.accessToken || !data?.refreshToken) throw new Error("Invalid authentication response.");
        sessionStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
        sessionStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
            userId: data.userId, email: data.email, role: data.role, expiresIn: data.expiresIn
        }));
    }

    async function logout() {
        const refreshToken = getRefreshToken();
        try {
            if (refreshToken) {
                await fetch(identityUrl("/api/auth/logout"), {
                    method: "POST",
                    headers: {"Content-Type":"application/json","Accept":"application/json"},
                    body: JSON.stringify({refreshToken})
                });
            }
        } catch (error) { console.warn("Logout request failed:", error); }
        finally { clearAuth(); }
    }

    function clearAuth() {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage.removeItem(REFRESH_TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);
    }
    function getAccessToken() { return sessionStorage.getItem(ACCESS_TOKEN_KEY); }
    function getRefreshToken() { return sessionStorage.getItem(REFRESH_TOKEN_KEY); }

    function getUser() {
        const raw = sessionStorage.getItem(USER_KEY);
        if (!raw) return null;
        try { return JSON.parse(raw); }
        catch (_) { clearAuth(); return null; }
    }

    function isAuthenticated() { return Boolean(getAccessToken() && getRefreshToken()); }
    function normalizeRole(role) { return String(role || "").replace(/^ROLE_/,"").toUpperCase(); }

    function requireAuthentication() {
        if (!isAuthenticated()) {
            window.location.href = getLoginPath();
            return false;
        }
        return true;
    }

    function requireRole(role = getExpectedRole()) {
        if (!requireAuthentication()) return false;
        const user = getUser();
        if (normalizeRole(user?.role) !== role) {
            clearAuth();
            window.location.href = `${getLoginPath()}?access=denied`;
            return false;
        }
        return true;
    }

    function isAuthPage() {
        const path = window.location.pathname;
        return ["/login.html","/register.html","/forgot-password.html","/reset-password.html","/oauth2/callback.html"]
            .some(s => path.endsWith(s));
    }

    return {
        request,json,saveAuth,resolveUrl,catalogAssetUrl,identityUrl,refreshAccessToken,logout,clearAuth,getAccessToken,getRefreshToken,getUser,
        isAuthenticated,requireAuthentication,requireRole,getPortal,getExpectedRole,getLoginPath,getHomePath,
        getForgotPasswordPath,getResetPasswordPath,normalizeRole
    };
})();
