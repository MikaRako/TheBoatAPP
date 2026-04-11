-- Create keycloak schema (Keycloak needs its own schema)
CREATE SCHEMA IF NOT EXISTS keycloak;
GRANT ALL PRIVILEGES ON SCHEMA keycloak TO boatuser;
