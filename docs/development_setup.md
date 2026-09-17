# development setup

## architecture

The application is a modular monolith. One Spring Boot process owns the API and one PostgreSQL database owns the shared business data. The React application only calls the API; it never connects to PostgreSQL directly.

All team machines should open the same deployed web URL. The browser may be different on each machine, but the API, database, permissions, sessions, and business data are shared by the deployment. Do not create a separate database for each client machine.

## prerequisites

- Java 21 or a compatible JDK.
- Node.js 20 or newer and npm.
- PostgreSQL 15 or newer.
- Network access from the backend host to PostgreSQL and from each team machine to the backend/web host.

## create the shared database

Create the database and application role once on the PostgreSQL server. Use an administrator account for this step and choose a strong password outside the repository.

```sql
create role erp_vinamik login password 'replace_with_a_secret';
create database erp_vinamik owner erp_vinamik;
```

The application role must be able to create and alter the schemas used by Flyway during the first deployment. After migrations have been applied, reduce permissions according to the team's deployment policy. Never commit the password or put it in frontend code.

## configure the backend

Use `ERP_Backend/.env.example` as a template, then export the variables into the backend process/IDE or configure them in the backend host's secret manager. Spring Boot/Maven does not read `.env` files automatically:

```text
ERP_DB_URL=jdbc:postgresql://database-host:5432/erp_vinamik
ERP_DB_USERNAME=erp_vinamik
ERP_DB_PASSWORD=replace_with_a_secret
ERP_AUTH_SECURE_COOKIE=true
ERP_BOOTSTRAP_ADMIN_USERNAME=admin
ERP_BOOTSTRAP_ADMIN_PASSWORD=replace_with_a_long_secret
```

`ERP_BOOTSTRAP_ADMIN_*` is used only when the identity tables are empty. The bootstrap password must be at least 12 characters. Remove or clear these variables after the first administrator has been created. The first account is stored as the single protected `super_admin`; it receives every active permission and cannot be disabled, demoted or deleted through the application. Session cookies require HTTPS and `ERP_AUTH_SECURE_COOKIE=true` in a shared or production environment.

Run the backend from `ERP_Backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway applies versioned migrations from `src/main/resources/db/migration` on startup. A failed migration stops startup so that the database is not used with a partial schema. Set `ERP_FLYWAY_ENABLED=false` only for an explicitly managed environment that already applies migrations; do not disable it for normal development.

## run the frontend locally

From `ERP_Client`:

```powershell
npm install
npm run dev
```

Vite serves the React application on port 5173 and proxies `/api` to `http://localhost:8080`. The current client already has a `package.json`, route-level lazy loading, and a shared API client. The browser must still call API routes; it must not contain database credentials.

## deploy for five machines

1. Run PostgreSQL on one reachable server or managed PostgreSQL service.
2. Run one Spring Boot instance with the database environment variables above.
3. Build the React application with `npm run build`.
4. Serve `ERP_Client/dist` from a web server or reverse proxy on the same origin as the API, forwarding `/api` to Spring Boot.
5. Give all five machines the same HTTPS URL. Each user signs in with an account whose roles and permissions are stored in the shared identity schema.

The backend is the authority for authorization. The frontend hides actions that the current account cannot use, but every API endpoint checks the permission again. A missing authentication session returns HTTP 401; an authenticated account without the required permission returns HTTP 403.

The deployment exposes only /actuator/health and its liveness/readiness groups for a reverse proxy. Readiness includes the database check; keep these endpoints reachable by the load balancer but do not expose other actuator endpoints publicly. Graceful shutdown is enabled so a rolling restart can drain requests.

## useful checks

Backend tests and packaging:

```powershell
cd ERP_Backend
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Frontend checks:

```powershell
cd ERP_Client
npm run lint
npm run build
```

The current backend includes the shared identity platform, employee management, employment contracts, leave/absence workflow, reward/discipline inputs, inventory master/receipt/issue/transfer/stocktake/balance flows, HR and Inventory reference master-data APIs, and production plan/BOM/order/material/assignment/output/progress flows. Payroll calculation remains deferred until its day-count formula is approved; frontend business screens, deployment packaging, and the two deferred modules remain in the task backlog until verified against the same contracts.
## Flyway và JPA

The order is deliberate: an administrator creates the PostgreSQL database once, Flyway creates and upgrades the `identity`, `hr`, `inventory`, and `production` schemas, then Hibernate/JPA validates the mappings with `spring.jpa.hibernate.ddl-auto=validate`. The backend uses JPA repositories for all persistence; ledger posting, balance projection, row locks, idempotency and PostgreSQL-specific read models use bound native queries inside module-owned JPA repositories where needed. Application code does not use JDBC/Spring JDBC APIs. The PostgreSQL JDBC driver remains an internal Hibernate/JPA dependency. Hibernate must not be configured with `ddl-auto=update` or `ddl-auto=create`.


