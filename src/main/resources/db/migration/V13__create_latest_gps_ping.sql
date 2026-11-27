CREATE TABLE latest_gps_ping (
    crn TEXT PRIMARY KEY,
    person_id TEXT,
    gps_date TIMESTAMP WITH TIME ZONE NOT NULL
);
