CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    password    VARCHAR(255),
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    yandex_id   VARCHAR(100) UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Идемпотентные апгрейды для уже существующих БД
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS yandex_id VARCHAR(100) UNIQUE;
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(512) UNIQUE NOT NULL,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS templates (
    id          BIGSERIAL    PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    content     JSONB        NOT NULL DEFAULT '[]',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_templates_owner   ON templates(owner_id);
CREATE INDEX IF NOT EXISTS idx_templates_public  ON templates(is_public) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_templates_content ON templates USING GIN(content);

CREATE TABLE IF NOT EXISTS characters (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id BIGINT      NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE'
                             CHECK (visibility IN ('PRIVATE', 'PUBLIC', 'RESTRICTED')),
    field_values JSONB       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS character_access (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (character_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_characters_owner      ON characters(owner_id);
CREATE INDEX IF NOT EXISTS idx_characters_public     ON characters(visibility) WHERE visibility = 'PUBLIC';
CREATE INDEX IF NOT EXISTS idx_character_access_user ON character_access(user_id);
CREATE INDEX IF NOT EXISTS idx_characters_field_vals ON characters USING GIN(field_values);

-- ── Модуль «Организация мероприятий» ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS events (
    id                 BIGSERIAL PRIMARY KEY,
    owner_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title              VARCHAR(150) NOT NULL,
    description        TEXT,
    location           VARCHAR(255),
    starts_at          TIMESTAMP    NOT NULL,
    ends_at            TIMESTAMP,
    event_type         VARCHAR(20)  NOT NULL DEFAULT 'CLOSED'
                                    CHECK (event_type IN ('OPEN', 'CLOSED')),
    allow_applications BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_owner ON events(owner_id);
CREATE INDEX IF NOT EXISTS idx_events_type  ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_start ON events(starts_at);

CREATE TABLE IF NOT EXISTS event_invitations (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT       NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT       NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'INVITED'
                            CHECK (status IN ('INVITED', 'ACCEPTED', 'DECLINED')),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_invites_user ON event_invitations(user_id);

CREATE TABLE IF NOT EXISTS event_applications (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    message    TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_applications_user ON event_applications(user_id);