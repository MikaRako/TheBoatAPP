-- ============================================================
-- V3 — Audit log table
--
-- Design notes:
--
--   1. JSONB (metadata): flexible per-action context without schema
--      changes. Postgres can index inside it with GIN if needed later.
--
--   2. Sequence allocationSize=50 matches the entity — amortises the
--      sequence fetch cost across 50 inserts per batch.
--
--   3. NO UPDATE / DELETE privileges should be granted to the
--      application DB user on this table (enforced at the DB level,
--      not just application level). See the REVOKE statements below.
--
--   4. Indexes are chosen for the most common audit queries:
--        - "show me everything user X did"         → idx_audit_log_username
--        - "show me all BOAT_DELETE events"        → idx_audit_log_action
--        - "show me all events on boat #42"        → idx_audit_log_resource
--        - "show events in a time window"          → idx_audit_log_occurred_at
--        - "show all FAILURE events"               → idx_audit_log_outcome
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS audit_log_sequence
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE audit_log (
    id            BIGINT        PRIMARY KEY DEFAULT nextval('audit_log_sequence'),
    username      VARCHAR(255)  NOT NULL,
    action        VARCHAR(64)   NOT NULL,
    resource_type VARCHAR(64),
    resource_id   BIGINT,
    outcome       VARCHAR(16)   NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    error_message VARCHAR(1000),
    metadata      JSONB,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(512),
    occurred_at   TIMESTAMPTZ   NOT NULL
);

-- Indexes
CREATE INDEX idx_audit_log_username    ON audit_log (username);
CREATE INDEX idx_audit_log_action      ON audit_log (action);
CREATE INDEX idx_audit_log_resource    ON audit_log (resource_type, resource_id);
CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_outcome     ON audit_log (outcome);

-- Optional GIN index for querying inside the JSONB metadata column.
-- Enable this when you need queries like: WHERE metadata @> '{"name": "Sea Breeze"}'
-- CREATE INDEX idx_audit_log_metadata_gin ON audit_log USING GIN (metadata);

-- Prevent the application role from modifying or deleting audit records.
-- The INSERT-only restriction is the primary tamper-proofing mechanism at the DB layer.
REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM boatuser;
