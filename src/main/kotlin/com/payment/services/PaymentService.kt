package com.payment.services

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse

/**
 * Service interface for payment operations
 */
interface PaymentService {
    /**
     * Initiate a payment transaction
     * @param userId ID of the user making the payment
     * @param request Payment request details
     * @param idempotencyKey Client-provided idempotency key
     * @return Payment response with transaction details
     */
    fun initiatePayment(userId: String, request: PaymentRequest, idempotencyKey: String): PaymentResponse
    
    /**
     * Get payment status by idempotency key
     * @param idempotencyKey Idempotency key of the payment
     * @return Payment response with current status
     */
    fun getPaymentStatus(idempotencyKey: String): PaymentResponse
    
    /**
     * Get transaction by idempotency key
     * @param idempotencyKey Idempotency key of the payment
     * @return Transaction or null if not found
     */
    fun getTransactionByIdempotencyKey(idempotencyKey: String): Transaction?
    
    /**
     * Update transaction status
     * @param idempotencyKey Idempotency key of the payment
     * @param status New status
     * @param vendorTransactionId Vendor transaction ID (optional)
     * @return Updated transaction or null if not found
     */
    fun updateTransactionStatus(idempotencyKey: String, status: TransactionStatus, vendorTransactionId: String? = null): Transaction?
}
