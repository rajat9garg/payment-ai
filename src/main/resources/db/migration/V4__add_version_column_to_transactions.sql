-- Add version column to transactions table for optimistic locking
ALTER TABLE transactions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment for the version column
COMMENT ON COLUMN transactions.version IS 'Version field for optimistic locking';

-- Create a function to update the version column on update
CREATE OR REPLACE FUNCTION update_version_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger to update the version column on update
DROP TRIGGER IF EXISTS update_transactions_version ON transactions;
CREATE TRIGGER update_transactions_version
BEFORE UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_version_column();
