-- Initial schema: boats table and its sequence.
-- This script represents the state of the database before Flyway was introduced.
-- On existing deployments, Flyway will use this as the baseline (it won't run this script).
-- On fresh deployments, this script runs first to build the schema from scratch.

CREATE SEQUENCE IF NOT EXISTS boat_sequence
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS boats (
    id          BIGINT                      NOT NULL DEFAULT nextval('boat_sequence'),
    name        VARCHAR(255)                NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE   NOT NULL,
    PRIMARY KEY (id)
);
