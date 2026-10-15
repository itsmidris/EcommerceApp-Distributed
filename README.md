# Sellzon --- Local Development, Docker, Environment & Recovery Notes

This README records the current working setup for
**EcommerceApp-Distributed / Sellzon**. It covers Git updates,
environment variables, frontend API routing, Docker commands, RAM
recovery, health checks and production safety.

> **Safety:** Local development and the production VPS are separate
> environments. Do not run local commands on the VPS unintentionally.
> Never use `docker compose down -v` as a routine restart; it can delete
> named volumes and data.

## 1. Project locations

**Local Windows repository**

``` text
D:\Imran\EcommerceApp-Distributed
```

**Production VPS repository**

``` text
/home/admin/apps/Sellzon
```

Run local commands in PowerShell from the Windows repository root. VPS
commands must be run in the SSH terminal and only when intentionally
working on production.

## 2. Known local endpoints

  Component                             Address
  ------------------------------------- ------------------------------------------------
  Storefront                            `http://localhost:8080/customer/index.html`
  Admin login                           `http://admin.localhost:8080/admin/login.html`
  API Gateway                           `http://localhost:8000`
  Gateway health                        `http://localhost:8000/actuator/health`
  Eureka                                `http://localhost:8761`
  Config Server inside Docker network   `http://config-server:8888`
  Windows PostgreSQL host               `localhost:5432`
  PostgreSQL from Docker                `host.docker.internal:5432`
  Kafka from Docker                     `kafka:9093`

The stack includes Eureka, Config Server, Identity, Catalog, Inventory,
Cart, Order, Payment, API Gateway, Frontend, Kafka and Redis.
Observability services include Elasticsearch, Kibana, Logstash,
Prometheus and Grafana.

## 3. Pull updates from GitHub safely

Repository: `https://github.com/itsmidris/EcommerceApp-Distributed.git`

### Check current state

``` powershell
cd D:\Imran\EcommerceApp-Distributed
git status --short --branch
git remote -v
git branch --show-current
```

Review modified and untracked files before pulling. Preserve changes you
need.

### Optional: stash local work

If you have work that is not committed and you want to temporarily put
it aside:

``` powershell
git stash push -u -m "WIP before pulling Sellzon updates"
git stash list
```

After pulling, inspect the stash and restore it when ready:

``` powershell
git stash apply
```

Resolve conflicts carefully. Do not use `git reset --hard` or
`git clean -fd` as a shortcut.

### Fetch and pull

``` powershell
git fetch origin
git status -sb
```

If your intended branch is `main` and your local branch is `main`:

``` powershell
git pull --ff-only origin main
```

If the project uses a different branch, replace `main` with that branch.
`--ff-only` prevents an unexpected merge commit. If it fails because
branches have diverged, stop and inspect rather than forcing the pull.

Verify afterward:

``` powershell
git status --short --branch
git log -5 --oneline
```

## 4. Environment variables and `.env.example`

### File purposes

  -----------------------------------------------------------------------
  File                    Purpose                 Commit?
  ----------------------- ----------------------- -----------------------
  `.env.example`          Safe template of        Yes
                          required variable names 
                          and placeholders        

  `.env.local`            Local development       **No**
                          settings and            
                          credentials             

  `.env.prod`             Production VPS settings **No**

  `.env`                  Default Compose         **No**
                          environment, if used    
  -----------------------------------------------------------------------

Use `.env.example` as the checklist of required variables. If it is
incomplete, update it with missing **variable names and safe
placeholders only**. Do not put real secrets in it.

### Local setup

1.  Inspect `.env.example`, `docker-compose.yml`, service configuration
    and `config-repo` to identify the exact environment variable names
    used by the application.
2.  Create/update `.env.local` with those exact names.
3.  For Windows PostgreSQL accessed from Docker, use
    `host.docker.internal:5432`, not `localhost:5432` inside a
    container.
4.  The local setup expects these databases: `identity_db`,
    `catalog_db`, `inventory_db`, `order_db`, `payment_db`.
5.  Configure development values for database credentials,
    JWT/signing-key configuration, email/SMTP, Google OAuth, Cloudinary,
    internal-service authentication and Razorpay test settings as
    required by the current code.
6.  Never use production credentials for local development.

Validate before starting:

``` powershell
docker compose --env-file .env.local config --quiet
docker compose --env-file .env.local config --services
```

The first command should complete without a configuration error. The
second lists the Compose service names.

**Do not invent variable names.** Names in `.env.local` must match the
names referenced by Compose/application configuration. Update
`.env.example` whenever a new required variable is introduced.

### Secret safety

-   Never commit `.env`, `.env.local`, `.env.prod`, private keys, tokens
    or real credentials.
-   Do not paste secret-bearing files or full rendered Compose
    configuration into public places.
-   Check the Git state before pushing:

``` powershell
git status --short
git ls-files .env .env.local .env.prod
git check-ignore .env .env.local .env.prod
```

Real environment files should be ignored and not tracked. If a secret
was shared or committed, rotate/revoke it; deleting it from the file
alone is not sufficient.

## 5. Frontend `api.js`: local vs production

File:

``` text
frontend-service/src/main/resources/static/js/api.js
```

The original configuration is:

``` javascript
const SERVICE_URLS = {
    gateway: ""
};
```

An empty gateway makes relative paths such as `/api/products` go to the
same origin as the frontend.

### Why local needed a change

Local frontend is served on port `8080`; the API Gateway is exposed on
port `8000`. We verified:

-   `http://localhost:8000/api/products?page=0&size=8` → `200`.
-   `http://localhost:8000/api/categories/tree` → `200`.
-   The product API on `http://localhost:8080` → `404` before the fix.

For the current local setup, `api.js` was changed to:

``` javascript
const SERVICE_URLS = {
    gateway: "http://localhost:8000"
};
```

The frontend was rebuilt and the user confirmed the local application
worked.

### Production warning

**Do not deploy the hardcoded `http://localhost:8000` value to
production.** In a visitor's browser, `localhost` refers to that
visitor's own computer. The production VPS uses a reverse-proxy
arrangement where the empty gateway setting allows same-origin
`/api/...` requests to be routed publicly. Production product and
category API checks returned HTTP `200` with the empty setting.

Before the next deployment, make the setting environment-aware. Suitable
designs include a runtime configuration file generated per environment
or a local reverse proxy that makes frontend and API share one origin.
Test local and production separately. If a CORS error appears, configure
only the required origins/headers; do not disable security broadly.

After editing frontend static files, rebuild only the frontend:

``` powershell
docker compose --env-file .env.local up -d --build frontend-service
```

Then hard-refresh the browser with **Ctrl + F5**.

## 6. Docker commands

Run from `D:\Imran\EcommerceApp-Distributed`.

### Start minimum infrastructure

``` powershell
docker compose --env-file .env.local up -d redis kafka eureka-server config-server
```

Wait for dependencies to become healthy.

### Start application services

``` powershell
docker compose --env-file .env.local up -d identity-service catalog-service inventory-service cart-service order-service payment-service api-gateway frontend-service
```

### Status and logs

``` powershell
docker compose --env-file .env.local ps -a
docker ps
docker compose --env-file .env.local logs --tail 100 api-gateway
docker compose --env-file .env.local logs --tail 100 identity-service
docker compose --env-file .env.local logs --tail 100 catalog-service
```

Follow a service's logs live:

``` powershell
docker compose --env-file .env.local logs -f api-gateway
```

Press `Ctrl + C` to stop following logs; this does not stop the
container.

### Rebuild one service

For frontend changes:

``` powershell
docker compose --env-file .env.local up -d --build frontend-service
```

Use the appropriate service name for a backend change. Prefer rebuilding
only the changed service over rebuilding/restarting the entire stack.

### Stop and start services

Stop application services while preserving containers and volumes:

``` powershell
docker compose --env-file .env.local stop frontend-service api-gateway identity-service catalog-service inventory-service cart-service order-service payment-service
```

Start them again:

``` powershell
docker compose --env-file .env.local start frontend-service api-gateway identity-service catalog-service inventory-service cart-service order-service payment-service
```

Restart only one service:

``` powershell
docker compose --env-file .env.local restart api-gateway
```

Avoid `docker compose down -v` during routine troubleshooting.

## 7. When Docker consumes too much RAM

The full stack can be heavy, especially with Kafka and Elasticsearch.
Reduce load in this order.

### A. Inspect usage

``` powershell
docker stats
docker compose --env-file .env.local ps -a
```

Also check Docker Desktop and Windows Task Manager.

### B. Stop observability first

If you are testing login/catalog/cart and do not need logs or
dashboards, stop these optional services:

``` powershell
docker compose --env-file .env.local stop grafana prometheus kibana logstash elasticsearch
```

Restart them when needed:

``` powershell
docker compose --env-file .env.local start elasticsearch logstash kibana prometheus grafana
```

Start Elasticsearch first and allow health checks to settle. If these
containers are already stopped, no action is needed.

### C. Stop application services not needed for the current test

For a catalog-only session, for example:

``` powershell
docker compose --env-file .env.local stop payment-service order-service cart-service inventory-service
```

Checkout/order flows will not work while their required services are
stopped. Resume them with:

``` powershell
docker compose --env-file .env.local start inventory-service cart-service order-service payment-service
```

### D. Stop the whole stack safely

If Windows is becoming unresponsive:

``` powershell
docker compose --env-file .env.local stop
```

Resume later:

``` powershell
docker compose --env-file .env.local start
docker compose --env-file .env.local ps -a
```

`stop` preserves containers and named volumes. It does not erase the
Windows PostgreSQL databases.

### E. If Docker Desktop is stuck

1.  Try stopping services through Compose.
2.  Save work in other applications.
3.  Restart Docker Desktop from its UI.
4.  After it is ready, check:

``` powershell
docker version
docker ps
```

5.  Check Compose status and logs before starting services.

Do **not** use Docker Desktop **Clean / Purge data**, **Factory reset**,
`docker system prune --volumes`, or `docker compose down -v` as routine
recovery. These can delete data or resources you still need.

### Memory guidance

Start only the dependencies and services required for the feature being
tested. Keep observability off unless actively working on
monitoring/logging. Measure `docker stats` before changing JVM heaps or
Docker memory limits; do not blindly assign large heaps to every
service.

## 8. Smoke tests

### Gateway health

``` powershell
Invoke-WebRequest "http://localhost:8000/actuator/health" -UseBasicParsing |
    Select-Object StatusCode, Content
```

### Products

``` powershell
Invoke-WebRequest "http://localhost:8000/api/products?page=0&size=8" -UseBasicParsing |
    Select-Object StatusCode, Content
```

### Categories

``` powershell
Invoke-WebRequest "http://localhost:8000/api/categories/tree" -UseBasicParsing |
    Select-Object StatusCode, Content
```

The expected result for these GET requests in the current working local
setup is HTTP `200`.

### Browser checklist

-   [ ] Storefront loads.
-   [ ] Products appear.
-   [ ] Category navigation loads.
-   [ ] Admin login page loads.
-   [ ] Login succeeds with valid credentials.
-   [ ] DevTools → Network/Console has no unexpected 404/CORS errors.
-   [ ] Cart, checkout and order flows work when their required services
    are running.

Do not share passwords, access/refresh tokens, cookies or secret-bearing
headers.

## 9. Database and data protection

Local Docker services connect to PostgreSQL on Windows using
`host.docker.internal:5432`. The local databases expected by the current
setup are `identity_db`, `catalog_db`, `inventory_db`, `order_db` and
`payment_db`.

The local database is separate from production. Do not point local
services to production databases for normal development. Before a schema
change, confirm which database URL is active.

If products do not appear, check the browser request and API response
before recreating databases. Docker volumes may contain Kafka or
observability data; preserve them unless a planned cleanup has been
reviewed.

## 10. Production VPS safety

VPS path:

``` bash
cd /home/admin/apps/Sellzon
```

Production uses its own environment settings. Never copy `.env.local` or
the local `localhost:8000` API URL into production.

Read-only checks:

``` bash
docker compose ps
docker compose logs --tail 100 api-gateway
```

Previously verified public GET endpoints:

``` bash
curl -sS -o /tmp/sellzon-products-check.txt -w 'HTTP %{http_code}\n' \
  'https://sellzon.shop/api/products?page=0&size=8'

curl -sS -o /tmp/sellzon-categories-check.txt -w 'HTTP %{http_code}\n' \
  'https://sellzon.shop/api/categories/tree'
```

Both returned HTTP `200` during verification; recheck when needed
because this is not a guarantee of future availability.

Production precautions:

-   Do not change production environment files during local
    troubleshooting.
-   Do not run `docker compose down -v`.
-   Avoid a full-stack rebuild/restart when only one service changed.
-   Review `git diff` and `git status` before pushing.
-   Review local-only files such as `application-local.properties` and
    `application-vps.properties` before staging; do not push them unless
    the deployment plan explicitly requires them.
-   Prefer deploying only the changed service after checking
    compatibility and health.
-   Back up appropriately before production database/schema changes.
-   Production VPS memory has previously been limited, so avoid
    unnecessary full-stack builds/restarts.

## 11. Common troubleshooting

### Frontend loads but products/categories are empty

1.  Open DevTools → Network.
2.  Inspect the failed request URL and status.
3.  Test the same endpoint directly on port `8000`.
4.  If Gateway works but port `8080` returns 404, check `api.js` and the
    local proxy/origin design.
5.  Confirm the local setting has not accidentally been deployed to
    production.

### Login reports invalid credentials

Do not immediately reset passwords or edit the database. Inspect the
login request first:

-   `404`: check URL/path and gateway routing.
-   `401` or application-level invalid credentials: check the account
    and request contract.
-   `500`: inspect Identity and Gateway logs for the underlying
    exception.

Share only the status and sanitized error message, never credentials or
tokens.

### Docker Compose engine/HTTP errors

Check Docker Desktop's Linux engine:

``` powershell
docker version
docker ps
```

If these fail, resolve Docker Desktop first. Do not delete Docker data
to fix an engine connection error.

### A service exits after startup

``` powershell
docker compose --env-file .env.local ps -a
docker compose --env-file .env.local logs --tail 200 <service-name>
```

Replace `<service-name>` with the failing Compose service. Check the
first meaningful exception, environment variables, dependency health and
RAM pressure before rebuilding.

### Logstash hostname warnings

When Logstash is intentionally stopped, application logs may contain
DNS/connection warnings for `logstash`. If the core application works,
these may be non-fatal. Start Logstash only when using the logging
pipeline, then verify whether the warnings stop.

## 12. Daily workflow

1.  Open PowerShell in the repository.
2.  Check `git status --short --branch`.
3.  Validate `.env.local` with
    `docker compose --env-file .env.local config --quiet`.
4.  Start only required infrastructure/services.
5.  Verify Gateway health and test the feature in the browser.
6.  Check logs and `docker stats` if something fails or RAM usage rises.
7.  Stop optional observability services when finished.
8.  Review changes before committing/pushing.
9.  Never deploy local-only API URLs or local secrets to the VPS.

## 13. Pre-push checklist

-   [ ] `git status` reviewed; no work was accidentally discarded.
-   [ ] `.env`, `.env.local`, `.env.prod`, private keys and credentials
    are not tracked.
-   [ ] `.env.example` contains placeholders only.
-   [ ] Local `api.js` URL is not being pushed to production without an
    environment-aware solution.
-   [ ] `application-local.properties` and `application-vps.properties`
    reviewed and excluded if local-only.
-   [ ] Only intended files are staged.
-   [ ] Relevant local smoke tests pass.
-   [ ] Production deployment is planned separately and targets only the
    necessary service.

Review before staging:

``` powershell
git status --short
git diff --stat
git diff
git diff --cached
```

Avoid `git add .` without reviewing all changed and untracked files.

## 14. Keep this README current

Update this document whenever Compose service names/ports, required
environment variables, API routing, database hosts, JWT key mounts,
deployment steps or recovery procedures change.

### Last verified state

-   Local API Gateway product and category requests returned HTTP `200`.
-   Local frontend product API on port `8080` returned `404` before the
    gateway URL was set.
-   Local `api.js` was changed to `gateway: "http://localhost:8000"`.
-   The frontend was rebuilt and the user confirmed local testing
    worked.
-   Production frontend still uses an empty gateway value, and public
    product/category endpoints returned HTTP `200`.
