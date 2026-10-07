ALTER TABLE timeline_events
    ADD COLUMN user_pdus JSONB,
    ADD COLUMN user_regions JSONB;
