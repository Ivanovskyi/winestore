# WineStore API

Spring Boot REST API for an online wine store: JWT authentication, catalog with filters (cached in Redis), shopping cart and orders with a stubbed payment step.

## Tech Stack

- **Language**: Java 25 (Eclipse Temurin)
- **Framework**: Spring Boot 4.1.2 (Spring Security 7, Spring Data JPA, Hibernate)
- **Database**: PostgreSQL 17, migrations with Flyway
- **Cache**: Redis 7 via Spring Cache / Spring Data Redis
- **Security**: Spring Security, JWT via OAuth2 Resource Server (HS256)
- **Build**: Gradle 9.6 (Kotlin DSL build script)
- **Testing**: JUnit 5 + Mockito + AssertJ
- **Infrastructure**: Docker, Docker Compose

## Features

- **Authentication**: registration and login, JWT, roles `USER` and `ADMIN`
- **Wine catalog**: public browsing with filters (type, country, price range) and pagination, served from Redis
- **Admin**: create, update and delete wines (admin only)
- **Shopping cart**: add and remove items, totals, stock check on add
- **Orders**: checkout from cart, pay, cancel (unpaid orders only), order history
- **Age verification**: checkout requires the customer to be 18+
- **Stock safety**: stock is deducted at checkout in one transaction; optimistic locking (`@Version`) prevents overselling under concurrent orders
- **Payment stub**: `FakePaymentService` always succeeds and returns "Purchase successful". Swap it for a real provider by implementing `PaymentService` under another `@Profile`
- **Errors**: all errors are returned as RFC 9457 `ProblemDetail`

## Caching (Redis)

The public catalog is the hot read path, so it is cached in Redis and a cache hit does not touch PostgreSQL at all.

| Cache | Used by | Key | TTL |
|-------|---------|-----|-----|
| `wine` | `GET /api/wines/{id}` | wine id | `app.cache.wine-ttl` (10m) |
| `wines-search` | `GET /api/wines` | normalized filters + page + size + sort | `app.cache.search-ttl` (2m) |

Keys in Redis look like `winestore:wine::42` and `winestore:wines-search::type=RED|country=italy|...`. Search keys are normalized (`France` / ` france `, `30` / `30.00` share one entry).

**Invalidation.** Every write evicts the affected entries, so the TTL is only a safety net:

- admin create / update / delete: the wine and all cached search pages
- checkout and cancel (they change `stock`, which is part of the catalog response): the wines in the order and all cached search pages

Eviction runs **after the database transaction commits**, so a concurrent reader cannot put the old row back into the cache before the commit is visible. Two rare cases can still leave a stale entry, and both are bounded by the TTL: an eviction that is lost because Redis was unreachable at that moment, and a reader that loaded the old row just before the commit and wrote it to the cache just after the eviction.

**What is deliberately not cached.** Cart, checkout, orders and users always read PostgreSQL. The stock check when adding to the cart and at checkout must see the authoritative value, and per-user data changes too often to benefit. So in the rare stale cases above the catalog can show an outdated stock value for up to the TTL, but a purchase can never oversell.

**Redis is optional at runtime.** If Redis is down or slow (1 s timeout), cache errors are logged and requests are served from the database. For that reason the Redis health indicator is excluded from `/actuator/health`.

Cached values are stored with JDK serialization, so Redis must not be exposed to untrusted clients. Redis runs without persistence and with `allkeys-lru` eviction in `docker-compose.yml`.

## Quick Start

### Docker Compose (app + database + Redis)

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. The database is seeded with 8 sample wines.

### Gradle (app on the host, database and Redis in Docker)

```bash
docker compose up -d db redis
./gradlew bootRun
```

### Try it

```bash
# log in as the default admin and keep the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@wine.local","password":"admin12345"}' | jq -r .accessToken)

# public catalog with filters (the second identical call is served from Redis)
curl 'http://localhost:8080/api/wines?type=RED&maxPrice=30&page=0&size=5'

# look at the cache
docker compose exec redis redis-cli --scan --pattern 'winestore:*'

# an authenticated endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart
```

A ready-made collection of requests (register, cart, checkout, pay) is in [`requests.http`](requests.http); it works with the IntelliJ HTTP Client.

## API

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register a new user, returns a JWT |
| POST | `/api/auth/login` | Log in, returns a JWT |

### Wines
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/wines` | public | List wines. Query: `type`, `country`, `minPrice`, `maxPrice`, `page`, `size`, `sort` |
| GET | `/api/wines/{id}` | public | Wine details |
| POST | `/api/admin/wines` | admin | Create a wine |
| PUT | `/api/admin/wines/{id}` | admin | Update a wine |
| DELETE | `/api/admin/wines/{id}` | admin | Delete a wine |

### Cart
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/cart` | View the cart with totals |
| POST | `/api/cart/items` | Add an item: `{"wineId": 1, "quantity": 2}` (`quantity` defaults to 1) |
| DELETE | `/api/cart/items/{wineId}` | Remove an item |

### Orders
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/orders` | Checkout: create an order from the cart |
| GET | `/api/orders` | List your orders |
| GET | `/api/orders/{id}` | Order details |
| POST | `/api/orders/{id}/pay` | Pay for the order (stub) |
| POST | `/api/orders/{id}/cancel` | Cancel an unpaid order, stock is returned |

### Health
- `GET /actuator/health`

## Domain Reference

**Wine types**: `RED`, `WHITE`, `ROSE`, `SPARKLING`, `DESSERT`

**Order statuses**: `NEW` (created, awaiting payment) → `PAID` or `CANCELLED`

## Security

- Public: `/api/auth/**`, `GET /api/wines/**`, `/actuator/health`
- `/api/admin/**` requires the `ADMIN` role
- Everything else requires a valid `Authorization: Bearer <token>` header
- Stateless: no sessions, CSRF is disabled because tokens are sent in a header
- Passwords are hashed with BCrypt; JWTs expire after 1 hour
- Other users' orders are returned as `404`, not `403`

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/winestore` | Database JDBC URL |
| `DB_USER` | `winestore` | Database user |
| `DB_PASSWORD` | `winestore` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `SPRING_DATA_REDIS_PASSWORD` | _(none)_ | Redis password, if your Redis requires one |
| `JWT_SECRET` | `dev-only-secret-change-me-dev-only-secret-change-me` | HS256 signing secret, at least 32 bytes |
| `ADMIN_EMAIL` | `admin@wine.local` | Admin account created on first start |
| `ADMIN_PASSWORD` | `admin12345` | Admin password |

Cache TTLs are set in `application.yml` (`app.cache.wine-ttl`, `app.cache.search-ttl`).

> **Development only.** The defaults above (database credentials, JWT secret, admin password) are meant for local use. Override all of them anywhere else.

## Testing

```bash
./gradlew test
```

Unit tests cover the service layer (auth, wines, cart, orders), the age rule, catalog cache keys and cache invalidation (including "after commit" and "Redis is down"), using Mockito. Security rules, JPA queries, Flyway migrations and the real Redis round trip are not covered by automated tests yet.

## Project Structure

```
src/main/java/dev/winestore/
├── auth/       # Register, login, JWT issuing
├── cart/       # Cart items and cart service
├── common/     # Exceptions, ProblemDetail handler, current-user helper
├── config/     # Security, Redis cache manager, configuration properties
├── order/      # Orders, checkout, age policy
├── payment/    # PaymentService and the fake implementation
├── user/       # User entity, repository, admin seeder
├── wine/       # Catalog, filters, admin CRUD, cache keys and invalidation
└── WinestoreApplication.java

src/main/resources/
├── application.yml
└── db/migration/   # Flyway: V1 schema, V2 sample wines
```

## Build

```bash
./gradlew bootJar      # executable jar in build/libs
./gradlew clean build  # full build with tests
```

If you change an already-applied Flyway migration during development, reset the local database with `docker compose down -v`.
