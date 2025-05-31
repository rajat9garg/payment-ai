package com.payment.mappers

import com.payment.models.domain.Transaction
import com.payment.models.domain.TransactionStatus
import com.payment.models.dto.PaymentRequest
import com.payment.models.dto.PaymentResponse
import learn.ai.generated.model.PaymentInitiateRequest
import learn.ai.generated.model.PaymentResponse as ApiPaymentResponse
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Mapper for payment-related API models
 */
@Component
class PaymentApiMapper {
    
    /**
     * Convert API payment request to domain payment request
     * @param apiRequest API payment request
     * @return Domain payment request
     */
    fun toDomainRequest(apiRequest: PaymentInitiateRequest): PaymentRequest {
        return PaymentRequest(
            amount = BigDecimal(apiRequest.amount.toString()),
            paymentMode = apiRequest.paymentMode,
            paymentType = apiRequest.paymentType,
            currency = apiRequest.currency ?: "INR",
            metadata = apiRequest.metadata?.toMap() ?: emptyMap()
        )
    }
    
    /**
     * Convert domain payment response to API payment response
     * @param domainResponse Domain payment response
     * @return API payment response
     */
    fun toApiResponse(domainResponse: PaymentResponse): ApiPaymentResponse {
        return ApiPaymentResponse(
            paymentId = domainResponse.paymentId,
            status = domainResponse.status.name,
            amount = domainResponse.amount,
            currency = domainResponse.currency,
            paymentMode = domainResponse.paymentMode,
            paymentType = domainResponse.paymentType,
            vendorTransactionId = domainResponse.vendorTransactionId,
            timestamp = domainResponse.timestamp,
            metadata = domainResponse.metadata?.let { HashMap(it) }
        )
    }
    
    /**
     * Convert transaction to API payment response
     * @param transaction Transaction domain model
     * @return API payment response
     */
    fun transactionToApiResponse(transaction: Transaction): ApiPaymentResponse {
        return ApiPaymentResponse(
            paymentId = transaction.idempotencyKey,
            status = transaction.status.name,
            amount = transaction.amount,
            currency = transaction.currency,
            paymentMode = transaction.paymentMode,
            paymentType = transaction.paymentType,
            vendorTransactionId = transaction.vendorTransactionId,
            timestamp = transaction.createdAt,
            metadata = HashMap(transaction.metadata)
        )
    }
    
    /**
     * Convert API status to domain status
     * @param status API status string
     * @return Domain transaction status
     */
    fun toTransactionStatus(status: String): TransactionStatus {
        return TransactionStatus.fromString(status)
    }
}
