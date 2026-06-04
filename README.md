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
- [API Endpoints](#api-endpoints)
- [Example Requests](#example-requests)
- [Rate Limiting](#rate-limiting)
- [Request Tracing](#request-tracing)
- [CORS](#cors)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
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
| 3 | `RateLimitFilter` | Token-bucket rate limiter (Bucket4j) — 100 requests/minute per API key; returns `429` when exceeded |

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
git clone <repository-url>
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

Each API key is limited to **100 requests per minute** (token-bucket algorithm via Bucket4j).

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

Cross-Origin Resource Sharing is enabled for all origins. The configuration is defined in [SecurityConfig.java](file:///Users/mohammadali/rag-chat-storage/src/main/java/com/assessment/ragchat/security/SecurityConfig.java).

| Setting | Value |
|---------|-------|
| Allowed Origins | `*` (all origins) |
| Allowed Methods | `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS` |
| Allowed Headers | `*` (all headers) |
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
  datasource:
    url: jdbc:postgresql://localhost:5432/ragchat
    username: ragchat_user
    password: ragchat_pass
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate          # Schema managed by Flyway; JPA only validates
    open-in-view: false           # Prevents lazy-loading outside transactions

server:
  port: 8082

app:
  security:
    api-key: my-secret-key        # Change this in production
```

Key design decisions:
- **`ddl-auto: validate`** — Hibernate validates the entity mapping against the Flyway-managed schema but never modifies it. This prevents accidental schema drift.
- **`open-in-view: false`** — Disables the Open Session in View anti-pattern, ensuring lazy-loading only works within explicit `@Transactional` boundaries.
- **Stateless sessions** — `SessionCreationPolicy.STATELESS` is configured since the API uses key-based auth, not session cookies.

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
