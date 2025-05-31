# Task 2: Payment Initiation API Implementation Plan - Part 2

## 4. Domain Model Implementation

### 4.1 Create Domain Models

Create the domain model for transactions:

```kotlin
// src/main/kotlin/com/payment/models/domain/Transaction.kt
package com.payment.models.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: Long? = null,
    val idempotencyKey: String,
    var status: String,
    val userId: String,
    val amount: BigDecimal,
    val paymentMode: String,
    var vendorTransactionId: String? = null,
    val paymentProvider: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
```

Create a payment request domain model:

```kotlin
// src/main/kotlin/com/payment/models/domain/PaymentRequest.kt
package com.payment.models.domain

import java.math.BigDecimal

data class PaymentRequest(
    val amount: BigDecimal,
    val paymentMode: String,
    val paymentType: String,
    val currency: String = "INR",
    val metadata: Map<String, Any> = emptyMap()
)
```

Create a payment response domain model:

```kotlin
// src/main/kotlin/com/payment/models/domain/PaymentResponse.kt
package com.payment.models.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    val paymentId: String,
    val status: String,
    val amount: BigDecimal,
    val paymentMode: String,
    val paymentType: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

### 4.2 Create Database Mappers

Create mappers to convert between domain models and database records:

```kotlin
// src/main/kotlin/com/payment/mappers/TransactionMapper.kt
package com.payment.mappers

import com.payment.jooq.tables.records.TransactionsRecord
import com.payment.models.domain.Transaction
import java.time.LocalDateTime

object TransactionMapper {
    
    fun toDomain(record: TransactionsRecord): Transaction {
        return Transaction(
            id = record.id,
            idempotencyKey = record.idempotencyKey,
            status = record.status,
            userId = record.userId,
            amount = record.amount,
            paymentMode = record.paymentMode,
            vendorTransactionId = record.vendorTransactionId,
            paymentProvider = record.paymentProvider,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
    
    fun toRecord(domain: Transaction, record: TransactionsRecord): TransactionsRecord {
        record.idempotencyKey = domain.idempotencyKey
        record.status = domain.status
        record.userId = domain.userId
        record.amount = domain.amount
        record.paymentMode = domain.paymentMode
        record.vendorTransactionId = domain.vendorTransactionId
        record.paymentProvider = domain.paymentProvider
        // Let database handle timestamps if null
        domain.createdAt?.let { record.createdAt = it }
        domain.updatedAt?.let { record.updatedAt = it }
        return record
    }
}
```

## 5. Repository Layer Implementation

### 5.1 Create Repository Interface

Define the repository interface for transactions:

```kotlin
// src/main/kotlin/com/payment/repositories/TransactionRepository.kt
package com.payment.repositories

import com.payment.models.domain.Transaction
import java.util.Optional

interface TransactionRepository {
    fun findById(id: Long): Transaction?
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?
    fun findByUserId(userId: String): List<Transaction>
    fun findByStatus(status: String): List<Transaction>
    fun save(transaction: Transaction): Transaction
    fun update(transaction: Transaction): Transaction
}
```

### 5.2 Implement Repository Using jOOQ

Implement the repository interface using jOOQ:

```kotlin
// src/main/kotlin/com/payment/repositories/impl/TransactionRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.TRANSACTIONS
import com.payment.mappers.TransactionMapper
import com.payment.models.domain.Transaction
import com.payment.repositories.TransactionRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TransactionRepositoryImpl(private val dsl: DSLContext) : TransactionRepository {
    
    override fun findById(id: Long): Transaction? {
        return dsl.selectFrom(TRANSACTIONS)
            .where(TRANSACTIONS.ID.eq(id))
            .fetchOne()
            ?.let { TransactionMapper.toDomain(it) }
    }
    
    override fun findByIdempotencyKey(idempotencyKey: String): Transaction? {
        return dsl.selectFrom(TRANSACTIONS)
            .where(TRANSACTIONS.IDEMPOTENCY_KEY.eq(idempotencyKey))
            .fetchOne()
            ?.let { TransactionMapper.toDomain(it) }
    }
    
    override fun findByUserId(userId: String): List<Transaction> {
        return dsl.selectFrom(TRANSACTIONS)
            .where(TRANSACTIONS.USER_ID.eq(userId))
            .fetch()
            .map { TransactionMapper.toDomain(it) }
    }
    
    override fun findByStatus(status: String): List<Transaction> {
        return dsl.selectFrom(TRANSACTIONS)
            .where(TRANSACTIONS.STATUS.eq(status))
            .fetch()
            .map { TransactionMapper.toDomain(it) }
    }
    
    override fun save(transaction: Transaction): Transaction {
        val now = LocalDateTime.now()
        
        val record = dsl.newRecord(TRANSACTIONS)
        TransactionMapper.toRecord(transaction.copy(createdAt = now, updatedAt = now), record)
        record.store()
        return TransactionMapper.toDomain(record)
    }
    
    override fun update(transaction: Transaction): Transaction {
        val now = LocalDateTime.now()
        
        val record = dsl.selectFrom(TRANSACTIONS)
            .where(TRANSACTIONS.ID.eq(transaction.id))
            .fetchOne() ?: throw IllegalArgumentException("Transaction not found with id: ${transaction.id}")
            
        TransactionMapper.toRecord(transaction.copy(updatedAt = now), record)
        record.store()
        return TransactionMapper.toDomain(record)
    }
}
```

### 5.3 Implement Redis Service for Idempotency Key Generation

Create a service for generating unique payment IDs using Redis:

```kotlin
// src/main/kotlin/com/payment/services/IdempotencyKeyService.kt
package com.payment.services

interface IdempotencyKeyService {
    /**
     * Generate a unique idempotency key for payment transactions
     * 
     * @return A unique idempotency key
     */
    fun generateIdempotencyKey(): String
}
```

Implement the idempotency key service:

```kotlin
// src/main/kotlin/com/payment/services/impl/IdempotencyKeyServiceImpl.kt
package com.payment.services.impl

import com.payment.repositories.TransactionRepository
import com.payment.services.IdempotencyKeyService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class IdempotencyKeyServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val transactionRepository: TransactionRepository
) : IdempotencyKeyService {

    companion object {
        private const val PAYMENT_COUNTER_KEY = "payment:counter"
        private const val MAX_RETRIES = 5
    }

    /**
     * Generate a unique idempotency key for payment transactions
     * Algorithm:
     * 1. Use Redis INCR to generate a sequential number
     * 2. Format as "PAY{number}"
     * 3. Check if key exists in database
     * 4. If exists, repeat from step 1
     * 5. If not exists, use as payment ID
     * 
     * @return A unique idempotency key
     */
    override fun generateIdempotencyKey(): String {
        var retries = 0
        
        while (retries < MAX_RETRIES) {
            // Increment counter in Redis
            val counter = redisTemplate.opsForValue().increment(PAYMENT_COUNTER_KEY) ?: 1L
            
            // Format as PAY{number}
            val idempotencyKey = "PAY$counter"
            
            // Check if key exists in database
            if (transactionRepository.findByIdempotencyKey(idempotencyKey) == null) {
                return idempotencyKey
            }
            
            retries++
        }
        
        throw IllegalStateException("Failed to generate unique idempotency key after $MAX_RETRIES attempts")
    }
}
```
