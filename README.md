# Accounting App

A double-entry accounting application with feature parity in mind for tools like QuickBooks/GnuCash:
a Java/Spring Boot backend, a React/TypeScript frontend, and a PostgreSQL database, all scoped to
multi-tenant organizations behind real authentication.

## Features

- **Chart of Accounts** — GAAP-style account hierarchy (Asset/Liability/Equity/Revenue/Expense),
  parent/child accounts, opening balances, tags.
- **Journal Entries** — double-entry postings validated both in the application and by a Postgres
  database trigger (debits must equal credits even if the app layer is bypassed).
- **Accounts Receivable** — customers, invoices, payments (with partial/overpayment handling), and
  an A/R aging report.
- **Accounts Payable** — vendors, bills, bill payments, early-payment discounts, and an A/P aging
  report.
- **Banking & Reconciliation** — CSV/OFX bank feed import, transaction-to-ledger-line matching
  (manual and auto-match), and a full statement reconciliation workflow.
- **Financial Reports** — Balance Sheet, Income Statement, Cash Flow (direct and indirect methods),
  General Ledger detail, A/R aging, A/P aging.
- **Authentication & RBAC** — JWT access tokens with rotating/revocable refresh tokens, account
  lockout after repeated failed logins, and five organization-scoped roles (Admin, Manager,
  Accountant, Viewer, Limited User) enforcing read/write and admin-only boundaries.
- **Multi-tenant Organizations** — every record is scoped to an organization; users can belong to
  multiple organizations with a different role in each, switchable from the UI.
- **Audit Trail** — every mutating action is recorded with who/when/what changed.

## Tech Stack

**Backend**: Java 21, Spring Boot 3, Spring Data JPA, Spring Security, PostgreSQL, Liquibase,
MapStruct, Lombok, JWT (jjwt), springdoc-openapi, JUnit 5 + Testcontainers.

**Frontend**: React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, React Hook Form + Zod,
Zustand, React Router, Vitest.

## Running the app

### Option 1: Docker Compose (fastest way to see the whole stack)

Requires Docker.

```bash
docker compose up --build
```

This starts Postgres, the backend (runs its own database migrations on boot), and the frontend
(served by nginx, which proxies `/api` to the backend). Once it's up:

- Frontend: http://localhost:8081
- Backend API: http://localhost:8080/api/v1
- API docs (Swagger UI): http://localhost:8080/swagger-ui.html

Stop everything with `docker compose down` (add `-v` to also drop the database volume).

### Option 2: Local development

Prerequisites: Java 21, Maven, Node 22+, PostgreSQL 14+ running locally.

**1. Create the database:**

```bash
createdb accounting_app
```

**2. Start the backend** (from `backend/`):

```bash
mvn spring-boot:run
```

This runs on port 8080 and applies database migrations automatically. It connects to
`jdbc:postgresql://localhost:5432/accounting_app` with user/password `accounting_app` by default —
override with the `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` environment variables if your local
Postgres is configured differently.

**3. Start the frontend** (from `frontend/`):

```bash
npm install
npm run dev
```

This runs on http://localhost:5173 and proxies API requests to the backend on port 8080.

**4. Sign up:** open http://localhost:5173, register an account, then create your first
organization from the switcher in the top bar — you'll be its admin.

### Configuration

Both the Docker Compose setup and local dev use sensible defaults for a first run. For anything
beyond local development, override these environment variables on the backend:

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Database connection | local Postgres, `accounting_app`/`accounting_app` |
| `JWT_SECRET` | Signing key for access tokens | a dev-only placeholder — **must** be changed for any non-local use |
| `JWT_ACCESS_TTL_MIN` | Access token lifetime (minutes) | `15` |
| `JWT_REFRESH_TTL_DAYS` | Refresh token lifetime (days) | `7` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin(s) | `http://localhost:5173` |

## Testing

**Backend** (from `backend/`, requires Docker for Testcontainers-backed integration tests):

```bash
mvn test
```

**Frontend** (from `frontend/`):

```bash
npm run lint
npm test
```

## Project Structure

```
backend/    Spring Boot API (Maven)
frontend/   React SPA (Vite)
```

See [CLAUDE.md](CLAUDE.md) for architectural notes aimed at future contributors/agents working in
this repo.
