# Task 1: Payment Modes API Implementation Plan

## 1. Overview

This document provides a detailed implementation plan for the Payment Modes API, which allows clients to retrieve available payment modes based on user ID and product type.

### API Endpoint
- **GET /v1/payment/modes**
- **Headers**: userId, productType
- **Response**: JSON with available payment modes

### Implementation Approach
Following the OpenAPI-first approach and the established project rules, we'll implement this API in a structured manner from database to API layer.

## 2. Database Implementation

### 2.1 Database Migration

Create a migration script for the payment_modes table:

```sql
-- src/main/resources/db/migration/V1__create_payment_modes_table.sql
CREATE TABLE payment_modes (
    id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    payment_mode VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_payment_modes_payment_mode ON payment_modes(payment_mode);
CREATE INDEX idx_payment_modes_type ON payment_modes(type);

-- Insert initial data
INSERT INTO payment_modes (uuid, payment_mode, type) VALUES 
    (gen_random_uuid(), 'UPI', 'GOOGLE_PAY'),
    (gen_random_uuid(), 'UPI', 'CRED'),
    (gen_random_uuid(), 'CREDIT_CARD', 'VISA'),
    (gen_random_uuid(), 'CREDIT_CARD', 'MASTERCARD'),
    (gen_random_uuid(), 'DEBIT_CARD', 'VISA'),
    (gen_random_uuid(), 'DEBIT_CARD', 'MASTERCARD');
```

### 2.2 Run Migration and Generate jOOQ Classes

Execute the Flyway migration and generate jOOQ classes:

```bash
./gradlew flywayMigrate
./gradlew generateJooq
```

## 3. Domain Model Implementation

### 3.1 Create Domain Models

Create the domain model for payment modes:

```kotlin
// src/main/kotlin/com/payment/models/domain/PaymentMode.kt
package com.payment.models.domain

import java.time.LocalDateTime
import java.util.UUID

data class PaymentMode(
    val id: Long? = null,
    val uuid: UUID,
    val paymentMode: String,
    val type: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
```

### 3.2 Create Database Mappers

Create mappers to convert between domain models and database records:

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentModeMapper.kt
package com.payment.mappers

import com.payment.models.domain.PaymentMode
import com.payment.jooq.tables.records.PaymentModesRecord
import java.util.UUID

object PaymentModeMapper {
    
    fun toDomain(record: PaymentModesRecord): PaymentMode {
        return PaymentMode(
            id = record.id,
            uuid = UUID.fromString(record.uuid),
            paymentMode = record.paymentMode,
            type = record.type,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
    
    fun toRecord(domain: PaymentMode, record: PaymentModesRecord): PaymentModesRecord {
        record.uuid = domain.uuid.toString()
        record.paymentMode = domain.paymentMode
        record.type = domain.type
        // Let database handle timestamps if null
        domain.createdAt?.let { record.createdAt = it }
        domain.updatedAt?.let { record.updatedAt = it }
        return record
    }
}
```

## 4. Repository Layer Implementation

### 4.1 Create Repository Interface

Define the repository interface for payment modes:

```kotlin
// src/main/kotlin/com/payment/repositories/PaymentModesRepository.kt
package com.payment.repositories

import com.payment.models.domain.PaymentMode
import java.util.UUID

interface PaymentModesRepository {
    fun findAll(): List<PaymentMode>
    fun findByPaymentMode(paymentMode: String): List<PaymentMode>
    fun findByType(type: String): List<PaymentMode>
    fun findByUuid(uuid: UUID): PaymentMode?
    fun save(paymentMode: PaymentMode): PaymentMode
    fun delete(uuid: UUID): Boolean
}
```

### 4.2 Implement Repository Using jOOQ

Implement the repository interface using jOOQ:

```kotlin
// src/main/kotlin/com/payment/repositories/impl/PaymentModesRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.PaymentModes
import com.payment.jooq.tables.references.PAYMENT_MODES
import com.payment.mappers.PaymentModeMapper
import com.payment.models.domain.PaymentMode
import com.payment.repositories.PaymentModesRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class PaymentModesRepositoryImpl(private val dsl: DSLContext) : PaymentModesRepository {
    
    override fun findAll(): List<PaymentMode> {
        return dsl.selectFrom(PAYMENT_MODES)
            .fetch()
            .map { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findByPaymentMode(paymentMode: String): List<PaymentMode> {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.PAYMENT_MODE.eq(paymentMode))
            .fetch()
            .map { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findByType(type: String): List<PaymentMode> {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.TYPE.eq(type))
            .fetch()
            .map { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findByUuid(uuid: UUID): PaymentMode? {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.UUID.eq(uuid.toString()))
            .fetchOne()
            ?.let { PaymentModeMapper.toDomain(it) }
    }
    
    override fun save(paymentMode: PaymentMode): PaymentMode {
        val now = LocalDateTime.now()
        
        if (paymentMode.id == null) {
            // Insert new record
            val record = dsl.newRecord(PAYMENT_MODES)
            PaymentModeMapper.toRecord(paymentMode.copy(createdAt = now, updatedAt = now), record)
            record.store()
            return PaymentModeMapper.toDomain(record)
        } else {
            // Update existing record
            val record = dsl.selectFrom(PAYMENT_MODES)
                .where(PAYMENT_MODES.ID.eq(paymentMode.id))
                .fetchOne() ?: throw IllegalArgumentException("Payment mode not found with id: ${paymentMode.id}")
                
            PaymentModeMapper.toRecord(paymentMode.copy(updatedAt = now), record)
            record.store()
            return PaymentModeMapper.toDomain(record)
        }
    }
    
    override fun delete(uuid: UUID): Boolean {
        return dsl.deleteFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.UUID.eq(uuid.toString()))
            .execute() > 0
    }
}

## 5. Service Layer Implementation

### 5.1 Create Service Interface

Define the service interface for payment modes:

```kotlin
// src/main/kotlin/com/payment/services/PaymentModesService.kt
package com.payment.services

interface PaymentModesService {
    /**
     * Get available payment modes for a user and product type
     * 
     * @param userId The ID of the user
     * @param productType The type of product
     * @return Map of payment modes formatted according to API requirements
     */
    fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any>
}
```

### 5.2 Implement Service

Implement the service interface with the business logic:

```kotlin
// src/main/kotlin/com/payment/services/impl/PaymentModesServiceImpl.kt
package com.payment.services.impl

import com.payment.models.domain.PaymentMode
import com.payment.repositories.PaymentModesRepository
import com.payment.services.PaymentModesService
import org.springframework.stereotype.Service

@Service
class PaymentModesServiceImpl(
    private val paymentModesRepository: PaymentModesRepository
) : PaymentModesService {

    override fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any> {
        // Step 1: Check payment modes available for the user
        val userModes = getUserPaymentModes(userId)
        
        // Step 2: Check payment modes available for the product type
        val productModes = getProductPaymentModes(productType)
        
        // Step 3: Check available payment modes for the vendor
        val vendorModes = getVendorPaymentModes()
        
        // Step 4: Calculate intersection of available modes
        val availableModes = calculateAvailableModes(userModes, productModes, vendorModes)
        
        // Step 5: Format response according to API requirements
        return formatPaymentModesResponse(availableModes)
    }
    
    /**
     * Get payment modes available for a specific user
     * Note: In a real implementation, this would query user preferences or restrictions
     */
    private fun getUserPaymentModes(userId: String): List<PaymentMode> {
        // For this implementation, we'll assume all payment modes are available to all users
        // In a real system, this would query user-specific payment modes
        return paymentModesRepository.findAll()
    }
    
    /**
     * Get payment modes available for a specific product type
     * Note: In a real implementation, this would query product-specific payment methods
     */
    private fun getProductPaymentModes(productType: String): List<PaymentMode> {
        // For this implementation, we'll assume all payment modes are available for all products
        // In a real system, this would query product-specific payment modes
        return paymentModesRepository.findAll()
    }
    
    /**
     * Get payment modes supported by vendors
     */
    private fun getVendorPaymentModes(): List<PaymentMode> {
        // For this implementation, we'll use all payment modes in the database
        return paymentModesRepository.findAll()
    }
    
    /**
     * Calculate the intersection of available payment modes
     */
    private fun calculateAvailableModes(
        userModes: List<PaymentMode>,
        productModes: List<PaymentMode>,
        vendorModes: List<PaymentMode>
    ): List<PaymentMode> {
        // For simplicity, we'll just return the intersection based on payment mode and type
        val userModeSet = userModes.map { "${it.paymentMode}:${it.type}" }.toSet()
        val productModeSet = productModes.map { "${it.paymentMode}:${it.type}" }.toSet()
        val vendorModeSet = vendorModes.map { "${it.paymentMode}:${it.type}" }.toSet()
        
        // Find the intersection of all three sets
        val intersectionSet = userModeSet.intersect(productModeSet).intersect(vendorModeSet)
        
        // Convert back to PaymentMode objects
        return vendorModes.filter { "${it.paymentMode}:${it.type}" in intersectionSet }
    }
    
    /**
     * Format payment modes according to API response requirements
     */
    private fun formatPaymentModesResponse(modes: List<PaymentMode>): Map<String, Any> {
        val response = mutableMapOf<String, Any>()
        
        // Group by payment mode type (UPI, CREDIT_CARD, DEBIT_CARD)
        val modesByType = modes.groupBy { it.paymentMode }
        
        // Format UPI modes as specified in requirements
        if (modesByType.containsKey("UPI")) {
            val upiTypes = modesByType["UPI"]?.map { it.type }?.toSet() ?: emptySet()
            response["UPI"] = mapOf<String, Any>().plus(upiTypes.associateWith { emptyMap<String, Any>() })
        }
        
        // Add CREDIT_CARD if available
        if (modesByType.containsKey("CREDIT_CARD")) {
            response["CREDIT_CARD"] = emptyMap<String, Any>()
        }
        
        // Add DEBIT_CARD if available
        if (modesByType.containsKey("DEBIT_CARD")) {
            response["DEBIT_CARD"] = emptyMap<String, Any>()
        }
        
        return response
    }
}

## 6. OpenAPI Specification

### 6.1 Update OpenAPI Specification

Update the OpenAPI specification in `src/main/resources/openapi/api.yaml`:

```yaml
openapi: 3.0.3
info:
  title: Payment Gateway API
  description: API for payment processing
  version: 1.0.0
  contact:
    name: API Support
    email: support@example.com
  license:
    name: Proprietary
servers:
  - url: /api/v1
    description: Development server
paths:
  /payment/modes:
    get:
      summary: Get available payment modes
      description: Returns available payment modes for the user and product type
      operationId: getPaymentModes
      tags:
        - Payment
      parameters:
        - name: userId
          in: header
          required: true
          schema:
            type: string
          description: ID of the user
        - name: productType
          in: header
          required: true
          schema:
            type: string
          description: Type of product
      responses:
        '200':
          description: Available payment modes
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentModesResponse'
        '400':
          description: Bad request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
components:
  schemas:
    PaymentModesResponse:
      type: object
      additionalProperties: true
      example:
        UPI:
          GOOGLE_PAY: {}
          CRED: {}
        CREDIT_CARD: {}
        DEBIT_CARD: {}
    ErrorResponse:
      type: object
      properties:
        status:
          type: integer
          format: int32
          example: 400
        message:
          type: string
          example: "Bad request"
        timestamp:
          type: string
          format: date-time
          example: "2025-05-31T12:34:56Z"

## 7. Controller Layer Implementation

### 7.1 Generate OpenAPI Models and Interfaces

Run the OpenAPI generator to create the controller interfaces and models:

```bash
./gradlew openApiGenerate
```

This will generate:
- Controller interfaces in `build/generated/src/main/kotlin/com/payment/api`
- API models in `build/generated/src/main/kotlin/com/payment/model`

### 7.2 Create API Model Mappers

Create mappers to convert between domain models and API models:

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentModesApiMapper.kt
package com.payment.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class PaymentModesApiMapper(private val objectMapper: ObjectMapper) {
    
    /**
     * Convert a map of payment modes to the API response format
     */
    fun toApiResponse(paymentModes: Map<String, Any>): Any {
        // Since the response is a dynamic structure, we'll use ObjectMapper
        // to convert between the service response and the API response
        return objectMapper.convertValue(paymentModes, Any::class.java)
    }
}
```

### 7.3 Implement Controller

Implement the controller interface generated from OpenAPI:

```kotlin
// src/main/kotlin/com/payment/controllers/PaymentModesControllerImpl.kt
package com.payment.controllers

import com.payment.api.PaymentApi
import com.payment.mappers.PaymentModesApiMapper
import com.payment.services.PaymentModesService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentModesControllerImpl(
    private val paymentModesService: PaymentModesService,
    private val paymentModesApiMapper: PaymentModesApiMapper
) : PaymentApi {

    override fun getPaymentModes(userId: String, productType: String): ResponseEntity<Any> {
        // Validate input parameters
        if (userId.isBlank()) {
            throw IllegalArgumentException("userId cannot be blank")
        }
        
        if (productType.isBlank()) {
            throw IllegalArgumentException("productType cannot be blank")
        }
        
        // Call service to get available payment modes
        val paymentModes = paymentModesService.getAvailablePaymentModes(userId, productType)
        
        // Convert to API response format
        val response = paymentModesApiMapper.toApiResponse(paymentModes)
        
        // Return response
        return ResponseEntity.ok(response)
    }
}
```

### 7.4 Implement Error Handling

Create a global exception handler for the API:

```kotlin
// src/main/kotlin/com/payment/exceptions/GlobalExceptionHandler.kt
package com.payment.exceptions

import com.payment.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.OffsetDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Bad request",
            timestamp = OffsetDateTime.now().toString()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "Internal server error",
            timestamp = OffsetDateTime.now().toString()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
```

## 8. Testing

### 8.1 Unit Testing

Create unit tests for each layer:

```kotlin
// src/test/kotlin/com/payment/services/PaymentModesServiceImplTest.kt
package com.payment.services

import com.payment.models.domain.PaymentMode
import com.payment.repositories.PaymentModesRepository
import com.payment.services.impl.PaymentModesServiceImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PaymentModesServiceImplTest {

    private val paymentModesRepository: PaymentModesRepository = mockk()
    private val paymentModesService = PaymentModesServiceImpl(paymentModesRepository)
    
    @Test
    fun `getAvailablePaymentModes should return formatted payment modes`() {
        // Given
        val userId = "user123"
        val productType = "PRODUCT_A"
        
        val paymentModes = listOf(
            PaymentMode(
                id = 1,
                uuid = UUID.randomUUID(),
                paymentMode = "UPI",
                type = "GOOGLE_PAY",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            PaymentMode(
                id = 2,
                uuid = UUID.randomUUID(),
                paymentMode = "UPI",
                type = "CRED",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            PaymentMode(
                id = 3,
                uuid = UUID.randomUUID(),
                paymentMode = "CREDIT_CARD",
                type = "VISA",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        // When
        every { paymentModesRepository.findAll() } returns paymentModes
        val result = paymentModesService.getAvailablePaymentModes(userId, productType)
        
        // Then
        assertTrue(result.containsKey("UPI"))
        assertTrue(result.containsKey("CREDIT_CARD"))
        
        @Suppress("UNCHECKED_CAST")
        val upiModes = result["UPI"] as Map<String, Any>
        assertTrue(upiModes.containsKey("GOOGLE_PAY"))
        assertTrue(upiModes.containsKey("CRED"))
    }
}
```

### 8.2 Integration Testing

Create integration tests for the API:

```kotlin
// src/test/kotlin/com/payment/controllers/PaymentModesControllerIntegrationTest.kt
package com.payment.controllers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class PaymentModesControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `getPaymentModes should return available payment modes`() {
        mockMvc.perform(
            get("/api/v1/payment/modes")
                .header("userId", "user123")
                .header("productType", "PRODUCT_A")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.UPI").exists())
    }
    
    @Test
    fun `getPaymentModes should return 400 when userId is missing`() {
        mockMvc.perform(
            get("/api/v1/payment/modes")
                .header("productType", "PRODUCT_A")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `getPaymentModes should return 400 when productType is missing`() {
        mockMvc.perform(
            get("/api/v1/payment/modes")
                .header("userId", "user123")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
}
```

## 9. Execution Plan

Follow these steps to implement the Payment Modes API:

1. Create the database migration script and run it
2. Generate jOOQ classes from the database schema
3. Create the domain model and mappers
4. Implement the repository layer
5. Implement the service layer
6. Update the OpenAPI specification
7. Generate controller interfaces and models from OpenAPI
8. Implement the controller and error handling
9. Write unit and integration tests
10. Test the API end-to-end

## 10. Deliverables

- Database migration script for payment_modes table
- Domain model: PaymentMode
- Mappers: PaymentModeMapper, PaymentModesApiMapper
- Repository: PaymentModesRepository, PaymentModesRepositoryImpl
- Service: PaymentModesService, PaymentModesServiceImpl
- OpenAPI specification for the Payment Modes API
- Controller: PaymentModesControllerImpl
- Exception handling: GlobalExceptionHandler
- Tests: Unit tests and integration tests
