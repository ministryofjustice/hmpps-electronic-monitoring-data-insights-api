CREATE TABLE status_alert_state (
    name VARCHAR(64) PRIMARY KEY,
    active BOOLEAN NOT NULL
);

INSERT INTO status_alert_state (name, active)
VALUES ('data_out_of_sync', FALSE);
