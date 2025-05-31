package com.payment.controllers

import com.payment.mappers.PaymentApiMapper
import com.payment.mappers.PaymentModeApiMapper
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.services.PaymentModesService
import com.payment.services.PaymentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import learn.ai.generated.model.PaymentInitiateRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaymentControllerTest {

    private lateinit var paymentModesService: PaymentModesService
    private lateinit var paymentModeApiMapper: PaymentModeApiMapper
    private lateinit var paymentService: PaymentService
    private lateinit var paymentApiMapper: PaymentApiMapper
    private lateinit var paymentController: PaymentController

    @BeforeEach
    fun setup() {
        paymentModesService = mockk()
        paymentModeApiMapper = mockk()
        paymentService = mockk()
        paymentApiMapper = mockk()
        
        paymentController = PaymentController(
            paymentModesService,
            paymentModeApiMapper,
            paymentService,
            paymentApiMapper
        )
    }
    
    @Test
    fun `initiatePayment should pass idempotency key to service layer`() {
        // Given
        val userId = "user123"
        val idempotencyKey = UUID.randomUUID()
        val apiRequest = PaymentInitiateRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        val domainRequest = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR"
        )
        
        val serviceResponse = PaymentResponse(
            paymentId = idempotencyKey.toString(),
            status = TransactionStatus.PENDING,
            amount = BigDecimal("100.00"),
            currency = "INR",
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            vendorTransactionId = "vendor123"
        )
        
        val apiResponse = learn.ai.generated.model.PaymentResponse(
            paymentId = idempotencyKey.toString(),
            status = "PENDING",
            amount = BigDecimal("100.00"),
            currency = "INR",
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        // Mock behavior
        every { paymentApiMapper.toDomainRequest(apiRequest) } returns domainRequest
        every { paymentService.initiatePayment(userId, domainRequest, idempotencyKey.toString()) } returns serviceResponse
        every { paymentApiMapper.toApiResponse(serviceResponse) } returns apiResponse
        
        // When
        val response = paymentController.initiatePayment(userId, idempotencyKey, apiRequest)
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(idempotencyKey.toString(), response.body?.paymentId)
        
        // Verify service was called with idempotency key
        verify(exactly = 1) { paymentService.initiatePayment(userId, domainRequest, idempotencyKey.toString()) }
    }
}
