CREATE TABLE timeline_events
(
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    activity_code VARCHAR(100) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    detail JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_timeline_events_user_name_occurred_at
    ON timeline_events (user_name, occurred_at DESC);