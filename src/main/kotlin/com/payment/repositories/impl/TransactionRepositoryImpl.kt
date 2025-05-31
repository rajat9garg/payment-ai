package com.payment.repositories.impl

import com.payment.infrastructure.jooq.tables.Transactions
import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.repositories.TransactionRepository
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Implementation of TransactionRepository using jOOQ
 */
@Repository
class TransactionRepositoryImpl(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper
) : TransactionRepository {

    override fun findById(id: Long): Transaction? {
        val transactions = Transactions.TRANSACTIONS
        
        val record = dsl.selectFrom(transactions)
            .where(transactions.ID.eq(id))
            .fetchOne()
        
        return record?.let { mapToTransaction(it) }
    }
    
    override fun findByIdempotencyKey(idempotencyKey: String): Transaction? {
        val transactions = Transactions.TRANSACTIONS
        
        val record = dsl.selectFrom(transactions)
            .where(transactions.IDEMPOTENCY_KEY.eq(idempotencyKey))
            .fetchOne()
        
        return record?.let { mapToTransaction(it) }
    }
    
    override fun findByIdempotencyKeyWithLock(idempotencyKey: String): Transaction? {
        val transactions = Transactions.TRANSACTIONS
        
        val record = dsl.selectFrom(transactions)
            .where(transactions.IDEMPOTENCY_KEY.eq(idempotencyKey))
            .forUpdate()
            .fetchOne()
        
        return record?.let { mapToTransaction(it) }
    }
    
    override fun save(transaction: Transaction): Transaction {
        val transactions = Transactions.TRANSACTIONS
        
        // Define the VERSION field manually since it's not in the generated jOOQ classes yet
        val VERSION = DSL.field("version", Long::class.java)
        
        // If transaction is new (no ID), insert it
        if (transaction.id == null) {
            try {
                val record = dsl.insertInto(transactions)
                    .set(transactions.IDEMPOTENCY_KEY, transaction.idempotencyKey)
                    .set(transactions.STATUS, transaction.status.name)
                    .set(transactions.USER_ID, transaction.userId)
                    .set(transactions.AMOUNT, transaction.amount)
                    .set(transactions.CURRENCY, transaction.currency)
                    .set(transactions.PAYMENT_MODE, transaction.paymentMode)
                    .set(transactions.PAYMENT_TYPE, transaction.paymentType)
                    .set(transactions.PAYMENT_PROVIDER, transaction.paymentProvider)
                    .set(transactions.VENDOR_TRANSACTION_ID, transaction.vendorTransactionId)
                    .set(transactions.METADATA, JSONB.valueOf(mapToJsonString(transaction.metadata)))
                    .set(transactions.CREATED_AT, transaction.createdAt)
                    .set(transactions.UPDATED_AT, transaction.updatedAt)
                    .set(VERSION, transaction.version)
                    .returning()
                    .fetchOne()
                
                return mapToTransaction(record!!)
            } catch (e: Exception) {
                // Handle unique constraint violation for idempotency key
                if (e is DataIntegrityViolationException || e.cause is DataIntegrityViolationException) {
                    // If there's a conflict, try to find the existing transaction
                    val existingTransaction = findByIdempotencyKey(transaction.idempotencyKey)
                    if (existingTransaction != null) {
                        return existingTransaction
                    }
                }
                throw e
            }
        } 
        // Otherwise, update existing record with optimistic locking
        else {
            val updateCount = dsl.update(transactions)
                .set(transactions.STATUS, transaction.status.name)
                .set(transactions.VENDOR_TRANSACTION_ID, transaction.vendorTransactionId)
                .set(transactions.METADATA, JSONB.valueOf(mapToJsonString(transaction.metadata)))
                .set(transactions.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .set(VERSION, transaction.version + 1)
                .where(
                    transactions.ID.eq(transaction.id)
                    .and(VERSION.eq(transaction.version))
                )
                .execute()
            
            if (updateCount == 0) {
                throw OptimisticLockingFailureException("Concurrent modification detected for transaction ${transaction.id}")
            }
            
            return findById(transaction.id)!!.copy(version = transaction.version + 1)
        }
    }
    
    override fun updateStatus(id: Long, status: TransactionStatus, vendorTransactionId: String?): Transaction? {
        val transactions = Transactions.TRANSACTIONS
        
        // Define the VERSION field manually since it's not in the generated jOOQ classes yet
        val VERSION = DSL.field("version", Long::class.java)
        
        val updateStep = dsl.update(transactions)
            .set(transactions.STATUS, status.name)
            .set(transactions.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
            .set(VERSION, DSL.field("version + 1", Long::class.java))
        
        // Add vendor transaction ID if provided
        if (vendorTransactionId != null) {
            updateStep.set(transactions.VENDOR_TRANSACTION_ID, vendorTransactionId)
        }
        
        val record = updateStep
            .where(transactions.ID.eq(id))
            .returning()
            .fetchOne()
        
        return record?.let { mapToTransaction(it) }
    }
    
    override fun findByUserId(userId: String): List<Transaction> {
        val transactions = Transactions.TRANSACTIONS
        
        return dsl.selectFrom(transactions)
            .where(transactions.USER_ID.eq(userId))
            .orderBy(transactions.CREATED_AT.desc())
            .fetch()
            .map { mapToTransaction(it) }
    }
    
    override fun findByStatus(status: TransactionStatus): List<Transaction> {
        val transactions = Transactions.TRANSACTIONS
        
        return dsl.selectFrom(transactions)
            .where(transactions.STATUS.eq(status.name))
            .orderBy(transactions.CREATED_AT.desc())
            .fetch()
            .map { mapToTransaction(it) }
    }
    
    /**
     * Map jOOQ record to Transaction domain model
     */
    private fun mapToTransaction(record: com.payment.infrastructure.jooq.tables.records.TransactionsRecord): Transaction {
        // Get the version value directly from the record using field name
        val version = record.get("version", Long::class.java) ?: 0L
        
        return Transaction(
            id = record.id,
            idempotencyKey = record.idempotencyKey,
            status = TransactionStatus.fromString(record.status),
            userId = record.userId,
            amount = record.amount,
            currency = record.currency,
            paymentMode = record.paymentMode,
            paymentType = record.paymentType,
            paymentProvider = record.paymentProvider,
            vendorTransactionId = record.vendorTransactionId,
            metadata = mapFromJsonString(record.metadata?.data() ?: "{}"),
            createdAt = record.createdAt ?: OffsetDateTime.now(ZoneOffset.UTC),
            updatedAt = record.updatedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
            version = version
        )
    }
    
    /**
     * Convert map to JSON string
     */
    private fun mapToJsonString(map: Map<String, Any>): String {
        return objectMapper.writeValueAsString(map)
    }
    
    /**
     * Convert JSON string to map
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapFromJsonString(json: String): Map<String, Any> {
        return objectMapper.readValue(json)
    }
}
