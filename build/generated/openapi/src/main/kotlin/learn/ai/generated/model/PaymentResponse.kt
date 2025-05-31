package learn.ai.generated.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.Valid

/**
 * 
 * @param paymentId Unique payment ID
 * @param status Payment status (PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED)
 * @param amount Payment amount
 * @param currency Currency code
 * @param paymentMode Payment mode
 * @param paymentType Payment type
 * @param vendorTransactionId Transaction ID from the payment provider
 * @param timestamp Timestamp of the payment
 * @param metadata Additional metadata for the payment
 */
data class PaymentResponse(

    @get:JsonProperty("paymentId") val paymentId: kotlin.String? = null,

    @get:JsonProperty("status") val status: kotlin.String? = null,

    @get:JsonProperty("amount") val amount: java.math.BigDecimal? = null,

    @get:JsonProperty("currency") val currency: kotlin.String? = null,

    @get:JsonProperty("paymentMode") val paymentMode: kotlin.String? = null,

    @get:JsonProperty("paymentType") val paymentType: kotlin.String? = null,

    @get:JsonProperty("vendorTransactionId") val vendorTransactionId: kotlin.String? = null,

    @get:JsonProperty("timestamp") val timestamp: java.time.OffsetDateTime? = null,

    @field:Valid
    @get:JsonProperty("metadata") val metadata: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null
) {

}

