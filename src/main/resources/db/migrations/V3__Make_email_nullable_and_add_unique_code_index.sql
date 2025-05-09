ALTER TABLE invitations
    ALTER COLUMN email DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND tablename = 'invitations'
          AND indexname = 'uq_invitations_code'
    ) THEN
        CREATE UNIQUE INDEX uq_invitations_code
            ON invitations(code);
    END IF;
END
$$;
