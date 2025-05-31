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
 * @param typeCode Payment type code
 * @param typeName Payment type name
 * @param description Payment type description
 * @param isActive Whether this payment type is active
 */
data class PaymentType(

    @get:JsonProperty("typeCode") val typeCode: kotlin.String? = null,

    @get:JsonProperty("typeName") val typeName: kotlin.String? = null,

    @get:JsonProperty("description") val description: kotlin.String? = null,

    @get:JsonProperty("isActive") val isActive: kotlin.Boolean? = null
) {

}

