CREATE INDEX idx_timeline_events_crn_pdus
    ON timeline_events (crn_pdus)
    WHERE crn_pdus IS NOT NULL AND crn_pdus <> CAST('null' AS jsonb) AND crn_pdus <> CAST('[]' AS jsonb);

CREATE INDEX idx_timeline_events_crn_probation_areas
    ON timeline_events (crn_probation_areas)
    WHERE crn_probation_areas <> '';
