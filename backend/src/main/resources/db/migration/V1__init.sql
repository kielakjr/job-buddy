CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           VARCHAR(255) UNIQUE NOT NULL,
                       name            VARCHAR(255),
                       avatar_url      TEXT,
                       google_id       VARCHAR(255) UNIQUE,
                       linkedin_id     VARCHAR(255) UNIQUE,
                       cv_text         TEXT,
                       timezone        VARCHAR(50) DEFAULT 'Europe/Warsaw',
                       created_at      TIMESTAMPTZ DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TYPE application_status AS ENUM (
    'DRAFT', 'APPLIED', 'TASK_GIVEN', 'TASK_DONE', 'PHONE_SCREEN', 'TECHNICAL_INTERVIEW',
    'FINAL_INTERVIEW', 'OFFER', 'ACCEPTED', 'REJECTED',
    'WITHDRAWN', 'GHOSTED'
);

CREATE TABLE applications (
                              id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              company             VARCHAR(255) NOT NULL,
                              position            VARCHAR(255) NOT NULL,
                              location            VARCHAR(255),
                              remote              BOOLEAN DEFAULT FALSE,
                              status              application_status NOT NULL DEFAULT 'DRAFT',
                              source              VARCHAR(100),
                              offer_url           TEXT,
                              salary_min          INTEGER,
                              salary_max          INTEGER,
                              salary_currency     VARCHAR(3) DEFAULT 'PLN',
                              applied_at          DATE,
                              notes               TEXT,
                              gmail_thread_id     VARCHAR(255),
                              created_at          TIMESTAMPTZ DEFAULT NOW(),
                              updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_applied_at ON applications(applied_at DESC);

CREATE TABLE application_events (
                                    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                    event_type      VARCHAR(50) NOT NULL,
                                    description     TEXT,
                                    metadata        JSONB,
                                    occurred_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_events_application_id ON application_events(application_id);
CREATE INDEX idx_events_occurred_at ON application_events(occurred_at DESC);

CREATE TABLE tags (
                      id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                      user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      name        VARCHAR(50) NOT NULL,
                      color       VARCHAR(7),
                      UNIQUE(user_id, name)
);

CREATE TABLE application_tags (
                                  application_id  UUID REFERENCES applications(id) ON DELETE CASCADE,
                                  tag_id          UUID REFERENCES tags(id) ON DELETE CASCADE,
                                  PRIMARY KEY (application_id, tag_id)
);

CREATE TABLE reminders (
                           id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                           user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           reminder_type   VARCHAR(50) NOT NULL,
                           title           VARCHAR(255) NOT NULL,
                           description     TEXT,
                           remind_at       TIMESTAMPTZ NOT NULL,
                           sent            BOOLEAN DEFAULT FALSE,
                           sent_at         TIMESTAMPTZ,
                           created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_reminders_remind_at ON reminders(remind_at) WHERE sent = FALSE;

CREATE TABLE gmail_integrations (
                                    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    user_id                     UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    encrypted_refresh_token     TEXT NOT NULL,
                                    history_id                  VARCHAR(50),
                                    watch_expires_at            TIMESTAMPTZ,
                                    enabled                     BOOLEAN DEFAULT TRUE,
                                    connected_at                TIMESTAMPTZ DEFAULT NOW(),
                                    last_sync_at                TIMESTAMPTZ
);

CREATE TABLE gmail_processed_messages (
                                          id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          gmail_msg_id    VARCHAR(255) NOT NULL,
                                          from_address    VARCHAR(255),
                                          subject         TEXT,
                                          received_at     TIMESTAMPTZ,
                                          matched_to_app  UUID REFERENCES applications(id),
                                          action_taken    VARCHAR(100),
                                          processed_at    TIMESTAMPTZ DEFAULT NOW(),
                                          UNIQUE(user_id, gmail_msg_id)
);

CREATE INDEX idx_gmail_processed_user ON gmail_processed_messages(user_id, processed_at DESC);