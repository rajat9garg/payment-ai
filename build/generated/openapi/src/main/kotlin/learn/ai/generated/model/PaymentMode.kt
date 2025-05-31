package learn.ai.generated.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import learn.ai.generated.model.PaymentType
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
 * @param modeCode Payment mode code
 * @param modeName Payment mode name
 * @param description Payment mode description
 * @param isActive Whether this payment mode is active
 * @param paymentTypes 
 */
data class PaymentMode(

    @get:JsonProperty("modeCode") val modeCode: kotlin.String? = null,

    @get:JsonProperty("modeName") val modeName: kotlin.String? = null,

    @get:JsonProperty("description") val description: kotlin.String? = null,

    @get:JsonProperty("isActive") val isActive: kotlin.Boolean? = null,

    @field:Valid
    @get:JsonProperty("paymentTypes") val paymentTypes: kotlin.collections.List<PaymentType>? = null
) {

}

