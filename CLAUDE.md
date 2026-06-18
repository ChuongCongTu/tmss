# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

GMS (Garage Management System) — a **learning project** run in mentor mode. The Maven project lives in [gms/](gms/), not the repo root. A lead assigns tickets with acceptance criteria; a member (5y Angular/Spring, mostly reads & fixes code) implements end-to-end to prepare for a backend interview. **[gms/PROJECT_LOG.md](gms/PROJECT_LOG.md) is the project's memory** — read it first. It records every ticket's design decisions, the *reasoning* behind them, and recurring behavioral lessons. Keep it updated when finishing a ticket.

When acting as the lead: give acceptance criteria, not code; review like a PR; use Socratic hints (max 2-3 line snippets) rather than handing over answers. Verify claims against the real DB/API on a **restarted** app — do not trust "done" reports without evidence (see Verification below).

## Commands

All commands run from `gms/`:

```bash
# Start Postgres + Adminer (do this before running the app)
docker-compose up -d            # postgres:16 on :5432, Adminer on :8081

# Run the app (Windows uses mvnw.cmd)
./mvnw spring-boot:run          # serves on :8080

# Build / test
./mvnw clean package            # full build incl. tests
./mvnw test                     # all tests
./mvnw test -Dtest=GmsApplicationTests#methodName   # single test

# Health & DB inspection
curl localhost:8080/actuator/health     # currently returns 403 (Security locks it — known limitation)
# Adminer: localhost:8081  → server `postgres`, db `gms_db`, user `postgres` / `postgres123`
```

JDBC: `jdbc:postgresql://localhost:5432/gms_db` (postgres / postgres123).
Seed login (created by [DataInitializer](gms/src/main/java/gms/example/gms/common/config/DataInitializer.java)): username `chuongtdq` / password `123456`, role MANAGER.

## Architecture

Spring Boot 3.5 / Java 17 / PostgreSQL 16. **Monolith, layered, package-by-feature.** Not microservices — avoid over-engineering. Create feature packages only when a ticket needs them.

Package layout under `gms.example.gms`:
- `customer/` — Customer + Vehicle (Ticket #2)
- `part/` — Part, StockAdjustment, RepairOrder, RepairOrderItem, Invoice, InvoiceCounter (Tickets #3–5). **Tech debt:** repair-order and invoice code lives here but should be split into `repairorder/` and `invoice/` packages.
- `staff/` — Staff, auth, JWT login/register (Ticket #6)
- `common/` — `ApiResponse<T>`, `exception/` (handlers below), `config/` (Security, Password, DataInitializer), `security/` (JwtUtil, JwtAuthenticationFilter)

Each feature follows `controller / service / repository / entity / dto` (+ `enums` for staff).

### Cross-cutting conventions (enforced — match them)
- **Layering:** Controllers return `ApiResponse<T>`; never expose `@Entity` to the API. DTOs split input (`CreateXxxRequest`) from output (`XxxResponse`). Response DTOs carry foreign keys as IDs (e.g. `VehicleResponse.customerId`), never nested entities — avoids lazy-init and Jackson cycles.
- **Errors:** `ResourceNotFoundException` → 404, `BusinessException` → 400, `UnauthorizedException` → 401, all mapped centrally in [GlobalExceptionHandler](gms/src/main/java/gms/example/gms/common/exception/GlobalExceptionHandler.java) (`@RestControllerAdvice`). `DataIntegrityViolationException` → 409. A bare `throw` is 500 by default — map it.
- **System-owned fields are never accepted from request DTOs** (`id`, `enabled`, `invoiceNo`, `taxRate`, timestamps). Set them server-side/defaults. A past bug: `enabled` taken from request → null → NOT NULL violation → masked as a 409 "duplicate" message. Don't repeat it.
- **DI** via constructor only (`@RequiredArgsConstructor` + `final` fields).
- **Time:** use `Instant` (maps to `TIMESTAMPTZ`), never `LocalDateTime`.
- **Money:** `BigDecimal` / `NUMERIC`, never `double`; rounding `setScale(2, HALF_UP)`. Counts use `INTEGER`.

### Database — Flyway is the single source of truth
- Hibernate `ddl-auto: validate` — it only checks the schema, never alters it. Schema changes happen **only** via Flyway migrations in [db/migration/](gms/src/main/resources/db/migration/).
- **Applied migrations are immutable.** Never edit a `V*.sql` that has run — Flyway stores a checksum; editing it causes checksum mismatch and the app fails to start (recover with `flyway repair`). To change schema, add a new `V<n>__*.sql`.
- PKs are UUID (`gen_random_uuid()`).
- Migration history: V1 customers · V2 vehicles · V3 customers.updated_at · V4 parts · V5 stock_adjustments (immutable ledger) · V6 quantity CHECK + repair_orders/items · V7 invoices + invoice_counters · V8 staffs.
- **Customer vs Staff are separate concepts:** `customer` is business data with no login; `staff` is a system operator with password_hash + role.

### Concurrency & data-integrity patterns (the project's teaching core)
These were deliberate; preserve the approach:
- **Stock changes are atomic SQL updates**, not read-modify-write in Java. `@Modifying @Query`: `UPDATE Part SET quantity = quantity - :qty WHERE id = :id AND quantity >= :qty`, returns rows-affected. `rows == 0` → insufficient stock → `BusinessException`, no ledger row written. `@Modifying` bypasses the L1 cache, so re-read after updating (a past "off-by-one-beat" bug came from a stale in-memory entity).
- **Ledger / snapshot pattern:** `stock_adjustments` and repair-order item `unit_price` record facts at the time they happened and are immutable. `repair_order.total_amount` and invoice totals are computed once at creation and **stored** (not derived) because they are historical and must survive later price/part changes.
- **Idempotency / uniqueness:** prefer a DB UNIQUE constraint over check-then-act (e.g. `invoices.repair_order_id UNIQUE` prevents duplicate invoices). Catch `DataIntegrityViolationException` → 409.
- **Gapless sequence numbers:** `invoice_no` is generated via an atomic upsert on `invoice_counters` (`INSERT ... ON CONFLICT (year) DO UPDATE SET last_no = last_no + 1 RETURNING last_no`), resetting per year. Avoid `MAX(...)+1` (check-then-act, unsafe under concurrency).
- **Multi-step writes are `@Transactional`** and all-or-nothing. Note `@Transactional` rolls back only on `RuntimeException`/`Error` — the custom exceptions extend `RuntimeException` for this reason. Known un-fixed N+1 queries exist in `toResponse` mappers (LAZY associations in loops) — accepted at current data volume; fix with JOIN FETCH / `@EntityGraph` when needed.

### Security
Spring Security + JWT (HS384). Stateless, CSRF disabled, CORS allows `http://localhost:4200`. The JWT secret is in [application.yaml](gms/src/main/resources/application.yaml) `app.jwt.secret` — **must move to an env var / not be committed** for any real use. [JwtAuthenticationFilter](gms/src/main/java/gms/example/gms/common/security/JwtAuthenticationFilter.java) reads `Authorization: Bearer`, validates, and sets a `ROLE_<role>` authority; a bad token is ignored (the authorization filter rejects later), not blocked in the filter. Role authorization matrix lives in [SecurityConfig](gms/src/main/java/gms/example/gms/common/config/SecurityConfig.java): GETs on operational data → any of the 3 roles; POST `/api/parts/**` → MANAGER; POST customers/repair-orders/invoices → RECEPTIONIST or MANAGER. Roles: RECEPTIONIST / TECHNICIAN / MANAGER (enum + DB CHECK constraint).

Known limitations (from log): `/actuator/health` returns 403 (should be permitAll for probes); `/api/auth/register` is public (anyone can self-grant MANAGER — should restrict to MANAGER); no refresh token.

## Verification (the hardest-won lesson — non-negotiable)
The recurring failure across every ticket has been claiming "done" without proof. Before reporting any task complete:
1. **Rebuild and restart the app** — code edits don't take effect until reloaded; devtools needs an IDE rebuild to trigger restart. Testing the old running build is the #1 source of false "done".
2. Confirm health, then call each affected endpoint and **read the actual HTTP status and every response field** against the acceptance criteria — don't just run the command.
3. Check the real DB (correct table names — use `\dt` first; tables are plural: `staffs`, `invoice_counters`) before concluding something is missing.
4. When data contradicts a claim, trust the data and re-run; show evidence (DB rows / response bodies), don't argue. "It worked before" (an old row) ≠ "it works now" (this restarted app).

Also: read SQL like a compiler before running it (repeated typos: `TIMESTAMPZ`, stray trailing commas, misspelled columns).

## Known code bugs to watch
`@PathVariable` names may not match URL templates (no `-parameters` at compile time), causing 400/500 "missing path variable". Seen in `POST /api/repair-orders/{orderId}/invoice` (declared `orderid`) and `GET /api/repair-orders/{id}` (declared `uuid`). Fix by binding explicitly: `@PathVariable("orderId") UUID orderId`. See [gms/TEST_DATA.md](gms/TEST_DATA.md) for full request/response examples and call ordering.
