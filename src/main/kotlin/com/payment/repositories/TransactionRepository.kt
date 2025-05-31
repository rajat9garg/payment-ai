package com.payment.repositories

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus

/**
 * Repository interface for transactions
 */
interface TransactionRepository {
    /**
     * Find transaction by ID
     * @param id Transaction ID
     * @return Transaction or null if not found
     */
    fun findById(id: Long): Transaction?
    
    /**
     * Find transaction by idempotency key
     * @param idempotencyKey Idempotency key
     * @return Transaction or null if not found
     */
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?
    
    /**
     * Find transaction by idempotency key with pessimistic lock
     * @param idempotencyKey Idempotency key
     * @return Transaction or null if not found
     */
    fun findByIdempotencyKeyWithLock(idempotencyKey: String): Transaction?
    
    /**
     * Save a transaction
     * @param transaction Transaction to save
     * @return Saved transaction
     */
    fun save(transaction: Transaction): Transaction
    
    /**
     * Update transaction status
     * @param id Transaction ID
     * @param status New status
     * @param vendorTransactionId Vendor transaction ID (optional)
     * @return Updated transaction or null if not found
     */
    fun updateStatus(id: Long, status: TransactionStatus, vendorTransactionId: String? = null): Transaction?
    
    /**
     * Find transactions by user ID
     * @param userId User ID
     * @return List of transactions for the user
     */
    fun findByUserId(userId: String): List<Transaction>
    
    /**
     * Find transactions by status
     * @param status Transaction status
     * @return List of transactions with the specified status
     */
    fun findByStatus(status: TransactionStatus): List<Transaction>
}
