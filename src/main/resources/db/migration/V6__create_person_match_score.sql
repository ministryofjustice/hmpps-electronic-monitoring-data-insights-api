CREATE TABLE person_match_score
(
    id UUID PRIMARY KEY,
    crn VARCHAR(20) NOT NULL,
    person_id VARCHAR(100) NOT NULL,
    exact_name_match BOOLEAN NOT NULL,
    exact_postcode_match BOOLEAN NOT NULL,
    exact_dob_match BOOLEAN NOT NULL,
    name_score DOUBLE PRECISION NOT NULL,
    postcode_score DOUBLE PRECISION NOT NULL,
    dob_score DOUBLE PRECISION NOT NULL,
    overall_match_score DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_person_match_score_crn_person_id
    ON person_match_score (crn, person_id);
