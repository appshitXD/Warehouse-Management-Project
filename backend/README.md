# Backend – Warehouse Management API

Spring Boot REST API over a MySQL database. All SQL is written by hand with JDBC
`PreparedStatement` (no JPA/Hibernate), as the course brief requires.

## Requirements

| | |
|---|---|
| JDK | 17 or newer |
| Database | MySQL 8.0+ (MariaDB 10.6+ also works) |
| JDBC driver | MySQL Connector/J (downloaded by Maven, version managed by Spring Boot) |
| Build tool | Maven wrapper (`./mvnw`), no install needed |

## Run it

```bash
# 1. Create the database (re-runnable: it drops and recreates warehouse_db)
mysql -u root -p < ../database/schema.sql
mysql -u root -p < ../database/dev-seed.sql      # optional test rows

# 2. Start the API on http://localhost:8080
./mvnw spring-boot:run

# 3. Check it
curl http://localhost:8080/api/health            # {"db":"up","catalog":"warehouse_db"}
```

## Connection settings

Read from environment variables, so nobody has to edit code or recompile:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/warehouse_db` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(empty)* |
| `CORS_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` |

Example: `DB_PASSWORD=secret ./mvnw spring-boot:run`

## Code layout (package by feature)

```
com.group4.warehouse
├── common/      error JSON, SQL-error translation, CORS
├── health/      GET /api/health
├── warehouse/   Warehouse, Create/Update request, Repository (SQL), Service, Controller
├── zone/  aisle/  bin/  product/  supplier/  customer/   (same five-file pattern)
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
| 503 | `DATABASE_UNAVAILABLE` | MySQL is not reachable |

## Endpoints

Status: ✅ done · 🚧 planned (issue #3, same conventions)

| Resource | Endpoints | JSON | Status |
|---|---|---|---|
| Warehouse | `/warehouses`, `/warehouses/{id}` | `{id, name, address}` | ✅ |
| Zone | `/warehouses/{warehouseId}/zones`, `.../zones/{zoneId}` | `{warehouseId, id, name}` | 🚧 |
| Aisle | `/warehouses/{w}/zones/{z}/aisles`, `.../aisles/{aisleId}` | `{warehouseId, zoneId, id, name}` | 🚧 |
| Bin | `/warehouses/{w}/zones/{z}/aisles/{a}/bins`, `.../bins/{position}` | `{warehouseId, zoneId, aisleId, position, name}` | 🚧 |
| Product | `/products`, `/products/{id}` | `{id, name, unit, storage, reorderLevel}` | 🚧 |
| Supplier | `/suppliers`, `/suppliers/{id}` | `{id, name, phone, address}` | 🚧 |
| Customer | `/customers`, `/customers/{id}` | `{id, name, phone, address}` | 🚧 |

Notes:
- Zone, aisle and bin keys are **composite** (a bin is identified by warehouse + zone +
  aisle + position), so their URLs are nested. `GET /api/bins` will also list every bin
  for screens that need all of them at once.
- All IDs are codes typed by the user (W01, Z01, A01, B001, P01, S01, C01):
  letters, digits, `-` and `_`, max 40 characters. They cannot be changed after creation.

### Warehouse examples

```bash
curl http://localhost:8080/api/warehouses?q=thu
curl -X POST http://localhost:8080/api/warehouses -H 'Content-Type: application/json' \
     -d '{"id":"W99","name":"District 7 Depot","address":"10 Tan Trao"}'
curl -X PATCH http://localhost:8080/api/warehouses/W99 -H 'Content-Type: application/json' \
     -d '{"address":""}'          # "" clears the address, a missing field is kept
curl -X DELETE http://localhost:8080/api/warehouses/W99
```
