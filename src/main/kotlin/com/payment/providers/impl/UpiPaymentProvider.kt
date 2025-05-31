package com.payment.providers.impl

import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import com.payment.providers.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Implementation of PaymentProvider for UPI payments
 */
@Component
class UpiPaymentProvider : PaymentProvider {
    
    private val logger = LoggerFactory.getLogger(UpiPaymentProvider::class.java)
    
    override fun getName(): String = "UPI_PROVIDER"
    
    override fun supports(paymentMode: String, paymentType: String): Boolean {
        return paymentMode == "UPI" && (
            paymentType == "GOOGLE_PAY" || 
            paymentType == "PHONE_PE" || 
            paymentType == "PAYTM" ||
            paymentType == "BHIM"
        )
    }
    
    override fun processPayment(request: PaymentRequest, idempotencyKey: String, userId: String): PaymentResponse {
        logger.info("Processing UPI payment for user: $userId, amount: ${request.amount}, mode: ${request.paymentType}")
        
        // In a real implementation, this would call the UPI payment gateway
        // For this mock implementation, we'll simulate a successful payment
        
        // Generate a mock vendor transaction ID
        val vendorTransactionId = "UPI_${UUID.randomUUID()}"
        
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
        logger.info("Checking UPI payment status for transaction: $vendorTransactionId")
        
        // In a real implementation, this would call the UPI payment gateway to check status
        // For this mock implementation, we'll randomly return SUCCESS or PENDING
        
        val status = if (Math.random() > 0.3) TransactionStatus.SUCCEEDED else TransactionStatus.PENDING
        
        return PaymentResponse(
            paymentId = "MOCK_ID",
            status = status,
            amount = java.math.BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            vendorTransactionId = vendorTransactionId
        )
    }
}
