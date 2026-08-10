ALTER TABLE timeline_events
    RENAME COLUMN outcome TO is_successful;

ALTER TABLE timeline_events
ALTER COLUMN is_successful TYPE BOOLEAN
    USING (
        CASE
            WHEN is_successful = 'SUCCESS' THEN TRUE
            WHEN is_successful = 'FAILURE' THEN FALSE
            ELSE NULL
        END
    );