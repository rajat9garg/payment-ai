package com.payment.providers

import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse

/**
 * Interface for payment providers
 */
interface PaymentProvider {
    /**
     * Get the name of the payment provider
     * @return Provider name
     */
    fun getName(): String
    
    /**
     * Check if the provider supports a specific payment mode and type
     * @param paymentMode The payment mode (UPI, CREDIT_CARD, etc.)
     * @param paymentType The payment type (GOOGLE_PAY, VISA, etc.)
     * @return True if the provider supports the payment mode and type
     */
    fun supports(paymentMode: String, paymentType: String): Boolean
    
    /**
     * Process a payment request
     * @param request The payment request
     * @param idempotencyKey The idempotency key for the transaction
     * @param userId The ID of the user making the payment
     * @return The payment response
     */
    fun processPayment(request: PaymentRequest, idempotencyKey: String, userId: String): PaymentResponse
    
    /**
     * Check the status of a payment
     * @param vendorTransactionId The vendor-specific transaction ID
     * @return The payment response with current status
     */
    fun checkPaymentStatus(vendorTransactionId: String): PaymentResponse
}
