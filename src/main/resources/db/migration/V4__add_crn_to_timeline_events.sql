ALTER TABLE timeline_events
    ADD COLUMN crn VARCHAR(7);

CREATE INDEX idx_timeline_events_crn
    ON timeline_events (crn);