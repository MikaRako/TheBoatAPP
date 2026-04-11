# ⚓ Boat Management Application

A full-stack fleet management application built with **Spring Boot 3**, **Angular 21**, **Keycloak**, and **PostgreSQL** — fully containerized with Docker Compose.

---

## 📁 Project Structure

```
boat-management/
├── backend/                        # Spring Boot 3 / Java 21
│   ├── src/
│   │   ├── main/java/com/boatmanagement/
│   │   │   ├── BoatManagementApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   └── BoatController.java
│   │   │   ├── service/
│   │   │   │   └── BoatService.java
│   │   │   ├── repository/
│   │   │   │   └── BoatRepository.java
│   │   │   ├── entity/
│   │   │   │   └── Boat.java
│   │   │   ├── dto/
│   │   │   │   └── BoatDto.java
│   │   │   ├── mapper/
│   │   │   │   └── BoatMapper.java
│   │   │   └── exception/
│   │   │       ├── BoatNotFoundException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── main/resources/
│   │       └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                       # Angular 21
│   ├── src/app/
│   │   ├── app.component.ts        # Root + toolbar
│   │   ├── app.config.ts           # DI providers
│   │   ├── app.routes.ts           # Router
│   │   ├── auth/
│   │   │   └── login/login.component.ts
│   │   ├── boats/
│   │   │   ├── list/boat-list.component.ts
│   │   │   ├── detail/boat-detail.component.ts
│   │   │   └── form/boat-form.component.ts
│   │   └── shared/
│   │       ├── services/
│   │       │   ├── auth.service.ts
│   │       │   └── boat.service.ts
│   │       ├── interceptors/
│   │       │   └── auth.interceptor.ts
│   │       ├── guards/
│   │       │   └── auth.guard.ts
│   │       └── components/
│   │           ├── loading-spinner.component.ts
│   │           └── confirm-dialog.component.ts
│   ├── src/environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   ├── Dockerfile
│   └── nginx.conf
│
├── keycloak/
│   └── realm-export.json.template  # Realm config template (passwords injected at startup)
│
├── infra/
│   └── init.sql                    # DB init script
│
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🚀 Quick Start

### 1. Prerequisites

- Docker ≥ 24
- Docker Compose ≥ 2.20
- (For local dev) Java 21, Node.js 20, Maven 3.9

### 2. Clone & configure

```bash
git clone <repo-url>
cd boat-management
cp .env.example .env
# Edit .env — set all passwords before first start
```

> **Never commit `.env`** — it is gitignored. All secrets stay on your machine.

### 3. Start everything

```bash
docker compose up --build
```

> ⏱ First build takes 3–5 minutes (Maven + npm downloads). Subsequent starts are fast.

### 4. Access the application

| Service    | URL / Host                            | Notes                                                             |
|------------|---------------------------------------|-------------------------------------------------------------------|
| Frontend   | http://localhost:4200                 | Angular app                                                       |
| Backend    | http://localhost:8081                 | Spring Boot API                                                   |
| Swagger    | http://localhost:8081/swagger-ui.html | API documentation                                                 |
| Keycloak   | http://localhost:8080                 | Admin: see `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` in `.env` |
| PostgreSQL | `localhost:5432`                      | DB: see `DB_NAME` / `DB_USER` / `DB_PASSWORD` in `.env`           |

---

## 🔐 Keycloak Setup (Auto-configured)

The realm is **automatically imported** on first start. At container startup, `keycloak/realm-export.json.template` is processed with `sed` to inject passwords from `.env`, and the result is imported by Keycloak.

No passwords are hardcoded in the repository.

### Pre-configured test users

Credentials are set via `.env` (see `.env.example` for variable names):

| Username   | `.env` variable          | Roles                 |
|-----------|---------------------------|-----------------------|
| boatuser  | `KC_BOATUSER_PASSWORD`   | ROLE_USER             |
| boatadmin | `KC_BOATADMIN_PASSWORD`  | ROLE_USER, ROLE_ADMIN |

> To change passwords: update `.env`, then run `docker compose down -v && docker compose up` to wipe and reimport the realm.

### Manual Keycloak setup (if needed)

If you prefer to configure Keycloak manually:

```
1. Open http://localhost:8080
2. Login with admin / admin
3. Create realm: "boat-realm"

4. Create client: boat-frontend
   - Client type: OpenID Connect
   - Client authentication: OFF (public client)
   - Standard flow: ON
   - Valid redirect URIs: http://localhost:4200/*
   - Valid post logout redirect URIs: http://localhost:4200/*
   - Web origins: http://localhost:4200

5. Create client: boat-backend
   - Client type: OpenID Connect
   - Client authentication: ON (confidential)
   - Standard flow: OFF
   - Bearer-only: ON

6. Create realm roles: ROLE_USER, ROLE_ADMIN

7. Create users and assign roles
```

---

## 🛠 Local Development (without Docker)

### Backend

```bash
cd backend

# Start only infrastructure
docker compose up postgres keycloak -d

# Run Spring Boot
./mvnw spring-boot:run
# Or: mvn spring-boot:run
```

Backend runs at: http://localhost:8081

### Frontend

```bash
cd frontend
npm install --legacy-peer-deps
npm start
```

Frontend runs at: http://localhost:4200 (proxies /api to localhost:8081)

---

## 📡 API Reference

### Authentication
All endpoints require a Bearer JWT token from Keycloak.

### Endpoints

#### List boats (paginated + search)
```bash
curl -X GET "http://localhost:8081/api/boats?search=sea&page=0&size=10&sortBy=name&sortDir=asc" \
  -H "Authorization: Bearer <token>"
```

#### Get boat by ID
```bash
curl -X GET "http://localhost:8081/api/boats/1" \
  -H "Authorization: Bearer <token>"
```

#### Create boat
```bash
curl -X POST "http://localhost:8081/api/boats" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Sea Explorer", "description": "A luxury sailing boat"}'
```

#### Update boat
```bash
curl -X PUT "http://localhost:8081/api/boats/1" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Sea Explorer II", "description": "Updated description"}'
```

#### Delete boat
```bash
curl -X DELETE "http://localhost:8081/api/boats/1" \
  -H "Authorization: Bearer <token>"
```

### Get a token for testing
```bash
curl -X POST "http://localhost:8080/realms/boat-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=boat-frontend" \
  -d "username=boatuser" \
  -d "password=<KC_BOATUSER_PASSWORD from .env>"
```

---

## 🧪 Running Tests

### Backend (Testcontainers)

```bash
cd backend
./mvnw test
# Tests spin up a real PostgreSQL container via Testcontainers
```

### Frontend

```bash
cd frontend
npm test
```

---

## 🐳 Docker Commands

```bash
# Start all services
docker compose up --build

# Start in background
docker compose up -d --build

# View logs
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f keycloak

# Stop all services
docker compose down

# Stop and remove volumes (WIPES DATA — required after changing passwords in .env)
docker compose down -v

# Rebuild a single service
docker compose up --build backend
```

---

## 🔒 Security Notes

- All API endpoints require a valid JWT token (401 if missing, 403 if forbidden)
- CORS is configured for localhost:4200 only (adjust for production)
- JWT is validated against Keycloak's JWKS endpoint
- Tokens are automatically refreshed by `angular-oauth2-oidc`
- HTTP interceptor attaches the Bearer token to all API requests
- Route guard prevents access to protected pages without authentication

---

## 🏗 Architecture Decisions

| Decision              | Choice                          | Reason                              |
|----------------------|---------------------------------|-------------------------------------|
| Auth protocol        | OIDC / OAuth2 Authorization Code + PKCE | Industry standard, secure for SPAs |
| JWT validation       | Spring OAuth2 Resource Server   | Auto-validates against Keycloak JWKS |
| ORM                  | Spring Data JPA + Hibernate     | Reduces boilerplate, clean queries  |
| DTO mapping          | MapStruct                       | Type-safe, compile-time, zero-overhead |
| Frontend state       | RxJS + Services                 | Lightweight, no NgRx complexity needed |
| Error format         | RFC 9457 ProblemDetail          | Standardized HTTP problem responses |

---

## ⚙️ Environment Variables

All variables are set in `.env` (copy from `.env.example`). The `.env` file is gitignored and never committed.

| Variable                   | Description                                          |
|---------------------------|------------------------------------------------------|
| `DB_NAME`                 | PostgreSQL database name                             |
| `DB_USER`                 | PostgreSQL username                                  |
| `DB_PASSWORD`             | PostgreSQL password                                  |
| `KEYCLOAK_ADMIN`          | Keycloak admin console username                      |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin console password                      |
| `KEYCLOAK_REALM`          | Keycloak realm name                                  |
| `KC_BOATUSER_PASSWORD`    | Password for the `boatuser` test account             |
| `KC_BOATADMIN_PASSWORD`   | Password for the `boatadmin` test account            |
| `SERVER_PORT`             | Backend server port                                  |
| `CORS_ALLOWED_ORIGINS`    | Allowed CORS origins                                 |
