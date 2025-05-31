package com.payment.models.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.payment.models.domain.TransactionStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * DTO for payment response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResponse(
    val paymentId: String,
    val status: TransactionStatus,
    val amount: BigDecimal,
    val currency: String = "INR",
    val paymentMode: String,
    val paymentType: String,
    val vendorTransactionId: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    val metadata: Map<String, Any>? = null
)
