# Backend – Warehouse Management API

Spring Boot REST API over a MySQL database. All SQL is written by hand with JDBC
`PreparedStatement` (no JPA/Hibernate), as the course brief requires.

## Requirements

| | |
|---|---|
| JDK | 17 or newer |
| Database | MySQL 8.0.16+ (the CHECK constraints in `warehouse.sql` need it) |
| JDBC driver | MySQL Connector/J (downloaded by Maven, version managed by Spring Boot) |
| Build tool | Maven wrapper (`./mvnw`), no install needed |

## Run it

```bash
# 1. Create the database WarehouseManagement: 20 tables + sample data.
#    Re-runnable: it drops the whole database (DROP DATABASE IF EXISTS) and builds it again.
mysql -u root -p < ../database/warehouse.sql

# 2. Start the API on http://localhost:8080
./mvnw spring-boot:run

# 3. Check it
curl http://localhost:8080/api/health            # {"db":"up","catalog":"WarehouseManagement"}
```

`database/schema.sql` and `database/dev-seed.sql` are the old 7-table scripts and are
no longer used by the app.

On Linux, MySQL table names are case-sensitive and some older repositories use
lower-case table names, so start MySQL there with `lower_case_table_names=1`.
macOS and Windows need nothing.

## Tests

```bash
./mvnw test
```

Unit tests (no database needed) for the parts that are easy to get wrong: the bin ID
format (`BinIdTest`), ISO dates (`IsoTimeTest`), the checks done before the stock-movement
transaction (`StockMovementServiceTest`) and the staff rules: duplicate phones and
supervision cycles (`StaffServiceTest`). `WarehouseApplicationTests` starts the whole app.

## Connection settings

Read from environment variables, so nobody has to edit code or recompile:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/WarehouseManagement` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(empty)* |
| `CORS_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` |

Example: `DB_PASSWORD=secret ./mvnw spring-boot:run`

## Code layout (package by feature)

```
com.group4.warehouse
├── common/      error JSON, SQL-error translation, CORS, BinId, IsoTime
├── health/      GET /api/health
├── warehouse/   Warehouse, Create/Update request, Repository (SQL), Service, Controller
├── zone/  aisle/  bin/  product/  supplier/  customer/   (same five-file pattern)
├── staff/       Staff CRUD, STAFF + STAFF_PHONE in one transaction
├── receipt/     goods receipts + lines; saving adds stock (one transaction)
├── issue/       goods issues + lines; Shipped/Completed takes stock out (one transaction)
├── count/       stock counts + lines; Completed sets stock to the counted quantity
├── movement/    POST /api/movements, 3 SQL statements in one transaction
├── order/       customer orders, plus orders waiting to ship
├── task/        tasks assigned to staff
├── report/      the report endpoints (/api/reports/...)
└── bootstrap/   GET /api/bootstrap, every list the frontend needs in one call
```

## API conventions

- Base URL: `http://localhost:8080/api`
- JSON field names are camelCase.
- `POST` creates (201 + created object), `PATCH` updates only the fields sent (200),
  `DELETE` returns 204 with no body.
- List endpoints accept `?q=` to search by ID or name.
- Every error has the same body:

```json
{ "message": "This warehouse cannot be deleted because other records still use it. ...",
  "code": "IN_USE",
  "fields": {} }
```

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | a field is invalid – `fields` maps field → message |
| 400 | `INVALID_JSON`, `BAD_REQUEST`, `CHECK_FAILED`, `TOO_LONG` | bad body or a value breaks a DB rule |
| 404 | `NOT_FOUND` | no row with that ID |
| 409 | `DUPLICATE` | ID or unique name already exists |
| 409 | `IN_USE` | delete blocked: child rows still reference it |
| 409 | `REFERENCE_NOT_FOUND` | the parent (e.g. warehouse of a zone) does not exist |
| 409 | `LOCKED` | the document's status forbids the change (e.g. deleting a Shipped issue) |
| 409 | `NOT_IN_BIN`, `NOT_ENOUGH_STOCK` | the bin cannot give the stock a document needs |
| 503 | `DATABASE_UNAVAILABLE` | MySQL is not reachable |

## Endpoints

Status: ✅ done

| Resource | Endpoints | JSON | Status |
|---|---|---|---|
| Warehouse | `/warehouses`, `/warehouses/{id}` | `{id, name, address}` | ✅ |
| Zone | `/zones`, `/warehouses/{w}/zones/{zoneId}` | `{warehouseId, id, name}` | ✅ |
| Aisle | `/aisles`, `/warehouses/{w}/zones/{z}/aisles/{aisleId}` | `{warehouseId, zoneId, id, name}` | ✅ |
| Bin | `/bins`, `/warehouses/{w}/zones/{z}/aisles/{a}/bins/{position}` | `{warehouseId, zoneId, aisleId, position, name}` | ✅ |
| Product | `/products`, `/products/{id}`, `GET /products/{id}/history` | `{id, name, unit, storage, reorderLevel}` | ✅ |
| Supplier | `/suppliers`, `/suppliers/{id}` | `{id, name, phone, address}` | ✅ |
| Customer | `/customers`, `/customers/{id}` | `{id, name, phone, address}` | ✅ |
| Staff | `/staff`, `/staff/{id}` (STAFF + STAFF_PHONE in one transaction) | `{id, name, address, warehouseId, supervisorId, phone, phones[]}` | ✅ |
| Goods receipt | `/receipts`, `/receipts/{id}`, `GET /receipts/{id}/lines` | `{id, date, supplierId, supplierName, lines[]}` | ✅ |
| Goods issue | `/issues`, `/issues/{id}`, `GET /issues/{id}/lines` | `{id, date, status, stateOfGood, orderId, customerId, customerName}` | ✅ |
| Stock count | `/counts`, `/counts/{id}`, `GET /counts/{id}/lines` | `{id, date, status, lines[]}` | ✅ |
| Stock movement | `POST /movements` (one transaction) | `{productId, fromBinId, toBinId, quantity, staffId}` | ✅ |
| Order | `/orders`, `/orders/{id}`, `GET /orders/awaiting-issue` | `{id, time, status, customerId, customerName}` | ✅ |
| Task | `/tasks`, `/tasks/{id}` | `{id, name, status, staffId, staffName}` | ✅ |
| Bootstrap | `GET /bootstrap` | one object with every list the frontend shows | ✅ |
| Reports | `GET /reports/product-locations`, `low-stock`, `movements`, `empty-bins`, `latest-count-differences`, `overstock`, `slow-moving?asOf=`, `top-customers` | `{items: [...]}` (+ `from`/`to`/`asOf`) | ✅ |

### Receipts, issues and counts

These three documents change stock, so each save runs as one transaction over the header,
its lines and `PRODUCT_BIN` (see `common/Transactions.java` and `common/StockLedger.java`).

Body of `POST`: `{ "lines": [ { "productId": "P01", "binId": "W02|Z02|A02|B007", "quantity": 5 } ], ... }`
plus `supplierId` (receipt), `orderId` + `stateOfGood` (issue). `id`, `date` and `status`
are optional: the next ID (GR16, GI17, SC20, ...), today, and the column `DEFAULT 'Pending'`.
`PATCH` takes the same fields, all optional; sending `lines` replaces every line.

| Document | When stock changes | Edit | Delete |
|---|---|---|---|
| Goods receipt (no status column) | added as soon as it is saved | any time; stock moves by the difference | takes the stock back out; refused (409) if it has already left the bin |
| Goods issue | taken out when the status becomes Shipped or Completed | everything while Pending; afterwards only Shipped → Completed | only while Pending |
| Stock count | when the status becomes Completed, stock = counted quantity; `Recorded_Quantity` is re-read just before | until Completed; status never goes back | only while Pending |

Orders and tasks are single-table CRUD with no stock effect. An order that already has a
goods issue cannot be deleted (`FK_ISSUE_ORDER ... ON DELETE RESTRICT` → 409 `IN_USE`).

Formats shared with the frontend (`contracts.js`):
- **Bin ID** is one string `W02|Z02|A02|B007` (warehouse|zone|aisle|position),
  see `common/BinId.java`.
- **Dates** are ISO 8601 with the time zone, e.g. `2026-10-12T00:00:00+07:00`,
  see `common/IsoTime.java`.
- Empty lists are `[]`, never `null`.

Notes:
- Zone, aisle and bin keys are **composite** (a bin is identified by warehouse + zone +
  aisle + position), so their URLs are nested. `GET /api/bins` will also list every bin
  for screens that need all of them at once.
- All IDs are codes typed by the user (W01, Z01, A01, B001, P01, S01, C01):
  letters, digits, `-` and `_`, max 10 characters (VARCHAR(10) in `warehouse.sql`). They cannot be changed after creation.

### Warehouse examples

```bash
curl http://localhost:8080/api/warehouses?q=thu
curl -X POST http://localhost:8080/api/warehouses -H 'Content-Type: application/json' \
     -d '{"id":"W99","name":"District 7 Depot","address":"10 Tan Trao"}'
curl -X PATCH http://localhost:8080/api/warehouses/W99 -H 'Content-Type: application/json' \
     -d '{"address":""}'          # "" clears the address, a missing field is kept
curl -X DELETE http://localhost:8080/api/warehouses/W99
```
