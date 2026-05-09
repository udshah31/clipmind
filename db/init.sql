-- Runs once on first container boot. Idempotent.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Sanity log so you can confirm in `make logs` that this ran.
DO $$ BEGIN RAISE NOTICE 'clipmind init.sql complete'; END $$;
