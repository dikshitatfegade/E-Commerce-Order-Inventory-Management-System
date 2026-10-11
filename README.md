# E-Commerce Order & Inventory Management System

Full-stack portfolio project: **Spring Boot 3 (Java 21) + MySQL + React (Vite)**, with JWT auth, role-based access
(Customer / Admin), stock-safe order placement, Docker Compose and GitHub Actions CI.

## Features
- Register / login with JWT (BCrypt passwords), roles `CUSTOMER` and `ADMIN`
- Product catalog with categories, search, pagination
- Cart and checkout (COD / mock card only, no real payments)
- Orders: place, view own orders, cancel (stock is restored)
- Admin: manage products and categories, set stock levels, view all orders, update order status
- Stock safety: pessimistic row lock on inventory when placing/cancelling orders, plus `@Version` optimistic field
- Flyway migrations, global error handler, Swagger UI, unit tests (JUnit 5 + Mockito)

## Project layout
```
backend/    Spring Boot API (entity, repository, service, controller, security, dto, exception, config)
frontend/   React + Vite + Bootstrap UI
docker-compose.yml   MySQL + backend + frontend
.github/workflows/ci.yml
```

## Prerequisites
| Tool | Version | Needed for |
|------|---------|-----------|
| JDK | 21 | local backend run |
| Maven | 3.9+ | local backend run |
| Node.js | 20+ | local frontend run |
| MySQL | 8+ | local run (not needed with Docker) |
| Docker Desktop | recent | easiest way to run everything |

## Option A — Run everything with Docker (easiest)
```bash
docker compose up --build
```
- Frontend: http://localhost:3000
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html

## Option B — Run locally (3 terminals)
1. **MySQL**: make sure it runs on `localhost:3306` with user `root` / password `root`
   (the `oms_db` database is created automatically). Different credentials? Set `DB_USERNAME` and `DB_PASSWORD`.
2. **Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. **Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173

## Demo accounts (seeded on first start)
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@oms.com | Admin@123 |
| Customer | customer@oms.com | Customer@123 |

Change these before deploying anywhere public.

## Run the tests
```bash
cd backend && mvn test
```

## Main API endpoints
| Method | Path | Access |
|--------|------|--------|
| POST | /api/auth/register, /api/auth/login | public |
| GET | /api/products?categoryId=&q=&page=&size= , /api/products/{id}, /api/categories | public |
| POST | /api/orders | customer |
| GET | /api/orders/my, /api/orders/{id} | customer (own orders) |
| POST | /api/orders/{id}/cancel | customer (own) / admin |
| POST/PUT/DELETE | /api/admin/products | admin |
| POST | /api/admin/categories | admin |
| GET/PUT | /api/admin/inventory, /api/admin/inventory/{productId} | admin |
| GET | /api/admin/orders?status= | admin |
| PUT | /api/admin/orders/{id}/status | admin |

## Configuration (environment variables)
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ chars), `CORS_ORIGINS`, and `VITE_API_URL` for the frontend.

## Notes / ideas for next steps
- Add Testcontainers integration tests against a real MySQL
- Add refresh tokens and password reset
- Add product images upload and order invoices (PDF)
