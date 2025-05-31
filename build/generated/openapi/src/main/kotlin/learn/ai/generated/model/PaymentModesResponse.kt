package learn.ai.generated.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import learn.ai.generated.model.PaymentMode
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
 * @param paymentModes 
 */
data class PaymentModesResponse(

    @field:Valid
    @get:JsonProperty("paymentModes") val paymentModes: kotlin.collections.List<PaymentMode>? = null
) {

}

