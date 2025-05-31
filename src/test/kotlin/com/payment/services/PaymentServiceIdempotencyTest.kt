package com.payment.services

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.providers.PaymentProvider
import com.payment.providers.PaymentProviderFactory
import com.payment.repositories.TransactionRepository
import com.payment.services.impl.PaymentServiceImpl
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaymentServiceIdempotencyTest {

    private lateinit var idempotencyKeyService: IdempotencyKeyService
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var paymentProviderFactory: PaymentProviderFactory
    private lateinit var paymentProvider: PaymentProvider
    private lateinit var redisLockService: RedisLockService
    private lateinit var paymentService: PaymentServiceImpl

    @BeforeEach
    fun setup() {
        idempotencyKeyService = mockk()
        transactionRepository = mockk()
        paymentProviderFactory = mockk()
        paymentProvider = mockk()
        redisLockService = mockk()
        
        paymentService = PaymentServiceImpl(
            idempotencyKeyService,
            transactionRepository,
            paymentProviderFactory,
            redisLockService
        )
    }
    
    @Test
    fun `initiatePayment should return existing transaction for same idempotency key`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-123"
        val request = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR",
            metadata = mapOf("orderId" to "order123")
        )
        
        val existingTransaction = Transaction(
            id = 1L,
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.SUCCEEDED,
            userId = userId,
            amount = request.amount,
            currency = request.currency,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            paymentProvider = "UPI_PROVIDER",
            vendorTransactionId = "vendor123",
            metadata = emptyMap(),
            createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10),
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5),
            version = 1
        )
        
        // Mock repository to return existing transaction
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns existingTransaction
        
        // When
        val result = paymentService.initiatePayment(userId, request, idempotencyKey)
        
        // Then
        assertEquals(idempotencyKey, result.paymentId)
        assertEquals(existingTransaction.status, result.status)
        assertEquals(existingTransaction.amount, result.amount)
        
        // Verify that no further processing was done
        verify(exactly = 0) { paymentProviderFactory.getProvider(any(), any()) }
        verify(exactly = 0) { paymentProvider.processPayment(any(), any(), any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
    
    @Test
    fun `initiatePayment should process new payment when idempotency key is unique`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-456"
        val request = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR",
            metadata = mapOf("orderId" to "order123")
        )
        
        val paymentResponse = PaymentResponse(
            paymentId = idempotencyKey,
            status = TransactionStatus.PENDING,
            amount = request.amount,
            currency = request.currency,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            vendorTransactionId = "vendor456"
        )
        
        // Mock dependencies
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
        every { redisLockService.acquireLock(idempotencyKey) } returns true
        every { redisLockService.releaseLock(idempotencyKey) } just Runs
        every { paymentProviderFactory.getProvider(request.paymentMode, request.paymentType) } returns paymentProvider
        every { paymentProvider.getName() } returns "UPI_PROVIDER"
        every { paymentProvider.processPayment(request, idempotencyKey, userId) } returns paymentResponse
        
        val transactionSlot = slot<Transaction>()
        every { transactionRepository.save(capture(transactionSlot)) } answers {
            transactionSlot.captured.copy(id = 2L)
        }
        
        // When
        val result = paymentService.initiatePayment(userId, request, idempotencyKey)
        
        // Then
        assertEquals(idempotencyKey, result.paymentId)
        assertEquals(TransactionStatus.PENDING, result.status)
        assertEquals(request.amount, result.amount)
        
        // Verify transaction was saved
        verify(exactly = 1) { transactionRepository.save(any()) }
        verify(exactly = 1) { redisLockService.acquireLock(idempotencyKey) }
        verify(exactly = 1) { redisLockService.releaseLock(idempotencyKey) }
    }
    
    @Test
    fun `initiatePayment should throw exception when lock cannot be acquired`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-789"
        val request = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        // Mock dependencies
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
        every { redisLockService.acquireLock(idempotencyKey) } returns false
        
        // When/Then
        assertThrows<ConcurrentModificationException> {
            paymentService.initiatePayment(userId, request, idempotencyKey)
        }
        
        // Verify lock was attempted but no further processing
        verify(exactly = 1) { redisLockService.acquireLock(idempotencyKey) }
        verify(exactly = 0) { paymentProviderFactory.getProvider(any(), any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
    
    @Test
    fun `initiatePayment should handle concurrent transaction creation`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-concurrent"
        val request = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        val concurrentTransaction = Transaction(
            id = 3L,
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.PENDING,
            userId = userId,
            amount = request.amount,
            currency = request.currency,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            paymentProvider = "UPI_PROVIDER",
            vendorTransactionId = "vendor789",
            metadata = emptyMap(),
            version = 0
        )
        
        val paymentResponse = PaymentResponse(
            paymentId = idempotencyKey,
            status = TransactionStatus.PENDING,
            amount = request.amount,
            currency = request.currency,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            vendorTransactionId = "vendor789"
        )
        
        // First check returns null, second check (after lock) returns the transaction
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returnsMany listOf(null, concurrentTransaction)
        every { redisLockService.acquireLock(idempotencyKey) } returns true
        every { redisLockService.releaseLock(idempotencyKey) } just Runs
        
        // When
        val result = paymentService.initiatePayment(userId, request, idempotencyKey)
        
        // Then
        assertEquals(idempotencyKey, result.paymentId)
        assertEquals(TransactionStatus.PENDING, result.status)
        
        // Verify we checked twice but didn't process payment
        verify(exactly = 2) { transactionRepository.findByIdempotencyKey(idempotencyKey) }
        verify(exactly = 0) { paymentProviderFactory.getProvider(any(), any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
