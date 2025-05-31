package com.payment.models.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

/**
 * DTO for payment initiation request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentRequest(
    val amount: BigDecimal,
    val paymentMode: String,
    val paymentType: String,
    val currency: String = "INR",
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be greater than zero" }
        require(paymentMode.isNotBlank()) { "Payment mode is required" }
        require(paymentType.isNotBlank()) { "Payment type is required" }
    }
}
