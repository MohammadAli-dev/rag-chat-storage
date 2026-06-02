# Database Schema Design

## chat_sessions

| Column     | Type        | Notes                    |
|------------|-------------|--------------------------|
| id         | UUID        | Primary key              |
| user_id    | VARCHAR     | Owner of the session     |
| title      | VARCHAR     | Display name             |
| favorite   | BOOLEAN     | Default false            |
| created_at | TIMESTAMP   | Auto-set on insert       |
| updated_at | TIMESTAMP   | Auto-set on update       |

## chat_messages

| Column     | Type        | Notes                             |
|------------|-------------|-----------------------------------|
| id         | UUID        | Primary key                       |
| session_id | UUID        | FK → chat_sessions.id             |
| sender     | VARCHAR     | 'USER' or 'BOT'                   |
| content    | TEXT        | The message body                  |
| context    | JSONB       | RAG context chunks (chunkId, content, sourceUrl, score) |
| created_at | TIMESTAMP   | Auto-set on insert                |

## Relationships

ChatSession (1) ──── (N) ChatMessage

## Indexes

- chat_sessions(user_id)         → fast user session lookup
- chat_messages(session_id)      → fast message retrieval per session
- chat_messages(context)         → GIN index for querying inside JSONB chunks