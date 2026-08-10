DROP INDEX IF EXISTS idx_timeline_events_user_name_occurred_at;

CREATE INDEX idx_timeline_events_user_name
    ON timeline_events (user_name);

CREATE INDEX idx_timeline_events_occurred_at
    ON timeline_events (occurred_at DESC);