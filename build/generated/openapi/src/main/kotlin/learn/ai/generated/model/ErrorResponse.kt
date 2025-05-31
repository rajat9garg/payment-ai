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
 * @param status HTTP status code
 * @param message Error message
 * @param timestamp Timestamp when the error occurred
 */
data class ErrorResponse(

    @get:JsonProperty("status") val status: kotlin.Int? = null,

    @get:JsonProperty("message") val message: kotlin.String? = null,

    @get:JsonProperty("timestamp") val timestamp: java.time.OffsetDateTime? = null
) {

}

