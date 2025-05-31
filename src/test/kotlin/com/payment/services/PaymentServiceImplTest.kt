package com.payment.services

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.providers.PaymentProvider
import com.payment.providers.PaymentProviderFactory
import com.payment.repositories.TransactionRepository
import com.payment.services.impl.PaymentServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.justRun
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PaymentServiceImplTest {

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
    fun `initiatePayment should process payment successfully`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-123"
        val paymentRequest = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR"
        )
        
        val transactionSlot = slot<Transaction>()
        val expectedResponse = PaymentResponse(
            paymentId = "transaction123",
            status = TransactionStatus.PENDING,
            amount = BigDecimal("100.00"),
            currency = "INR",
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            vendorTransactionId = "vendor123"
        )
        
        // Mock behavior
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null andThen null // Initial check and after lock check
        every { redisLockService.acquireLock(idempotencyKey) } returns true
        justRun { redisLockService.releaseLock(idempotencyKey) }
        every { paymentProviderFactory.getProvider("UPI", "GOOGLE_PAY") } returns paymentProvider
        every { paymentProvider.getName() } returns "UPI_PROVIDER"
        every { paymentProvider.processPayment(paymentRequest, idempotencyKey, userId) } returns expectedResponse
        every { transactionRepository.save(capture(transactionSlot)) } answers {
            transactionSlot.captured.copy(id = 1L)
        }
        
        // When
        val result = paymentService.initiatePayment(userId, paymentRequest, idempotencyKey)
        
        // Then
        assertNotNull(result)
        assertEquals(expectedResponse.paymentId, result.paymentId)
        assertEquals(expectedResponse.status, result.status)
        assertEquals(expectedResponse.amount, result.amount)
        
        // Verify correct order of method calls
        verifyOrder {
            transactionRepository.findByIdempotencyKey(idempotencyKey)
            redisLockService.acquireLock(idempotencyKey)
            transactionRepository.findByIdempotencyKey(idempotencyKey)
            paymentProviderFactory.getProvider("UPI", "GOOGLE_PAY")
            paymentProvider.processPayment(paymentRequest, idempotencyKey, userId)
            transactionRepository.save(any())
            redisLockService.releaseLock(idempotencyKey)
        }
        
        // Verify transaction properties
        with(transactionSlot.captured) {
            assertEquals(userId, this.userId)
            assertEquals(paymentRequest.amount, this.amount)
            assertEquals(paymentRequest.currency, this.currency)
            assertEquals(paymentRequest.paymentMode, this.paymentMode)
            assertEquals(paymentRequest.paymentType, this.paymentType)
            assertEquals("UPI_PROVIDER", this.paymentProvider)
            assertEquals(idempotencyKey, this.idempotencyKey)
            assertEquals(TransactionStatus.PENDING, this.status)
        }
    }
    
    @Test
    fun `initiatePayment should return existing transaction for same idempotency key`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "idem-key-123"
        val paymentRequest = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR"
        )
        
        val existingTransaction = Transaction(
            id = 1L,
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.SUCCEEDED,
            userId = userId,
            amount = BigDecimal("100.00"),
            currency = "INR",
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            paymentProvider = "UPI_PROVIDER",
            vendorTransactionId = "vendor123",
            metadata = emptyMap(),
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
            version = 0
        )
        
        // Mock behavior
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns existingTransaction
        
        // When
        val result = paymentService.initiatePayment(userId, paymentRequest, idempotencyKey)
        
        // Then
        assertNotNull(result)
        assertEquals(idempotencyKey, result.paymentId)
        assertEquals(TransactionStatus.SUCCEEDED, result.status)
        
        // Verify no payment processing occurred
        verify(exactly = 0) { paymentProviderFactory.getProvider(any(), any()) }
        verify(exactly = 0) { paymentProvider.processPayment(any(), any(), any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
    
    @Test
    fun `getPaymentStatus should return transaction status`() {
        // Given
        val transactionId = "transaction123"
        val transaction = Transaction(
            id = 1L,
            idempotencyKey = transactionId,
            status = TransactionStatus.SUCCEEDED,
            userId = "user123",
            amount = BigDecimal("100.00"),
            currency = "INR",
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            paymentProvider = "UPI_PROVIDER",
            vendorTransactionId = "vendor123",
            metadata = emptyMap(),
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
            version = 0
        )
        
        // Mock behavior
        every { transactionRepository.findByIdempotencyKey(transactionId) } returns transaction
        
        // When
        val result = paymentService.getPaymentStatus(transactionId)
        
        // Then
        assertNotNull(result)
        assertEquals(transactionId, result.paymentId)
        assertEquals(TransactionStatus.SUCCEEDED, result.status)
        assertEquals(transaction.amount, result.amount)
    }
}
