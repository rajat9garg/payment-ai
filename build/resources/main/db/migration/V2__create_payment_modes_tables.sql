-- Create payment_modes table
CREATE TABLE payment_modes (
    id SERIAL PRIMARY KEY,
    mode_code VARCHAR(50) NOT NULL UNIQUE,
    mode_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create payment_types table
CREATE TABLE payment_types (
    id SERIAL PRIMARY KEY,
    mode_id INT NOT NULL,
    type_code VARCHAR(50) NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mode_id) REFERENCES payment_modes(id),
    UNIQUE (mode_id, type_code)
);

-- Create indexes for better query performance
CREATE INDEX idx_payment_types_mode_id ON payment_types(mode_id);

-- Insert sample payment modes
INSERT INTO payment_modes (mode_code, mode_name, description) VALUES
('UPI', 'Unified Payment Interface', 'Direct bank transfer using UPI'),
('CREDIT_CARD', 'Credit Card', 'Pay using credit card'),
('DEBIT_CARD', 'Debit Card', 'Pay using debit card'),
('NET_BANKING', 'Net Banking', 'Pay using net banking');

-- Insert sample payment types
INSERT INTO payment_types (mode_id, type_code, type_name, description) VALUES
((SELECT id FROM payment_modes WHERE mode_code = 'UPI'), 'GOOGLE_PAY', 'Google Pay', 'Pay using Google Pay UPI'),
((SELECT id FROM payment_modes WHERE mode_code = 'UPI'), 'CRED', 'CRED', 'Pay using CRED UPI'),
((SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'), 'VISA', 'Visa', 'Pay using Visa credit card'),
((SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'), 'MASTERCARD', 'Mastercard', 'Pay using Mastercard credit card'),
((SELECT id FROM payment_modes WHERE mode_code = 'DEBIT_CARD'), 'VISA', 'Visa', 'Pay using Visa debit card'),
((SELECT id FROM payment_modes WHERE mode_code = 'DEBIT_CARD'), 'MASTERCARD', 'Mastercard', 'Pay using Mastercard debit card'),
((SELECT id FROM payment_modes WHERE mode_code = 'NET_BANKING'), 'HDFC', 'HDFC Bank', 'Pay using HDFC net banking'),
((SELECT id FROM payment_modes WHERE mode_code = 'NET_BANKING'), 'ICICI', 'ICICI Bank', 'Pay using ICICI net banking');

-- Create triggers for updated_at columns
CREATE TRIGGER update_payment_modes_updated_at
BEFORE UPDATE ON payment_modes
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_types_updated_at
BEFORE UPDATE ON payment_types
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
