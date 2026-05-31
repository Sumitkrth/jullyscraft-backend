-- Create extensions only
\c jullyscraft_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- for trigram search
CREATE EXTENSION IF NOT EXISTS "unaccent";     -- for accent-insensitive search

-- NOTE: Role seeding moved to application startup (Liquibase/Flyway or @PostConstruct)
-- This ensures the roles table exists before attempting inserts
