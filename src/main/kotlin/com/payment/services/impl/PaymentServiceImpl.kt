package com.payment.services.impl

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.providers.PaymentProviderFactory
import com.payment.repositories.TransactionRepository
import com.payment.services.IdempotencyKeyService
import com.payment.services.PaymentService
import com.payment.services.RedisLockService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Implementation of PaymentService
 */
@Service
class PaymentServiceImpl(
    private val idempotencyKeyService: IdempotencyKeyService,
    private val transactionRepository: TransactionRepository,
    private val paymentProviderFactory: PaymentProviderFactory,
    private val redisLockService: RedisLockService
) : PaymentService {

    private val logger = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    @Transactional
    override fun initiatePayment(userId: String, request: PaymentRequest, idempotencyKey: String): PaymentResponse {
        logger.info("Processing payment with idempotency key: $idempotencyKey for user: $userId")
        
        // Check if a transaction with this idempotency key already exists
        val existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
        if (existingTransaction != null) {
            logger.info("Found existing transaction with idempotency key: $idempotencyKey, returning cached response")
            
            // Return existing transaction details
            return PaymentResponse(
                paymentId = existingTransaction.idempotencyKey,
                status = existingTransaction.status,
                amount = existingTransaction.amount,
                currency = existingTransaction.currency,
                paymentMode = existingTransaction.paymentMode,
                paymentType = existingTransaction.paymentType,
                vendorTransactionId = existingTransaction.vendorTransactionId,
                metadata = existingTransaction.metadata
            )
        }
        
        // Try to acquire a distributed lock for this idempotency key
        val lockAcquired = redisLockService.acquireLock(idempotencyKey)
        if (!lockAcquired) {
            logger.warn("Could not acquire lock for idempotency key: $idempotencyKey, another request is in progress")
            throw ConcurrentModificationException("Another request with the same idempotency key is being processed")
        }
        
        try {
            // Double-check if transaction was created while we were waiting for the lock
            val transactionAfterLock = transactionRepository.findByIdempotencyKey(idempotencyKey)
            if (transactionAfterLock != null) {
                logger.info("Transaction was created while waiting for lock, idempotency key: $idempotencyKey")
                return PaymentResponse(
                    paymentId = transactionAfterLock.idempotencyKey,
                    status = transactionAfterLock.status,
                    amount = transactionAfterLock.amount,
                    currency = transactionAfterLock.currency,
                    paymentMode = transactionAfterLock.paymentMode,
                    paymentType = transactionAfterLock.paymentType,
                    vendorTransactionId = transactionAfterLock.vendorTransactionId,
                    metadata = transactionAfterLock.metadata
                )
            }
            
            // Check if payment mode and type are supported
            val paymentProvider = paymentProviderFactory.getProvider(request.paymentMode, request.paymentType)
                ?: throw IllegalArgumentException("Unsupported payment mode or type: ${request.paymentMode}/${request.paymentType}")
            
            // Process payment with the provider
            val paymentResponse = paymentProvider.processPayment(request, idempotencyKey, userId)
            
            try {
                // Create and save transaction record
                val transaction = Transaction(
                    idempotencyKey = idempotencyKey,
                    status = paymentResponse.status,
                    userId = userId,
                    amount = request.amount,
                    currency = request.currency,
                    paymentMode = request.paymentMode,
                    paymentType = request.paymentType,
                    paymentProvider = paymentProvider.getName(),
                    vendorTransactionId = paymentResponse.vendorTransactionId,
                    metadata = request.metadata
                )
                
                val savedTransaction = transactionRepository.save(transaction)
                logger.info("Saved transaction with ID: ${savedTransaction.id} and idempotency key: $idempotencyKey")
                
                return paymentResponse
            } catch (e: DataIntegrityViolationException) {
                // Handle the case where another thread/instance created the transaction
                // This is a belt-and-suspenders approach in addition to the Redis lock
                logger.warn("Concurrent insert detected for idempotency key: $idempotencyKey", e)
                
                // Fetch the transaction that was created by the other thread/instance
                val concurrentTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                if (concurrentTransaction != null) {
                    return PaymentResponse(
                        paymentId = concurrentTransaction.idempotencyKey,
                        status = concurrentTransaction.status,
                        amount = concurrentTransaction.amount,
                        currency = concurrentTransaction.currency,
                        paymentMode = concurrentTransaction.paymentMode,
                        paymentType = concurrentTransaction.paymentType,
                        vendorTransactionId = concurrentTransaction.vendorTransactionId,
                        metadata = concurrentTransaction.metadata
                    )
                }
                
                // If we can't find the transaction, rethrow the exception
                throw e
            }
        } finally {
            // Always release the lock
            redisLockService.releaseLock(idempotencyKey)
        }
    }

    override fun getPaymentStatus(idempotencyKey: String): PaymentResponse {
        val transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
            ?: throw IllegalArgumentException("Transaction not found for idempotency key: $idempotencyKey")
        
        // If transaction is in a terminal state, return current status
        if (transaction.isTerminalState()) {
            return PaymentResponse(
                paymentId = transaction.idempotencyKey,
                status = transaction.status,
                amount = transaction.amount,
                currency = transaction.currency,
                paymentMode = transaction.paymentMode,
                paymentType = transaction.paymentType,
                vendorTransactionId = transaction.vendorTransactionId,
                metadata = transaction.metadata
            )
        }
        
        // Try to acquire a lock for this idempotency key
        val lockAcquired = redisLockService.acquireLock(idempotencyKey)
        if (!lockAcquired) {
            logger.info("Could not acquire lock for status check, idempotency key: $idempotencyKey, returning current status")
            return PaymentResponse(
                paymentId = transaction.idempotencyKey,
                status = transaction.status,
                amount = transaction.amount,
                currency = transaction.currency,
                paymentMode = transaction.paymentMode,
                paymentType = transaction.paymentType,
                vendorTransactionId = transaction.vendorTransactionId,
                metadata = transaction.metadata
            )
        }
        
        try {
            // Refresh transaction data
            val refreshedTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                ?: throw IllegalArgumentException("Transaction not found for idempotency key: $idempotencyKey")
            
            // If transaction is in a terminal state, return current status
            if (refreshedTransaction.isTerminalState()) {
                return PaymentResponse(
                    paymentId = refreshedTransaction.idempotencyKey,
                    status = refreshedTransaction.status,
                    amount = refreshedTransaction.amount,
                    currency = refreshedTransaction.currency,
                    paymentMode = refreshedTransaction.paymentMode,
                    paymentType = refreshedTransaction.paymentType,
                    vendorTransactionId = refreshedTransaction.vendorTransactionId,
                    metadata = refreshedTransaction.metadata
                )
            }
            
            // If transaction is pending, check with payment provider
            val provider = paymentProviderFactory.getProviderByName(refreshedTransaction.paymentProvider)
                ?: throw IllegalStateException("Payment provider not found: ${refreshedTransaction.paymentProvider}")
            
            // Check payment status with provider
            val vendorTransactionId = refreshedTransaction.vendorTransactionId
                ?: throw IllegalStateException("Vendor transaction ID not found for transaction: ${refreshedTransaction.id}")
            
            val providerResponse = provider.checkPaymentStatus(vendorTransactionId)
            
            // Update transaction status if changed
            if (providerResponse.status != refreshedTransaction.status) {
                try {
                    refreshedTransaction.status = providerResponse.status
                    refreshedTransaction.updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
                    transactionRepository.save(refreshedTransaction)
                    logger.info("Updated transaction status: ${refreshedTransaction.id} to ${providerResponse.status}")
                } catch (e: OptimisticLockingFailureException) {
                    logger.warn("Optimistic locking failure when updating transaction status", e)
                    // Get the latest version
                    val latestTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                        ?: throw IllegalArgumentException("Transaction not found for idempotency key: $idempotencyKey")
                    
                    return PaymentResponse(
                        paymentId = latestTransaction.idempotencyKey,
                        status = latestTransaction.status,
                        amount = latestTransaction.amount,
                        currency = latestTransaction.currency,
                        paymentMode = latestTransaction.paymentMode,
                        paymentType = latestTransaction.paymentType,
                        vendorTransactionId = latestTransaction.vendorTransactionId,
                        metadata = latestTransaction.metadata
                    )
                }
            }
            
            return PaymentResponse(
                paymentId = refreshedTransaction.idempotencyKey,
                status = providerResponse.status,
                amount = refreshedTransaction.amount,
                currency = refreshedTransaction.currency,
                paymentMode = refreshedTransaction.paymentMode,
                paymentType = refreshedTransaction.paymentType,
                vendorTransactionId = refreshedTransaction.vendorTransactionId,
                metadata = refreshedTransaction.metadata
            )
        } finally {
            // Always release the lock
            redisLockService.releaseLock(idempotencyKey)
        }
    }

    override fun getTransactionByIdempotencyKey(idempotencyKey: String): Transaction? {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
    }

    @Transactional
    override fun updateTransactionStatus(
        idempotencyKey: String, 
        status: TransactionStatus, 
        vendorTransactionId: String?
    ): Transaction? {
        // Try to acquire a lock for this idempotency key
        val lockAcquired = redisLockService.acquireLock(idempotencyKey)
        if (!lockAcquired) {
            logger.warn("Could not acquire lock for status update, idempotency key: $idempotencyKey")
            throw ConcurrentModificationException("Another request is updating this transaction")
        }
        
        try {
            val transaction = transactionRepository.findByIdempotencyKeyWithLock(idempotencyKey)
                ?: return null
            
            transaction.status = status
            if (vendorTransactionId != null) {
                transaction.vendorTransactionId = vendorTransactionId
            }
            
            try {
                return transactionRepository.save(transaction)
            } catch (e: OptimisticLockingFailureException) {
                logger.warn("Optimistic locking failure when updating transaction status", e)
                throw ConcurrentModificationException("Transaction was modified by another request")
            }
        } finally {
            // Always release the lock
            redisLockService.releaseLock(idempotencyKey)
        }
    }
}
