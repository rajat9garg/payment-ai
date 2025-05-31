# Task 4: Payment Reconciliation API Implementation Plan - Part 1

## 1. Overview

This document provides the first part of a detailed implementation plan for the Payment Reconciliation API, which allows for reconciliation of pending payment transactions.

### API Endpoint
- **POST /v1/payment/reconcile**
- **Response**: List of reconciled payment transactions

### Implementation Approach
Following the OpenAPI-first approach and the established project rules, we'll implement this API in a structured manner, starting with the database setup for reconciliation records.

## 2. Database Implementation

### 2.1 Database Migration

Create a migration script for the reconciliation table:

```sql
-- src/main/resources/db/migration/V3__create_reconciliation_table.sql
CREATE TABLE reconciliation (
    id SERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    current_status VARCHAR(20) NOT NULL,
    reconciled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_sent BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_reconciliation_transaction_id ON reconciliation(transaction_id);
CREATE INDEX idx_reconciliation_reconciled_at ON reconciliation(reconciled_at);
```

### 2.2 Run Migration and Generate jOOQ Classes

Execute the Flyway migration and generate jOOQ classes:

```bash
./gradlew flywayMigrate
./gradlew generateJooq
```

## 3. Domain Model Implementation

### 3.1 Create Domain Models

Create the domain model for reconciliation records:

```kotlin
// src/main/kotlin/com/payment/models/domain/Reconciliation.kt
package com.payment.models.domain

import java.time.LocalDateTime

data class Reconciliation(
    val id: Long? = null,
    val transactionId: Long,
    val previousStatus: String,
    val currentStatus: String,
    val reconciledAt: LocalDateTime = LocalDateTime.now(),
    val notificationSent: Boolean = false
)
```

### 3.2 Create Database Mappers

Create mappers to convert between domain models and database records:

```kotlin
// src/main/kotlin/com/payment/mappers/ReconciliationMapper.kt
package com.payment.mappers

import com.payment.jooq.tables.records.ReconciliationRecord
import com.payment.models.domain.Reconciliation
import java.time.LocalDateTime

object ReconciliationMapper {
    
    fun toDomain(record: ReconciliationRecord): Reconciliation {
        return Reconciliation(
            id = record.id,
            transactionId = record.transactionId,
            previousStatus = record.previousStatus,
            currentStatus = record.currentStatus,
            reconciledAt = record.reconciledAt ?: LocalDateTime.now(),
            notificationSent = record.notificationSent ?: false
        )
    }
    
    fun toRecord(domain: Reconciliation, record: ReconciliationRecord): ReconciliationRecord {
        record.transactionId = domain.transactionId
        record.previousStatus = domain.previousStatus
        record.currentStatus = domain.currentStatus
        record.reconciledAt = domain.reconciledAt
        record.notificationSent = domain.notificationSent
        return record
    }
}
```

## 4. Repository Layer Implementation

### 4.1 Create Repository Interface

Define the repository interface for reconciliation records:

```kotlin
// src/main/kotlin/com/payment/repositories/ReconciliationRepository.kt
package com.payment.repositories

import com.payment.models.domain.Reconciliation
import java.time.LocalDateTime

interface ReconciliationRepository {
    fun findById(id: Long): Reconciliation?
    fun findByTransactionId(transactionId: Long): List<Reconciliation>
    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Reconciliation>
    fun findByNotificationSent(notificationSent: Boolean): List<Reconciliation>
    fun save(reconciliation: Reconciliation): Reconciliation
    fun update(reconciliation: Reconciliation): Reconciliation
}
```

### 4.2 Implement Repository Using jOOQ

Implement the repository interface using jOOQ:

```kotlin
// src/main/kotlin/com/payment/repositories/impl/ReconciliationRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.RECONCILIATION
import com.payment.mappers.ReconciliationMapper
import com.payment.models.domain.Reconciliation
import com.payment.repositories.ReconciliationRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ReconciliationRepositoryImpl(private val dsl: DSLContext) : ReconciliationRepository {
    
    override fun findById(id: Long): Reconciliation? {
        return dsl.selectFrom(RECONCILIATION)
            .where(RECONCILIATION.ID.eq(id))
            .fetchOne()
            ?.let { ReconciliationMapper.toDomain(it) }
    }
    
    override fun findByTransactionId(transactionId: Long): List<Reconciliation> {
        return dsl.selectFrom(RECONCILIATION)
            .where(RECONCILIATION.TRANSACTION_ID.eq(transactionId))
            .fetch()
            .map { ReconciliationMapper.toDomain(it) }
    }
    
    override fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Reconciliation> {
        return dsl.selectFrom(RECONCILIATION)
            .where(RECONCILIATION.RECONCILED_AT.between(startDate, endDate))
            .fetch()
            .map { ReconciliationMapper.toDomain(it) }
    }
    
    override fun findByNotificationSent(notificationSent: Boolean): List<Reconciliation> {
        return dsl.selectFrom(RECONCILIATION)
            .where(RECONCILIATION.NOTIFICATION_SENT.eq(notificationSent))
            .fetch()
            .map { ReconciliationMapper.toDomain(it) }
    }
    
    override fun save(reconciliation: Reconciliation): Reconciliation {
        val record = dsl.newRecord(RECONCILIATION)
        ReconciliationMapper.toRecord(reconciliation, record)
        record.store()
        return ReconciliationMapper.toDomain(record)
    }
    
    override fun update(reconciliation: Reconciliation): Reconciliation {
        val record = dsl.selectFrom(RECONCILIATION)
            .where(RECONCILIATION.ID.eq(reconciliation.id))
            .fetchOne() ?: throw IllegalArgumentException("Reconciliation not found with id: ${reconciliation.id}")
            
        ReconciliationMapper.toRecord(reconciliation, record)
        record.store()
        return ReconciliationMapper.toDomain(record)
    }
}
```
