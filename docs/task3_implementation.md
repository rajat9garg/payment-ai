# Task 3: Payment Status API Implementation Plan

## 1. Overview

This document provides a detailed implementation plan for the Payment Status API, which allows clients to check the status of a payment transaction.

### API Endpoint
- **GET /v1/payment/status**
- **Headers**: userId
- **Query Parameters**: paymentId
- **Response**: Payment status details

### Implementation Approach
Following the OpenAPI-first approach and the established project rules, we'll implement this API by extending the existing payment service and creating a new endpoint.

## 2. Service Layer Implementation

### 2.1 Update Payment Service Interface

The `getPaymentStatus` method has already been defined in the Payment Service interface in Task 2:

```kotlin
// src/main/kotlin/com/payment/services/PaymentService.kt
interface PaymentService {
    // ... existing methods
    
    /**
     * Get the status of a payment
     * 
     * @param userId The ID of the user who made the payment
     * @param paymentId The ID of the payment
     * @return The payment response with current status
     */
    fun getPaymentStatus(userId: String, paymentId: String): PaymentResponse
}
```

### 2.2 Implement Payment Status Service Logic

The implementation of `getPaymentStatus` was also included in Task 2's PaymentServiceImpl. Let's review and ensure it meets our requirements:

```kotlin
// src/main/kotlin/com/payment/services/impl/PaymentServiceImpl.kt
override fun getPaymentStatus(userId: String, paymentId: String): PaymentResponse {
    // Find the transaction in the database
    val transaction = transactionRepository.findByIdempotencyKey(paymentId)
        ?: throw IllegalArgumentException("Payment not found with ID: $paymentId")
    
    // Verify the user ID matches
    if (transaction.userId != userId) {
        throw IllegalArgumentException("Payment $paymentId does not belong to user $userId")
    }
    
    // Return the payment response
    return PaymentResponse(
        paymentId = transaction.idempotencyKey,
        status = transaction.status,
        amount = transaction.amount,
        paymentMode = transaction.paymentMode,
        paymentType = "", // We don't store payment type in the transaction
        timestamp = transaction.updatedAt ?: LocalDateTime.now()
    )
}
```

## 3. OpenAPI Specification

### 3.1 Update OpenAPI Specification

Update the OpenAPI specification in `src/main/resources/openapi/api.yaml` to include the payment status endpoint:

```yaml
# Add this to the existing OpenAPI specification
paths:
  /payment/status:
    get:
      summary: Get payment status
      description: Returns the status of a payment transaction
      operationId: getPaymentStatus
      tags:
        - Payment
      parameters:
        - name: userId
          in: header
          required: true
          schema:
            type: string
          description: ID of the user who made the payment
        - name: paymentId
          in: query
          required: true
          schema:
            type: string
          description: ID of the payment
      responses:
        '200':
          description: Payment status retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentResponse'
        '400':
          description: Bad request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: Payment not found
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
```

## 4. Controller Layer Implementation

### 4.1 Generate OpenAPI Models and Interfaces

Run the OpenAPI generator to create the controller interfaces and models:

```bash
./gradlew openApiGenerate
```

This will update:
- Controller interfaces in `build/generated/src/main/kotlin/com/payment/api`
- API models in `build/generated/src/main/kotlin/com/payment/model`

### 4.2 Update Controller Implementation

Update the controller implementation to include the payment status endpoint:

```kotlin
// src/main/kotlin/com/payment/controllers/PaymentControllerImpl.kt
package com.payment.controllers

import com.payment.api.PaymentApi
import com.payment.mappers.PaymentApiMapper
import com.payment.model.PaymentInitiateRequest as ApiPaymentInitiateRequest
import com.payment.model.PaymentResponse as ApiPaymentResponse
import com.payment.services.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentControllerImpl(
    private val paymentService: PaymentService,
    private val paymentApiMapper: PaymentApiMapper
) : PaymentApi {

    // ... existing methods
    
    override fun getPaymentStatus(userId: String, paymentId: String): ResponseEntity<ApiPaymentResponse> {
        // Validate input parameters
        if (userId.isBlank()) {
            throw IllegalArgumentException("userId cannot be blank")
        }
        
        if (paymentId.isBlank()) {
            throw IllegalArgumentException("paymentId cannot be blank")
        }
        
        // Call service to get payment status
        val domainResponse = paymentService.getPaymentStatus(userId, paymentId)
        
        // Convert domain response to API response
        val apiResponse = paymentApiMapper.toApiResponse(domainResponse)
        
        // Return response
        return ResponseEntity.ok(apiResponse)
    }
}
```

### 4.3 Update Exception Handling

Add handling for the 404 Not Found scenario:

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

    // ... existing methods
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = ex.message ?: "Resource not found",
            timestamp = OffsetDateTime.now().toString()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
}
```

## 5. Testing

### 5.1 Unit Testing

Create unit tests for the payment status service:

```kotlin
// src/test/kotlin/com/payment/services/PaymentServiceImplTest.kt
// Add this test method to the existing test class

@Test
fun `getPaymentStatus should return payment status for valid payment ID`() {
    // Given
    val userId = "user123"
    val paymentId = "PAY123456"
    
    val transaction = Transaction(
        id = 1L,
        idempotencyKey = paymentId,
        status = "SUCCESS",
        userId = userId,
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        vendorTransactionId = "VENDOR123",
        paymentProvider = "UPI_PROVIDER",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    // Mock dependencies
    every { transactionRepository.findByIdempotencyKey(paymentId) } returns transaction
    
    // When
    val result = paymentService.getPaymentStatus(userId, paymentId)
    
    // Then
    assertEquals(paymentId, result.paymentId)
    assertEquals("SUCCESS", result.status)
    assertEquals(BigDecimal("100.00"), result.amount)
    assertEquals("UPI", result.paymentMode)
}

@Test
fun `getPaymentStatus should throw exception when payment ID not found`() {
    // Given
    val userId = "user123"
    val paymentId = "INVALID_ID"
    
    // Mock dependencies
    every { transactionRepository.findByIdempotencyKey(paymentId) } returns null
    
    // When/Then
    assertThrows<IllegalArgumentException> {
        paymentService.getPaymentStatus(userId, paymentId)
    }
}

@Test
fun `getPaymentStatus should throw exception when user ID doesn't match`() {
    // Given
    val userId = "user123"
    val wrongUserId = "user456"
    val paymentId = "PAY123456"
    
    val transaction = Transaction(
        id = 1L,
        idempotencyKey = paymentId,
        status = "SUCCESS",
        userId = userId,
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        vendorTransactionId = "VENDOR123",
        paymentProvider = "UPI_PROVIDER",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    // Mock dependencies
    every { transactionRepository.findByIdempotencyKey(paymentId) } returns transaction
    
    // When/Then
    assertThrows<IllegalArgumentException> {
        paymentService.getPaymentStatus(wrongUserId, paymentId)
    }
}
```

### 5.2 Integration Testing

Create integration tests for the payment status API:

```kotlin
// src/test/kotlin/com/payment/controllers/PaymentControllerIntegrationTest.kt
// Add these test methods to the existing test class

@Test
fun `getPaymentStatus should return payment status`() {
    // Given
    val userId = "user123"
    val paymentId = "PAY123456"
    
    // Assuming a payment with this ID exists in the test database
    
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/status")
            .header("userId", userId)
            .param("paymentId", paymentId)
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.paymentId").value(paymentId))
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.amount").exists())
        .andExpect(jsonPath("$.paymentMode").exists())
}

@Test
fun `getPaymentStatus should return 400 when userId is missing`() {
    // Given
    val paymentId = "PAY123456"
    
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/status")
            .param("paymentId", paymentId)
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isBadRequest)
}

@Test
fun `getPaymentStatus should return 400 when paymentId is missing`() {
    // Given
    val userId = "user123"
    
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/status")
            .header("userId", userId)
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isBadRequest)
}

@Test
fun `getPaymentStatus should return 404 when payment is not found`() {
    // Given
    val userId = "user123"
    val paymentId = "NONEXISTENT_ID"
    
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/status")
            .header("userId", userId)
            .param("paymentId", paymentId)
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isNotFound)
}
```

## 6. Execution Plan

Follow these steps to implement the Payment Status API:

1. Update the OpenAPI specification with the payment status endpoint
2. Generate controller interfaces and models from OpenAPI
3. Implement the controller method for getting payment status
4. Update exception handling for 404 scenarios
5. Write unit and integration tests
6. Test the API end-to-end

## 7. Deliverables

- Updated OpenAPI specification for the Payment Status API
- Updated controller implementation with getPaymentStatus method
- Updated exception handling for 404 scenarios
- Tests: Unit tests and integration tests
