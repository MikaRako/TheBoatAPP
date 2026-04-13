-- Add status and type columns to the boats table.
-- DEFAULT values ensure existing rows get a valid enum value when the columns are created.

ALTER TABLE boats
    ADD COLUMN IF NOT EXISTS status VARCHAR(255) NOT NULL DEFAULT 'IN_PORT';

ALTER TABLE boats
    ADD COLUMN IF NOT EXISTS type VARCHAR(255) NOT NULL DEFAULT 'YACHT';
