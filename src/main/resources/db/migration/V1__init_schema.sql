-- ============================================================
-- Chat Sessions
-- ============================================================
CREATE TABLE chat_sessions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(255) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    favorite    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);

-- ============================================================
-- Chat Messages
-- ============================================================
CREATE TABLE chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID        NOT NULL,
    sender      VARCHAR(10) NOT NULL CHECK (sender IN ('USER', 'BOT')),
    content     TEXT        NOT NULL,
    context     JSONB,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (session_id)
        REFERENCES chat_sessions(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_context    ON chat_messages USING GIN(context);