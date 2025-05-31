package com.payment.mappers

import com.payment.models.dto.HealthResponse
import org.springframework.stereotype.Component

@Component
class HealthMapper {
    fun toHealthResponse(status: String): HealthResponse {
        return HealthResponse(status = status)
    }
}
