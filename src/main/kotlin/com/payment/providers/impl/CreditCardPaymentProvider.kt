package com.payment.providers.impl

import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.providers.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Implementation of PaymentProvider for credit card payments
 */
@Component
class CreditCardPaymentProvider : PaymentProvider {
    
    private val logger = LoggerFactory.getLogger(CreditCardPaymentProvider::class.java)
    
    override fun getName(): String = "CREDIT_CARD_PROVIDER"
    
    override fun supports(paymentMode: String, paymentType: String): Boolean {
        return paymentMode == "CREDIT_CARD" && (
            paymentType == "VISA" || 
            paymentType == "MASTERCARD" || 
            paymentType == "AMEX" ||
            paymentType == "RUPAY"
        )
    }
    
    override fun processPayment(request: PaymentRequest, idempotencyKey: String, userId: String): PaymentResponse {
        logger.info("Processing credit card payment for user: $userId, amount: ${request.amount}, type: ${request.paymentType}")
        
        // In a real implementation, this would call the credit card payment gateway
        // For this mock implementation, we'll simulate a successful payment
        
        // Generate a mock vendor transaction ID
        val vendorTransactionId = "CC_${UUID.randomUUID()}"
        
        // Return a pending payment response
        return PaymentResponse(
            paymentId = idempotencyKey,
            status = TransactionStatus.PENDING,
            amount = request.amount,
            currency = request.currency,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            vendorTransactionId = vendorTransactionId,
            metadata = request.metadata
        )
    }
    
    override fun checkPaymentStatus(vendorTransactionId: String): PaymentResponse {
        logger.info("Checking credit card payment status for transaction: $vendorTransactionId")
        
        // In a real implementation, this would call the credit card payment gateway to check status
        // For this mock implementation, we'll randomly return SUCCESS or PENDING
        
        val status = if (Math.random() > 0.2) TransactionStatus.SUCCEEDED else TransactionStatus.PENDING
        
        return PaymentResponse(
            paymentId = "MOCK_ID",
            status = status,
            amount = java.math.BigDecimal("100.00"),
            paymentMode = "CREDIT_CARD",
            paymentType = "VISA",
            vendorTransactionId = vendorTransactionId
        )
    }
}
