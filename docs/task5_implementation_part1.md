# Task 5: Payment Modes API Implementation Plan - Part 1

## 1. Overview

This document provides the first part of a detailed implementation plan for the Payment Modes API, which allows clients to retrieve available payment modes based on user and product type.

### API Endpoint
- **GET /v1/payment/modes**
- **Headers**: userId
- **Query Parameters**: productType
- **Response**: Available payment modes

### Implementation Approach
Following the OpenAPI-first approach and the established project rules, we'll implement this API in a structured manner, starting with the database setup for payment modes.

## 2. Database Implementation

### 2.1 Database Migration

Create a migration script for the payment modes and product payment modes tables:

```sql
-- src/main/resources/db/migration/V4__create_payment_modes_tables.sql

-- Table for payment modes
CREATE TABLE payment_modes (
    id SERIAL PRIMARY KEY,
    mode_code VARCHAR(50) NOT NULL UNIQUE,
    mode_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for payment types (e.g., VISA, MASTERCARD for CREDIT_CARD mode)
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

-- Table for product payment mode mappings
CREATE TABLE product_payment_modes (
    id SERIAL PRIMARY KEY,
    product_type VARCHAR(50) NOT NULL,
    mode_id INT NOT NULL,
    type_id INT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mode_id) REFERENCES payment_modes(id),
    FOREIGN KEY (type_id) REFERENCES payment_types(id),
    UNIQUE (product_type, mode_id, type_id)
);

-- Table for user payment mode preferences
CREATE TABLE user_payment_preferences (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    mode_id INT NOT NULL,
    type_id INT,
    is_preferred BOOLEAN DEFAULT FALSE,
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mode_id) REFERENCES payment_modes(id),
    FOREIGN KEY (type_id) REFERENCES payment_types(id),
    UNIQUE (user_id, mode_id, type_id)
);

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

-- Insert sample product payment modes
INSERT INTO product_payment_modes (product_type, mode_id, type_id) VALUES
('MOBILE', (SELECT id FROM payment_modes WHERE mode_code = 'UPI'), (SELECT id FROM payment_types WHERE type_code = 'GOOGLE_PAY')),
('MOBILE', (SELECT id FROM payment_modes WHERE mode_code = 'UPI'), (SELECT id FROM payment_types WHERE type_code = 'CRED')),
('MOBILE', (SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'), (SELECT id FROM payment_types WHERE type_code = 'VISA' AND mode_id = (SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'))),
('ELECTRONICS', (SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'), (SELECT id FROM payment_types WHERE type_code = 'VISA' AND mode_id = (SELECT id FROM payment_modes WHERE mode_code = 'CREDIT_CARD'))),
('ELECTRONICS', (SELECT id FROM payment_modes WHERE mode_code = 'DEBIT_CARD'), (SELECT id FROM payment_types WHERE type_code = 'VISA' AND mode_id = (SELECT id FROM payment_modes WHERE mode_code = 'DEBIT_CARD'))),
('FURNITURE', (SELECT id FROM payment_modes WHERE mode_code = 'NET_BANKING'), (SELECT id FROM payment_types WHERE type_code = 'HDFC')),
('FURNITURE', (SELECT id FROM payment_modes WHERE mode_code = 'NET_BANKING'), (SELECT id FROM payment_types WHERE type_code = 'ICICI'));

-- Create indexes for better query performance
CREATE INDEX idx_payment_types_mode_id ON payment_types(mode_id);
CREATE INDEX idx_product_payment_modes_product_type ON product_payment_modes(product_type);
CREATE INDEX idx_product_payment_modes_mode_id ON product_payment_modes(mode_id);
CREATE INDEX idx_user_payment_preferences_user_id ON user_payment_preferences(user_id);
```

### 2.2 Run Migration and Generate jOOQ Classes

Execute the Flyway migration and generate jOOQ classes:

```bash
./gradlew flywayMigrate
./gradlew generateJooq
```

## 3. Domain Model Implementation

### 3.1 Create Domain Models

Create the domain models for payment modes:

```kotlin
// src/main/kotlin/com/payment/models/domain/PaymentMode.kt
package com.payment.models.domain

import java.time.LocalDateTime

data class PaymentMode(
    val id: Long? = null,
    val modeCode: String,
    val modeName: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val paymentTypes: List<PaymentType> = emptyList()
)
```

```kotlin
// src/main/kotlin/com/payment/models/domain/PaymentType.kt
package com.payment.models.domain

import java.time.LocalDateTime

data class PaymentType(
    val id: Long? = null,
    val modeId: Long,
    val typeCode: String,
    val typeName: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/payment/models/domain/ProductPaymentMode.kt
package com.payment.models.domain

import java.time.LocalDateTime

data class ProductPaymentMode(
    val id: Long? = null,
    val productType: String,
    val modeId: Long,
    val typeId: Long? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/payment/models/domain/UserPaymentPreference.kt
package com.payment.models.domain

import java.time.LocalDateTime

data class UserPaymentPreference(
    val id: Long? = null,
    val userId: String,
    val modeId: Long,
    val typeId: Long? = null,
    val isPreferred: Boolean = false,
    val isBlocked: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 3.2 Create Database Mappers

Create mappers to convert between domain models and database records:

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentModeMapper.kt
package com.payment.mappers

import com.payment.jooq.tables.records.PaymentModesRecord
import com.payment.models.domain.PaymentMode
import java.time.LocalDateTime

object PaymentModeMapper {
    
    fun toDomain(record: PaymentModesRecord): PaymentMode {
        return PaymentMode(
            id = record.id,
            modeCode = record.modeCode,
            modeName = record.modeName,
            description = record.description,
            isActive = record.isActive ?: true,
            createdAt = record.createdAt ?: LocalDateTime.now(),
            updatedAt = record.updatedAt ?: LocalDateTime.now()
        )
    }
    
    fun toRecord(domain: PaymentMode, record: PaymentModesRecord): PaymentModesRecord {
        record.modeCode = domain.modeCode
        record.modeName = domain.modeName
        record.description = domain.description
        record.isActive = domain.isActive
        record.createdAt = domain.createdAt
        record.updatedAt = domain.updatedAt
        return record
    }
}
```

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentTypeMapper.kt
package com.payment.mappers

import com.payment.jooq.tables.records.PaymentTypesRecord
import com.payment.models.domain.PaymentType
import java.time.LocalDateTime

object PaymentTypeMapper {
    
    fun toDomain(record: PaymentTypesRecord): PaymentType {
        return PaymentType(
            id = record.id,
            modeId = record.modeId,
            typeCode = record.typeCode,
            typeName = record.typeName,
            description = record.description,
            isActive = record.isActive ?: true,
            createdAt = record.createdAt ?: LocalDateTime.now(),
            updatedAt = record.updatedAt ?: LocalDateTime.now()
        )
    }
    
    fun toRecord(domain: PaymentType, record: PaymentTypesRecord): PaymentTypesRecord {
        record.modeId = domain.modeId
        record.typeCode = domain.typeCode
        record.typeName = domain.typeName
        record.description = domain.description
        record.isActive = domain.isActive
        record.createdAt = domain.createdAt
        record.updatedAt = domain.updatedAt
        return record
    }
}
```
