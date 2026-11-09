ALTER TABLE timeline_events
    ADD COLUMN crn_pdus JSONB,
    ADD COLUMN tier TEXT;
