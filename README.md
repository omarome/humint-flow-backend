# HumintFlow — Backend API

Spring Boot 3 REST API for the HumintFlow CRM workspace.

## Tech Stack

- **Java 21** + Spring Boot 3
- **PostgreSQL** (via Spring Data JPA / Hibernate)
- **Spring Security** — HTTP Basic Auth + Firebase ID token verification
- **Firebase Admin SDK** — Token verification, FCM push notifications, Firestore sync
- **Hibernate Envers** — Audit history on all CRM entities
- **Docker** + Docker Compose

---

## Local Development

### Prerequisites
- Java 21
- Maven 3.6+
- PostgreSQL 17 (or Docker)

### Setup

1. Create `.env` in `humint-flow-backend/`:
   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/humintflow_db"
   export DB_USERNAME="your_username"
   export DB_PASSWORD="your_password"
   export API_USERNAME="admin"
   export API_PASSWORD="your_secure_password"
   export APP_CORS_ALLOWED_ORIGINS="http://localhost:5174"
   ```

2. Run:
   ```bash
   source .env && ./mvnw spring-boot:run
   ```

API available at `http://localhost:8080/api/`

---

## API Endpoints

All endpoints require HTTP Basic Authentication unless noted.

### Authentication & Users
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users` | List all users (team members / employee directory) |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/variables` | Field definitions for the Query Builder |
| `GET` | `/api/variables/{id}` | Get variable by ID |

### Saved Views
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/saved-views` | List all saved views for the authenticated user |
| `POST` | `/api/saved-views` | Create a saved view (`{ name, queryJson }`) |
| `DELETE` | `/api/saved-views/{id}` | Delete a saved view |

> `queryJson` encodes either a react-querybuilder query (Employee Directory) or `{ entityType, filters }` (Sales pages).

### Organizations
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/organizations` | List organizations (paginated, sortable) |
| `GET` | `/api/organizations/{id}` | Get organization by ID |
| `POST` | `/api/organizations` | Create organization |
| `PUT` | `/api/organizations/{id}` | Update organization |
| `DELETE` | `/api/organizations/{id}` | Soft-delete organization |

### Contacts
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/contacts` | List contacts (paginated, sortable) |
| `GET` | `/api/contacts/{id}` | Get contact by ID |
| `POST` | `/api/contacts` | Create contact |
| `PUT` | `/api/contacts/{id}` | Update contact |
| `DELETE` | `/api/contacts/{id}` | Soft-delete contact |

### Opportunities (Pipeline)
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/opportunities` | List opportunities (paginated, sortable) |
| `GET` | `/api/opportunities/{id}` | Get opportunity by ID |
| `POST` | `/api/opportunities` | Create opportunity |
| `PUT` | `/api/opportunities/{id}` | Update opportunity |
| `DELETE` | `/api/opportunities/{id}` | Soft-delete opportunity |

### Activities
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/activities` | List activities |
| `GET` | `/api/activities/{entityType}/{entityId}` | Get activities for a specific entity |
| `POST` | `/api/activities` | Log a new activity (NOTE, EMAIL, CALL, MEETING, TASK) |
| `DELETE` | `/api/activities/{id}` | Delete activity |

### Automations
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/automations` | List automation rules |
| `POST` | `/api/automations` | Create automation rule |
| `PUT` | `/api/automations/{id}` | Update automation rule |
| `DELETE` | `/api/automations/{id}` | Delete automation rule |

### Firebase / Notifications
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/fcm/register` | Register or refresh an FCM device token |

---

## Common Query Parameters

Paginated list endpoints support:

| Parameter | Description | Example |
|-----------|-------------|---------|
| `page` | Page number (0-indexed) | `?page=0` |
| `size` | Items per page | `?size=500` |
| `sortBy` | Field to sort by | `?sortBy=name` |
| `sortDir` | Sort direction | `?sortDir=asc` |

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/query_builder_db` | PostgreSQL JDBC URL |
| `DB_USERNAME` | — | Database username |
| `DB_PASSWORD` | — | Database password |
| `API_USERNAME` | `admin` | HTTP Basic auth username |
| `API_PASSWORD` | — | HTTP Basic auth password |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5174,http://localhost:3000` | Allowed CORS origins |
| `PORT` | `8080` | Server port (Cloud Run compatible) |

---

## Data Model

```
auth_accounts          — Authenticated users / team members
  ├── firebase_uid     — Firebase UID (linked to Firebase Auth)
  ├── fcm_token        — FCM device token for push notifications
  ├── job_title, department, phone, avatar_url
  └── manager_id       → auth_accounts (self-referential)

organizations          — Company accounts
  └── assignedTo       → auth_accounts

contacts               — People at organizations
  ├── organization_id  → organizations
  └── assignedTo       → auth_accounts

opportunities          — Pipeline deals
  ├── organization_id  → organizations
  ├── primaryContact   → contacts
  └── assignedTo       → auth_accounts (deal owner)

activities             — Polymorphic activity log (NOTE/EMAIL/CALL/MEETING/TASK)
  ├── entity_type + entity_id  — points to any CRM entity
  └── assigned_to      → auth_accounts

automation_rules       — No-code trigger/action workflow rules
saved_views            — Persisted filter queries (queryJson)
variables              — Field definitions for the Query Builder
```

> All CRM entities extend `BaseEntity` with: UUID primary key, `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted` (soft delete).  
> Hibernate Envers `@Audited` provides full revision history on all entities.

---

## Security

- **HTTP Basic Auth** for all `/api/**` endpoints
- **Firebase ID token verification** via Admin SDK (`FirebaseTokenFilter`)
- **Rate limiting** — 100 requests/minute per IP
- **Input validation** — Bean Validation on all request bodies
- **Soft deletes** — Records are never hard-deleted; `is_deleted = TRUE` flag used
- **CORS** — Configurable via `APP_CORS_ALLOWED_ORIGINS`

Never commit credentials. Use environment variables or a secrets manager in production.

---

## Docker / Cloud Run

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The app binds to `${PORT:8080}` for Cloud Run compatibility.

```bash
# Build
mvn clean package -DskipTests

# Docker build
docker build -t humintflow-backend .

# Run
docker run -p 8080:8080 --env-file .env humintflow-backend
```

---

## License

MIT
