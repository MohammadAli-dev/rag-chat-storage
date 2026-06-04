# RAG Chat Storage Service

A production-style Spring Boot 4 REST API for storing and managing RAG (Retrieval-Augmented Generation) chat sessions and messages. Designed as a backend storage layer for AI chat applications that need to persist conversation history along with the retrieval context used to generate responses.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [API Contract](#api-contract)
- [API Endpoints](#api-endpoints)
- [Example Requests](#example-requests)
- [Rate Limiting](#rate-limiting)
- [Request Tracing](#request-tracing)
- [CORS](#cors)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Design Decisions](#design-decisions)
- [Testing](#testing)
- [Building for Production](#building-for-production)

---

## Architecture Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    Client / RAG Engine                       │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP + X-API-KEY
┌──────────────────────────▼──────────────────────────────────┐
│                  Spring Boot 4 (port 8082)                   │
│                                                              │
│  Filter Chain:                                               │
│  RequestIdFilter → ApiKeyFilter → RateLimitFilter            │
│                                                              │
│  ┌──────────────────────┐  ┌───────────────────────┐        │
│  │ ChatSessionController│  │ ChatMessageController  │        │
│  │ ChatSessionService   │  │ ChatMessageService     │        │
│  │ ChatSessionRepository│  │ ChatMessageRepository  │        │
│  └──────────────────────┘  └───────────────────────┘        │
│                                                              │
│  GlobalExceptionHandler · Springdoc OpenAPI                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ JPA + Flyway Migrations
┌──────────────────────────▼──────────────────────────────────┐
│                    PostgreSQL 16 (Docker)                     │
│                                                              │
│  chat_sessions              chat_messages                    │
│  ─────────────              ─────────────                    │
│  id (UUID, PK)              id (UUID, PK)                    │
│  user_id                    session_id (FK → sessions)       │
│  title                      sender (USER | BOT)              │
│  favorite                   content (TEXT)                   │
│  created_at                 context (JSONB)                  │
│  updated_at                 created_at                       │
└─────────────────────────────────────────────────────────────┘
```

### Filter Chain

Every request passes through three ordered filters before reaching a controller:

| Order | Filter | Responsibility |
|-------|--------|---------------|
| 1 | `RequestIdFilter` | Generates or propagates a `X-Request-ID` header; injects it into MDC for structured logging |
| 2 | `ApiKeyFilter` | Validates the `X-API-KEY` header against the configured key; sets Spring Security authentication on success; returns `401` on failure. Skipped for public paths |
| 3 | `RateLimitFilter` | Token-bucket rate limiter (Bucket4j), defaulting to 100 requests/minute per API key; returns `429` when exceeded |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring MVC (embedded Tomcat) |
| Security | Spring Security (stateless, API key auth) |
| Database | PostgreSQL 16 |
| ORM | Hibernate 7 (via Spring Data JPA) |
| Migrations | Flyway |
| Rate Limiting | Bucket4j 8.10.1 |
| API Documentation | Springdoc OpenAPI 3.0.1 (Swagger UI) |
| Validation | Jakarta Bean Validation |
| Utilities | Lombok, Jackson JSR-310 |
| Build | Maven |
| Containers | Docker Compose (PostgreSQL + Adminer) |

---

## Project Structure

```
rag-chat-storage/
├── docker-compose.yml              # PostgreSQL + Adminer containers
├── pom.xml                         # Maven build configuration
├── src/
│   ├── main/
│   │   ├── java/com/assessment/ragchat/
│   │   │   ├── RagchatApplication.java         # Entry point
│   │   │   ├── common/
│   │   │   │   ├── ErrorResponse.java          # Standard error envelope
│   │   │   │   └── PagedResponse.java          # Paginated response wrapper
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java # Centralized error handling
│   │   │   │   ├── SessionNotFoundException.java
│   │   │   │   └── MessageNotFoundException.java
│   │   │   ├── security/
│   │   │   │   ├── SecurityConfig.java         # Spring Security filter chain
│   │   │   │   ├── ApiKeyFilter.java           # API key authentication
│   │   │   │   ├── RateLimitFilter.java        # Rate limiting (Bucket4j)
│   │   │   │   └── RequestIdFilter.java        # Request correlation IDs
│   │   │   ├── session/
│   │   │   │   ├── controller/
│   │   │   │   │   └── ChatSessionController.java
│   │   │   │   ├── service/
│   │   │   │   │   └── ChatSessionService.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── ChatSessionRepository.java
│   │   │   │   ├── entity/
│   │   │   │   │   └── ChatSession.java
│   │   │   │   └── dto/
│   │   │   │       ├── CreateSessionRequest.java
│   │   │   │       ├── CreateSessionResponse.java
│   │   │   │       ├── RenameSessionRequest.java
│   │   │   │       └── SessionResponse.java
│   │   │   └── message/
│   │   │       ├── controller/
│   │   │       │   └── ChatMessageController.java
│   │   │       ├── service/
│   │   │       │   └── ChatMessageService.java
│   │   │       ├── repository/
│   │   │       │   └── ChatMessageRepository.java
│   │   │       ├── entity/
│   │   │       │   ├── ChatMessage.java
│   │   │       │   └── ContextChunkConverter.java
│   │   │       └── dto/
│   │   │           ├── AddMessageRequest.java
│   │   │           ├── AddMessageResponse.java
│   │   │           ├── MessageResponse.java
│   │   │           ├── ContextChunk.java
│   │   │           └── SenderType.java
│   │   └── resources/
│   │       ├── application.yml                  # Application configuration
│   │       └── db/migration/
│   │           └── V1__init_schema.sql          # Flyway migration
│   └── test/
│       ├── java/com/assessment/ragchat/
│       │   ├── ChatFlowIntegrationTest.java     # End-to-end integration tests
│       │   └── RagchatApplicationTests.java     # Context load test
│       └── resources/
│           └── application-test.yml             # Test profile configuration
```

---

## Prerequisites

| Requirement | Version | Notes |
|------------|---------|-------|
| Java | 21+ | LTS release |
| Maven | 3.9+ | Or use the included `./mvnw` wrapper |
| Docker Desktop | Latest | Required for PostgreSQL and Adminer |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/MohammadAli-dev/rag-chat-storage.git
cd rag-chat-storage
```

### 2. Start the database

```bash
docker compose up -d
```

This starts two containers:

| Container | Purpose | URL |
|-----------|---------|-----|
| `ragchat-postgres` | PostgreSQL 16 database | `localhost:5432` |
| `ragchat-adminer` | Web-based DB admin tool | [http://localhost:8090](http://localhost:8090) |

Flyway will automatically apply migrations on first startup.

### 3. Start the application

```bash
./mvnw spring-boot:run
```

The application starts on port **8082**.

### 4. Verify it's running

```bash
curl http://localhost:8082/actuator/health
```

Expected response:
```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### 5. Open Swagger UI

Navigate to [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) to explore all endpoints interactively. Click **Authorize** and enter your API key to test authenticated endpoints.

---

## Authentication

All `/api/v1/**` endpoints require the `X-API-KEY` header:

```http
X-API-KEY: my-secret-key
```

The API key is configured in `application.yml` under `app.security.api-key`.

**Public endpoints** (no key required):

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Health check |
| `GET /swagger-ui/**` | Swagger UI |
| `GET /v3/api-docs/**` | OpenAPI specification |

Requests with a missing or invalid API key receive:
```json
{
  "timestamp": "2026-06-04T12:00:00",
  "status": 401,
  "message": "Invalid or missing API key"
}
```

---

## API Contract

The static OpenAPI contract is maintained in [`docs/openapi.yaml`](docs/openapi.yaml). It mirrors the implemented controller DTOs, including the structured RAG `context` array stored with each message.

Swagger UI is also available at [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) when the application is running.

---

## API Endpoints

### Sessions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/sessions` | Create a new chat session |
| `GET` | `/api/v1/sessions?userId={userId}` | List all sessions for a user (ordered by newest first) |
| `GET` | `/api/v1/sessions/{id}` | Get a single session by ID |
| `PATCH` | `/api/v1/sessions/{id}/rename` | Rename a session |
| `PATCH` | `/api/v1/sessions/{id}/favorite` | Toggle the favorite flag |
| `DELETE` | `/api/v1/sessions/{id}` | Delete a session and all its messages (cascade) |

### Messages

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/sessions/{id}/messages` | Add a message to a session |
| `GET` | `/api/v1/sessions/{id}/messages?page=0&size=20` | Get paginated messages for a session |

### Request/Response Formats

**Create Session** — `POST /api/v1/sessions`
```json
// Request
{ "userId": "user-1", "title": "My Chat Session" }

// Response (201 Created)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-1",
  "title": "My Chat Session",
  "favorite": false,
  "createdAt": "2026-06-04T12:00:00",
  "updatedAt": "2026-06-04T12:00:00"
}
```

**Add Message** — `POST /api/v1/sessions/{id}/messages`
```json
// Request
{
  "sender": "USER",
  "content": "What is the capital of France?",
  "context": [
    {
      "chunkId": "abc-123",
      "content": "Paris is the capital of France",
      "sourceUrl": "https://docs.example.com/france",
      "score": 0.95
    }
  ]
}

// Response (201 Created)
{
  "id": "...",
  "sessionId": "550e8400-...",
  "sender": "USER",
  "content": "What is the capital of France?",
  "context": [{ "chunkId": "abc-123", "content": "Paris is...", "sourceUrl": "...", "score": 0.95 }],
  "createdAt": "2026-06-04T12:00:00"
}
```

**Get Messages** — `GET /api/v1/sessions/{id}/messages?page=0&size=20`
```json
// Response (200 OK)
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}
```

---

## Example Requests

```bash
# Create a session
curl -X POST http://localhost:8082/api/v1/sessions \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: my-secret-key" \
  -d '{"userId": "user-1", "title": "My Chat Session"}'

# Add a message with RAG context
curl -X POST http://localhost:8082/api/v1/sessions/{id}/messages \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: my-secret-key" \
  -d '{
    "sender": "USER",
    "content": "What is the capital of France?",
    "context": [
      {
        "chunkId": "abc-123",
        "content": "Paris is the capital of France",
        "sourceUrl": "https://docs.example.com/france",
        "score": 0.95
      }
    ]
  }'

# List sessions for a user
curl "http://localhost:8082/api/v1/sessions?userId=user-1" \
  -H "X-API-KEY: my-secret-key"

# Get paginated messages
curl "http://localhost:8082/api/v1/sessions/{id}/messages?page=0&size=20" \
  -H "X-API-KEY: my-secret-key"

# Rename a session
curl -X PATCH http://localhost:8082/api/v1/sessions/{id}/rename \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: my-secret-key" \
  -d '{"title": "New Title"}'

# Toggle favorite
curl -X PATCH http://localhost:8082/api/v1/sessions/{id}/favorite \
  -H "X-API-KEY: my-secret-key"

# Delete a session (cascades to messages)
curl -X DELETE http://localhost:8082/api/v1/sessions/{id} \
  -H "X-API-KEY: my-secret-key"
```

---

## Rate Limiting

Each API key is limited to **100 requests per minute by default** (token-bucket algorithm via Bucket4j). Override `APP_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE` for production environments that need a different limit.

Exceeding the limit returns:
```json
{
  "timestamp": "2026-06-04T12:00:00",
  "status": 429,
  "message": "Too many requests - limit is 100 requests per minute"
}
```

---

## Request Tracing

Every request receives a unique `X-Request-ID` response header for distributed tracing. You can also pass your own ID to correlate across services:

```bash
curl http://localhost:8082/actuator/health \
  -H "X-Request-ID: my-custom-id-123"
# Response header: X-Request-ID: my-custom-id-123
```

The request ID is injected into the logging MDC, so all log lines for a single request share the same trace ID.

---

## CORS

Cross-Origin Resource Sharing is enabled for all origins by default. The configuration is defined in [`SecurityConfig.java`](src/main/java/com/assessment/ragchat/security/SecurityConfig.java) and can be overridden with `APP_CORS_ALLOWED_ORIGINS`, `APP_CORS_ALLOWED_METHODS`, and `APP_CORS_ALLOWED_HEADERS`.

| Setting | Value |
|---------|-------|
| Allowed Origins | `*` (all origins), configurable |
| Allowed Methods | `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS`, configurable |
| Allowed Headers | `*` (all headers), configurable |
| Path Pattern | `/**` (all endpoints) |

To test CORS with a preflight request:

```bash
curl -v -X OPTIONS http://localhost:8082/api/v1/sessions \
  -H "Origin: http://example.com" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: X-API-KEY, Content-Type"
```

The response will include headers such as:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: *
```

---

## Error Handling

All errors return a consistent JSON envelope:

```json
{
  "timestamp": "2026-06-04T12:00:00",
  "status": 404,
  "message": "Session not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

| Status Code | Scenario |
|-------------|----------|
| `400` | Validation failure (missing or invalid fields) |
| `401` | Missing or invalid API key |
| `404` | Session or message not found |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error |

---

## Database Schema

Tables are managed by Flyway migrations in `src/main/resources/db/migration/`.

### `chat_sessions`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, auto-generated |
| `user_id` | VARCHAR(255) | NOT NULL, indexed |
| `title` | VARCHAR(255) | NOT NULL |
| `favorite` | BOOLEAN | NOT NULL, default FALSE |
| `created_at` | TIMESTAMP | NOT NULL, default now() |
| `updated_at` | TIMESTAMP | NOT NULL, default now() |

### `chat_messages`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, auto-generated |
| `session_id` | UUID | NOT NULL, FK → chat_sessions (ON DELETE CASCADE), indexed |
| `sender` | VARCHAR(10) | NOT NULL, CHECK IN ('USER', 'BOT') |
| `content` | TEXT | NOT NULL |
| `context` | JSONB | Nullable, GIN indexed |
| `created_at` | TIMESTAMP | NOT NULL, default now() |

The `context` JSONB column stores the retrieval chunks that were used by the RAG engine to generate the response, enabling full auditability of what context influenced each answer.

### Adminer (Web DB Client)

Adminer is available at [http://localhost:8090](http://localhost:8090):

| Field | Value |
|-------|-------|
| System | PostgreSQL |
| Server | `postgres` |
| Username | `ragchat_user` |
| Password | `ragchat_pass` |
| Database | `ragchat` |

---

## Configuration

All settings are in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ${SPRING_APPLICATION_NAME:ragchat}
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/ragchat}
    username: ${SPRING_DATASOURCE_USERNAME:ragchat_user}
    password: ${SPRING_DATASOURCE_PASSWORD:ragchat_pass}
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:true}
    locations: ${SPRING_FLYWAY_LOCATIONS:classpath:db/migration}
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
    show-sql: ${SPRING_JPA_SHOW_SQL:false}
    open-in-view: ${SPRING_JPA_OPEN_IN_VIEW:false}

server:
  port: ${SERVER_PORT:8082}

springdoc:
  swagger-ui:
    path: ${SPRINGDOC_SWAGGER_UI_PATH:/swagger-ui.html}
  api-docs:
    path: ${SPRINGDOC_API_DOCS_PATH:/v3/api-docs}

app:
  security:
    api-key: ${APP_SECURITY_API_KEY:my-secret-key}
  rate-limit:
    max-requests-per-minute: ${APP_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE:100}
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:*}
    allowed-methods: ${APP_CORS_ALLOWED_METHODS:GET,POST,PATCH,DELETE,OPTIONS}
    allowed-headers: ${APP_CORS_ALLOWED_HEADERS:*}
```

Key design decisions:
- **`ddl-auto: validate`** — Hibernate validates the entity mapping against the Flyway-managed schema but never modifies it. This prevents accidental schema drift.
- **`open-in-view: false`** — Disables the Open Session in View anti-pattern, ensuring lazy-loading only works within explicit `@Transactional` boundaries.
- **Stateless sessions** — `SessionCreationPolicy.STATELESS` is configured since the API uses key-based auth, not session cookies.

---

## Design Decisions

PostgreSQL JSONB was chosen for RAG context storage because retrieval context is naturally semi-structured: chunks may contain identifiers, source URLs, scores, and content today, while future retrieval metadata can be added without a schema migration. JSONB also keeps the context attached to the message it explains, supports GIN indexing, and preserves auditability for generated answers.

API key authentication was selected because this service is intended as a backend storage API consumed by trusted clients or internal RAG services rather than by end users directly. It keeps authentication stateless, simple to operate, and easy to rotate through environment configuration while still protecting all `/api/v1/**` endpoints.

The application is implemented as a modular monolith because sessions, messages, security, and persistence are tightly related in this submission-sized service. Keeping them in one deployable avoids premature distributed-system complexity while preserving clear package boundaries, service layers, repositories, DTOs, and migration ownership if the codebase later needs to split.

---

## Testing

The project includes integration tests that exercise the full request lifecycle (filters → controllers → services → database) using MockMvc.

```bash
# Ensure PostgreSQL is running, then:
./mvnw test
```

Expected output:
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Test coverage:**

| Test | What it verifies |
|------|-----------------|
| `shouldCreateSession` | POST creates a session with correct fields |
| `shouldAddMessage` | POST adds a USER message to a session |
| `shouldAddBotMessage` | POST adds a BOT message to the same session |
| `shouldGetMessages` | GET returns paginated messages |
| `shouldRenameSession` | PATCH updates the session title |
| `shouldToggleFavorite` | PATCH toggles the favorite flag |
| `shouldDeleteSession` | DELETE removes the session |
| `shouldReturn404AfterDeletion` | GET after deletion returns 404 |
| `shouldReturn401WithoutApiKey` | Requests without API key are rejected |
| `shouldReturn400WithInvalidRequest` | Validation errors return 400 |
| `contextLoads` | Application context starts successfully |

Tests use a separate `application-test.yml` profile with `api-key: test-api-key`.

---

## Building for Production

```bash
# Build the executable JAR
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/ragchat-0.0.1-SNAPSHOT.jar
```

For production, override the API key via environment variable:
```bash
APP_SECURITY_API_KEY=your-production-key java -jar target/ragchat-0.0.1-SNAPSHOT.jar
```
