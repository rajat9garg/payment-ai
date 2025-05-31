-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_mode VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    vendor_transaction_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_mode FOREIGN KEY (payment_mode) REFERENCES payment_modes(mode_code)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_idempotency_key ON transactions(idempotency_key);

-- Create a function to update the updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger to update the updated_at column on update
DROP TRIGGER IF EXISTS update_transactions_updated_at ON transactions;
CREATE TRIGGER update_transactions_updated_at
BEFORE UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Add comments
COMMENT ON TABLE transactions IS 'Stores payment transaction details';
COMMENT ON COLUMN transactions.idempotency_key IS 'Unique idempotency key for the transaction';
COMMENT ON COLUMN transactions.status IS 'Transaction status (PENDING, SUCCESS, FAILED, CANCELLED)';
COMMENT ON COLUMN transactions.user_id IS 'ID of the user who initiated the transaction';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount';
COMMENT ON COLUMN transactions.currency IS 'Transaction currency (default: INR)';
COMMENT ON COLUMN transactions.payment_mode IS 'Payment mode (e.g., UPI, CREDIT_CARD, DEBIT_CARD)';
COMMENT ON COLUMN transactions.payment_type IS 'Payment type (e.g., GOOGLE_PAY, VISA, MASTERCARD)';
COMMENT ON COLUMN transactions.payment_provider IS 'Payment provider name';
COMMENT ON COLUMN transactions.vendor_transaction_id IS 'Transaction ID from the payment provider';
COMMENT ON COLUMN transactions.metadata IS 'Additional metadata related to the transaction';
COMMENT ON COLUMN transactions.created_at IS 'Timestamp when the transaction was created';
COMMENT ON COLUMN transactions.updated_at IS 'Timestamp when the transaction was last updated';
