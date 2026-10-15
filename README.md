# Sellzon — Complete Local Setup, Run & Test Guide

This guide walks through the full workflow for **EcommerceApp-Distributed (Sellzon)** on Windows: pull the latest code, configure environment variables, start Docker services, test the APIs and frontend, troubleshoot common issues, and keep production safe.

> **Scope:** This guide is for local development on Windows using PowerShell and Docker Desktop. Production VPS instructions are separated near the end.
>
> **Safety:** Never commit `.env`, `.env.local`, `.env.prod`, private keys, tokens, or real credentials. Do not run `docker compose down -v` as a routine fix; it can delete volumes and data.

---

## Quick workflow

1. Check Git and preserve local work.
2. Pull the intended branch from GitHub.
3. Create/check `.env.local`.
4. Validate Docker Compose configuration.
5. Start infrastructure.
6. Start application services.
7. Test Gateway, products, categories, and the browser.
8. Check logs and RAM if something fails.
9. Commit/push only intended files.
10. Deploy to the VPS separately and deliberately.

---

## 1. Prerequisites

Before starting, make sure these are available:

- Git
- Docker Desktop using the Linux container engine
- PowerShell
- The Sellzon repository
- Windows PostgreSQL if using the existing local databases

Repository:

`https://github.com/itsmidris/EcommerceApp-Distributed.git`

Local path:

`D:\Imran\EcommerceApp-Distributed`

Open PowerShell and go to the project:

```powershell
cd D:\Imran\EcommerceApp-Distributed
```

Check Docker is responding:

```powershell
docker version
docker ps
docker compose version
```

If these commands fail, fix Docker Desktop first. Do not delete Docker data as a first response to an engine error.

## 2. Pull the latest code safely

### 2.1 Check your current branch and changes

```powershell
git status --short --branch
git branch --show-current
git remote -v
```

Review all modified and untracked files. Commit or otherwise preserve work you need before pulling. Do not discard changes just to make Git pull succeed.

### 2.2 Fetch remote updates

```powershell
git fetch origin
git status -sb
```

### 2.3 Pull the correct branch

First check the branch name from `git branch --show-current`. If your intended branch is `master`, use:

```powershell
git pull --ff-only origin master
```

If your intended branch is `main`, use:

```powershell
git pull --ff-only origin main
```

Use the branch you actually work on; do not blindly switch branches. `--ff-only` avoids an unexpected merge commit. If it fails because local and remote history diverged, stop and inspect the Git state rather than forcing it.

### 2.4 Verify the pull

```powershell
git status --short --branch
git log -5 --oneline
```

If `git status` shows local changes, inspect them before continuing.

### Optional: preserve uncommitted work with a stash

Use this only when you have confirmed that stashing is appropriate:

```powershell
git stash push -u -m "WIP before pulling Sellzon updates"
git stash list
```

After pulling, restore it when ready:

```powershell
git stash apply
```

Review and resolve any conflicts carefully. Avoid `git reset --hard` and `git clean -fd` as shortcuts.

---

## 3. Configure environment variables

### 3.1 Understand the environment files

| File | Role | Commit? |
|---|---|---|
| `.env.example` | Template containing variable names and safe placeholders | Yes |
| `.env.local` | Your local development settings and credentials | **No** |
| `.env.prod` | Production settings, if maintained locally | **No** |
| `.env` | Default Compose environment, if used | **No** |

Use `.env.example` as a checklist. The exact required variables are determined by the current `docker-compose.yml` and service configuration. Do not assume the template includes every required variable without validating it.

### 3.2 Create `.env.local`

If `.env.local` already exists and your local setup works, **do not overwrite it**. Keep your existing database settings and credentials.

If it does not exist, copy the template:

```powershell
Copy-Item .env.example .env.local
```

Edit `.env.local` in your editor and replace placeholders with your own local values. Do not put production secrets in this file.

For the current local setup:

- PostgreSQL runs on Windows, so container database URLs use `host.docker.internal:5432`.
- Expected local databases: `identity_db`, `catalog_db`, `inventory_db`, `order_db`, `payment_db`.
- Docker-internal service URLs use Compose service names, not `localhost`.
- Kafka's internal address is `kafka:9093` for the current setup.
- `SELLZON_KEYS_DIR` should point to your local key directory, for example `D:/Imran/Sellzon-local-keys`, if that directory contains the expected key files.

Do not paste or commit the contents of `.env.local`.

### 3.3 Validate environment and Compose configuration

Run:

```powershell
docker compose --env-file .env.local config --quiet
```

Expected: it finishes without a configuration error.

Then list the Compose services:

```powershell
docker compose --env-file .env.local config --services
```

If validation fails, read the first error and check the variable names in `.env.local` against `docker-compose.yml`. Do not print or share a fully rendered Compose configuration publicly, because it can contain secrets.

### 3.4 Confirm environment files are not tracked

```powershell
git status --short
git ls-files .env .env.local .env.prod
git check-ignore .env .env.local .env.prod
```

The real environment files should not be tracked by Git. `.env.example` is the safe file intended for Git.

---

## 4. Start Sellzon locally

Run all commands below from:

`D:\Imran\EcommerceApp-Distributed`

### 4.1 Start Docker infrastructure

```powershell
docker compose --env-file .env.local up -d redis kafka eureka-server config-server
```

Check status:

```powershell
docker compose --env-file .env.local ps -a
```

Wait for health checks to settle before starting application services.

### 4.2 Start the application services

```powershell
docker compose --env-file .env.local up -d identity-service catalog-service inventory-service cart-service order-service payment-service api-gateway frontend-service
```

Check all services:

```powershell
docker compose --env-file .env.local ps -a
```

Look for services that are `Up` or healthy where health checks are configured. If a service repeatedly exits, do not keep rebuilding blindly—inspect its logs first.

### 4.3 Open the local application

| Page / service | URL |
|---|---|
| Customer storefront | `http://localhost:8080/customer/index.html` |
| Admin login | `http://admin.localhost:8080/admin/login.html` |
| API Gateway | `http://localhost:8000` |
| Gateway health | `http://localhost:8000/actuator/health` |

Use valid local test accounts that already exist in your local database. Do not assume a particular password.

---

## 5. Test the backend APIs

Run these commands in PowerShell while the stack is running.

### 5.1 Gateway health

```powershell
Invoke-WebRequest "http://localhost:8000/actuator/health" -UseBasicParsing |
    Select-Object StatusCode, Content
```

Expected: HTTP `200`, with a healthy response if dependencies are available.

### 5.2 Products API

```powershell
Invoke-WebRequest "http://localhost:8000/api/products?page=0&size=8" -UseBasicParsing |
    Select-Object StatusCode, Content
```

Expected in the previously verified local setup: HTTP `200`.

### 5.3 Categories API

```powershell
Invoke-WebRequest "http://localhost:8000/api/categories/tree" -UseBasicParsing |
    Select-Object StatusCode, Content
```

Expected in the previously verified local setup: HTTP `200`.

A successful HTTP response does not necessarily mean the response contains data. Inspect the response body if the UI is empty.

---

## 6. Test the frontend in the browser

Open:

- Customer storefront: `http://localhost:8080/customer/index.html`
- Admin login: `http://admin.localhost:8080/admin/login.html`

Hard-refresh after a frontend rebuild using **Ctrl + F5**.

### Browser test checklist

- [ ] Storefront loads.
- [ ] Products appear.
- [ ] Category navigation loads.
- [ ] Admin login page loads.
- [ ] Login works with valid local credentials.
- [ ] Product details and images load.
- [ ] Cart operations work.
- [ ] Checkout/order flows work when all required services are running.
- [ ] Browser DevTools → Network/Console shows no unexpected 404 or CORS errors.

### Understand local API routing

File:

`frontend-service/src/main/resources/static/js/api.js`

The environment-aware configuration should send local hosts (`localhost`, `127.0.0.1`, and `*.localhost`) to the gateway on port `8000`. Production hostnames use an empty prefix so `/api/...` requests remain same-origin and are routed by the production reverse proxy.

If the backend API works on port `8000` but the browser shows an error:

1. Open DevTools (`F12`) → Network.
2. Reproduce the problem.
3. Select the failed request.
4. Check the full Request URL, status code, and response.
5. Confirm the URL is correct for local development.
6. Check browser Console for CORS or JavaScript errors.

Do not replace the environment-aware configuration with a hardcoded local URL for production.

---

## 7. Troubleshoot services and logs

### 7.1 Check service status

```powershell
docker compose --env-file .env.local ps -a
docker ps
```

### 7.2 Read logs for a service

```powershell
docker compose --env-file .env.local logs --tail 150 api-gateway
docker compose --env-file .env.local logs --tail 150 identity-service
docker compose --env-file .env.local logs --tail 150 catalog-service
```

Follow logs live:

```powershell
docker compose --env-file .env.local logs -f api-gateway
```

Press `Ctrl + C` to stop following logs; it does not stop the container.

### 7.3 If a service exits

```powershell
docker compose --env-file .env.local logs --tail 200 <service-name>
```

Replace `<service-name>` with the failing Compose service. Check the first meaningful exception, database connectivity, missing environment variables, dependency health and RAM pressure before rebuilding.

### 7.4 Login troubleshooting

- `404`: check the request URL, controller path and gateway route.
- `401` or invalid credentials: check the local account and request contract.
- `500`: inspect Identity and Gateway logs for the underlying exception.

Do not immediately reset passwords or edit database rows. Never share credentials, tokens, cookies or secret-bearing headers.

### 7.5 Config Server access

The Config Server is available to other containers through `http://config-server:8888`. In the current local Compose setup, port `8888` may not be published to Windows, so `http://localhost:8888` can fail even when the container is healthy. Check the Compose port mapping before assuming Config Server is down.

---

## 8. Rebuild after code changes

### Frontend-only change

For a change to frontend static files such as `api.js`:

```powershell
docker compose --env-file .env.local up -d --build frontend-service
```

Then hard-refresh the browser with **Ctrl + F5** and repeat the relevant browser tests.

### Backend service change

Rebuild only the service you changed, using its Compose service name:

```powershell
docker compose --env-file .env.local up -d --build <service-name>
```

Confirm the service is healthy and retest the affected API and frontend flow.

**Local commands in this guide do not deploy to the VPS.** Run them from the Windows repository, not from the VPS SSH terminal.

---

## 9. Reduce RAM usage

The full stack can be resource-intensive, especially Kafka and Elasticsearch.

### 9.1 Inspect resource usage

```powershell
docker stats
docker compose --env-file .env.local ps -a
```

You can also check Docker Desktop and Windows Task Manager.

### 9.2 Stop observability services when not needed

```powershell
docker compose --env-file .env.local stop grafana prometheus kibana logstash elasticsearch
```

Start them again when needed:

```powershell
docker compose --env-file .env.local start elasticsearch logstash kibana prometheus grafana
```

Start Elasticsearch first and allow its health checks to settle.

### 9.3 Stop application services not needed for a limited test

For example, for a catalog-only session:

```powershell
docker compose --env-file .env.local stop payment-service order-service cart-service inventory-service
```

Checkout and order flows will not work while their required services are stopped. Start them again when needed:

```powershell
docker compose --env-file .env.local start inventory-service cart-service order-service payment-service
```

### 9.4 Stop the local stack safely

```powershell
docker compose --env-file .env.local stop
```

Resume later:

```powershell
docker compose --env-file .env.local start
docker compose --env-file .env.local ps -a
```

`stop` preserves containers and named volumes. It does not erase the Windows PostgreSQL databases.

> **Never use these as routine troubleshooting commands:** Docker Desktop Clean/Purge Data, Factory Reset, `docker system prune --volumes`, or `docker compose down -v`. They can delete data or resources you still need.

---

## 10. Database and data protection

The local Docker services connect to PostgreSQL on Windows using `host.docker.internal:5432`. Expected local databases:

- `identity_db`
- `catalog_db`
- `inventory_db`
- `order_db`
- `payment_db`

Local databases are separate from production. Do not point local services to production databases during normal development. Confirm the active database URL before making schema changes.

If products are missing, inspect the browser request and API response before recreating databases. Preserve existing database contents and Docker volumes unless a deliberate cleanup has been reviewed.

---

## 11. Commit and push your changes

Before staging:

```powershell
git status --short
git diff --stat
git diff
```

Stage only the files intended for the commit. For example:

```powershell
git add README.md .env.example
git diff --cached
```

Review the staged diff to ensure it contains no secrets or unrelated changes. Then commit and push to your current branch:

```powershell
git commit -m "docs: add complete local setup and testing guide"
git push origin HEAD
```

Do not use `git add .` without reviewing all modified and untracked files.

---

## 12. Production VPS: separate deployment workflow

**Do not run the local `.env.local` commands on the VPS.** Production has its own environment and configuration.

VPS repository:

```bash
cd /home/admin/apps/Sellzon
```

### 12.1 Read-only production checks

```bash
git status --short --branch
docker compose ps
docker compose logs --tail 100 api-gateway
```

### 12.2 Pull production source only when ready

First review local changes and production status. Then fetch and pull the intended branch. If production uses `master`:

```bash
git fetch origin
git status -sb
git pull --ff-only origin master
```

Use the actual production branch. A Git pull alone updates files; it does not automatically rebuild or restart running containers unless an automated deployment workflow is configured.

### 12.3 Before rebuilding any production service

- Verify production `.env` remains in place and is not overwritten.
- Verify Config Server's native repository path still resolves to `/app/config-repo` inside the container.
- Verify the production frontend uses same-origin `/api/...` routing through the reverse proxy.
- Confirm which service needs rebuilding.
- Consider the VPS's available RAM and avoid unnecessary full-stack builds/restarts.
- Inspect health and logs after deployment.

Deploy only after checking the change and its compatibility. Do not run `docker compose down -v`.

### 12.4 Public API smoke tests

These public endpoints previously returned HTTP `200`; test again after deployment:

```bash
curl -sS -o /tmp/sellzon-products-check.txt -w 'HTTP %{http_code}\n' \
  'https://sellzon.shop/api/products?page=0&size=8'

curl -sS -o /tmp/sellzon-categories-check.txt -w 'HTTP %{http_code}\n' \
  'https://sellzon.shop/api/categories/tree'
```

A successful GET test is useful but does not replace checking the storefront, login and relevant business flows.

---

## 13. Daily checklist

For each local development session:

1. [ ] Open PowerShell in the repository.
2. [ ] Review Git status and preserve uncommitted work.
3. [ ] Validate `.env.local` using `docker compose --env-file .env.local config --quiet`.
4. [ ] Start required infrastructure and application services.
5. [ ] Check Gateway health.
6. [ ] Test the API endpoint related to your task.
7. [ ] Test the feature in the browser.
8. [ ] Inspect logs and RAM usage if something fails.
9. [ ] Stop optional services when finished.
10. [ ] Review the Git diff before committing or pushing.
11. [ ] Keep production deployment separate from local development.

## 14. Keep this guide current

Update this guide whenever Compose service names or ports, environment variables, API routing, database hosts, JWT key mounts, or setup/deployment procedures change.
