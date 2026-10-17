-- Optional REFERENCE schema for a future Flyway-based design. NOT used at runtime.
-- The backend now auto-creates its own tables (app_users, study_documents, document_chunks,
-- learn_quizzes, quiz_questions_v2, quiz_attempts_v2) via Hibernate ddl-auto=update.
-- Prefer Flyway migrations before production use.

CREATE TABLE IF NOT EXISTS topics (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    prerequisite_topic_id UUID REFERENCES topics(id)
);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    storage_key TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY,
    topic_id UUID REFERENCES topics(id),
    source_document_id UUID REFERENCES documents(id),
    source_page_number INTEGER,
    question_text TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(10) NOT NULL,
    explanation TEXT,
    difficulty SMALLINT NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id),
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id),
    position INTEGER NOT NULL,
    PRIMARY KEY (quiz_id, question_id)
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    quiz_id UUID NOT NULL REFERENCES quizzes(id),
    score NUMERIC(5,2),
    submitted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS attempt_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id),
    selected_answer VARCHAR(10),
    is_correct BOOLEAN,
    response_time_ms INTEGER,
    UNIQUE (attempt_id, question_id)
);

CREATE TABLE IF NOT EXISTS topic_mastery (
    user_id UUID NOT NULL REFERENCES app_users(id),
    topic_id UUID NOT NULL REFERENCES topics(id),
    mastery_score NUMERIC(5,2) NOT NULL CHECK (mastery_score BETWEEN 0 AND 100),
    last_practiced_at TIMESTAMPTZ,
    attempts_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, topic_id)
);

CREATE TABLE IF NOT EXISTS study_tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    topic_id UUID REFERENCES topics(id),
    task_type VARCHAR(50) NOT NULL,
    due_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_documents_user_created ON documents(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_attempts_user_submitted ON quiz_attempts(user_id, submitted_at);
CREATE INDEX IF NOT EXISTS idx_study_tasks_user_status_due ON study_tasks(user_id, status, due_at);
