package com.payment.controllers

import com.payment.mappers.PaymentModeApiMapper
import com.payment.mappers.PaymentApiMapper
import com.payment.services.PaymentModesService
import com.payment.services.PaymentService
import learn.ai.generated.api.PaymentApi
import learn.ai.generated.model.PaymentInitiateRequest
import learn.ai.generated.model.PaymentModesResponse
import learn.ai.generated.model.PaymentResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Controller implementation for payment-related endpoints
 */
@RestController
class PaymentController(
    private val paymentModesService: PaymentModesService,
    private val paymentModeApiMapper: PaymentModeApiMapper,
    private val paymentService: PaymentService,
    private val paymentApiMapper: PaymentApiMapper
) : PaymentApi {

    /**
     * Get available payment modes
     * @return ResponseEntity containing payment modes
     */
    override fun getPaymentModes(): ResponseEntity<PaymentModesResponse> {
        // Get payment modes from service
        val serviceResponse = paymentModesService.getPaymentModes()
        
        // Convert to API response
        val apiResponse = paymentModeApiMapper.toApiResponse(serviceResponse)
        
        // Return response
        return ResponseEntity.ok(apiResponse)
    }

    /**
     * Initiate a payment transaction
     * @param userId ID of the user making the payment
     * @param idempotencyKey Client-provided idempotency key
     * @param paymentInitiateRequest Payment initiation request
     * @return ResponseEntity containing payment response
     */
    override fun initiatePayment(
        userId: String,
        idempotencyKey: UUID,
        paymentInitiateRequest: PaymentInitiateRequest
    ): ResponseEntity<PaymentResponse> {
        // Convert API request to domain request
        val domainRequest = paymentApiMapper.toDomainRequest(paymentInitiateRequest)
        
        // Call service to initiate payment
        val serviceResponse = paymentService.initiatePayment(userId, domainRequest, idempotencyKey.toString())
        
        // Convert to API response
        val apiResponse = paymentApiMapper.toApiResponse(serviceResponse)
        
        // Return response
        return ResponseEntity.ok(apiResponse)
    }
}
