package com.payment.controllers

import com.payment.mappers.HealthMapper
import com.payment.models.dto.HealthResponse
import com.payment.services.HealthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health", description = "Health Check Endpoints")
class HealthController(
    private val healthService: HealthService,
    private val healthMapper: HealthMapper
) {
    @GetMapping("/api/v1/health")
    @Operation(summary = "Health check endpoint", description = "Returns the health status of the application")
    fun healthCheck(): ResponseEntity<HealthResponse> {
        val status = healthService.checkHealth()
        val response = healthMapper.toHealthResponse(status)
        return ResponseEntity.ok(response)
    }
}
