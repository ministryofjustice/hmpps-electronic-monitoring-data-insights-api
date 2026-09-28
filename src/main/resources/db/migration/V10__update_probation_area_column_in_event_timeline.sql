-- timeline_events: add column probation_area

ALTER TABLE timeline_events
add COLUMN crn_probation_areas varchar(255);