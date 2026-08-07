-- timeline_events: drop the success flag, add the result count, and rename
-- activity_code to event_type.

ALTER TABLE timeline_events
DROP COLUMN is_successful,
  ADD COLUMN results integer;

ALTER TABLE timeline_events
    RENAME COLUMN activity_code TO event_type;