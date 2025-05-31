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
 * @param amount Payment amount
 * @param paymentMode Payment mode (UPI, CREDIT_CARD, DEBIT_CARD)
 * @param paymentType Payment type (GOOGLE_PAY, PHONE_PE, VISA, MASTERCARD)
 * @param currency Currency code
 * @param metadata Additional metadata for the payment
 */
data class PaymentInitiateRequest(

    @get:JsonProperty("amount", required = true) val amount: java.math.BigDecimal,

    @get:JsonProperty("paymentMode", required = true) val paymentMode: kotlin.String,

    @get:JsonProperty("paymentType", required = true) val paymentType: kotlin.String,

    @get:JsonProperty("currency") val currency: kotlin.String? = "INR",

    @field:Valid
    @get:JsonProperty("metadata") val metadata: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null
) {

}

