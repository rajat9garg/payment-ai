# Task 2: Payment Initiation API Implementation Plan - Part 4

## 8. OpenAPI Specification

### 8.1 Update OpenAPI Specification

Update the OpenAPI specification in `src/main/resources/openapi/api.yaml` to include the payment initiation endpoint:

```yaml
# Add this to the existing OpenAPI specification
paths:
  /payment/initiate:
    post:
      summary: Initiate a payment transaction
      description: Initiates a payment transaction with the specified payment details
      operationId: initiatePayment
      tags:
        - Payment
      parameters:
        - name: userId
          in: header
          required: true
          schema:
            type: string
          description: ID of the user making the payment
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentInitiateRequest'
      responses:
        '200':
          description: Payment initiated successfully
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
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

# Add these schemas to the components/schemas section
components:
  schemas:
    PaymentInitiateRequest:
      type: object
      required:
        - amount
        - paymentMode
        - paymentType
      properties:
        amount:
          type: number
          format: decimal
          example: 100.00
          description: Payment amount
        paymentMode:
          type: string
          example: "UPI"
          description: Payment mode (UPI, CREDIT_CARD, DEBIT_CARD)
        paymentType:
          type: string
          example: "GOOGLE_PAY"
          description: Payment type (GOOGLE_PAY, CRED, VISA, MASTERCARD)
        currency:
          type: string
          default: "INR"
          example: "INR"
          description: Currency code
        metadata:
          type: object
          additionalProperties: true
          description: Additional metadata for the payment
          example:
            orderId: "ORDER123"
            productId: "PROD456"
    
    PaymentResponse:
      type: object
      properties:
        paymentId:
          type: string
          example: "PAY123456"
          description: Unique payment ID
        status:
          type: string
          example: "PENDING"
          description: Payment status (PENDING, SUCCESS, FAILED)
        amount:
          type: number
          format: decimal
          example: 100.00
          description: Payment amount
        paymentMode:
          type: string
          example: "UPI"
          description: Payment mode used
        paymentType:
          type: string
          example: "GOOGLE_PAY"
          description: Payment type used
        timestamp:
          type: string
          format: date-time
          example: "2025-05-31T12:34:56Z"
          description: Timestamp of the payment
```

## 9. Controller Layer Implementation

### 9.1 Generate OpenAPI Models and Interfaces

Run the OpenAPI generator to create the controller interfaces and models:

```bash
./gradlew openApiGenerate
```

This will generate:
- Controller interfaces in `build/generated/src/main/kotlin/com/payment/api`
- API models in `build/generated/src/main/kotlin/com/payment/model`

### 9.2 Create API Model Mappers

Create mappers to convert between domain models and API models:

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentApiMapper.kt
package com.payment.mappers

import com.payment.model.PaymentInitiateRequest as ApiPaymentInitiateRequest
import com.payment.model.PaymentResponse as ApiPaymentResponse
import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class PaymentApiMapper {
    
    /**
     * Convert API payment request to domain payment request
     */
    fun toDomainRequest(apiRequest: ApiPaymentInitiateRequest): PaymentRequest {
        return PaymentRequest(
            amount = apiRequest.amount,
            paymentMode = apiRequest.paymentMode,
            paymentType = apiRequest.paymentType,
            currency = apiRequest.currency ?: "INR",
            metadata = apiRequest.metadata ?: emptyMap()
        )
    }
    
    /**
     * Convert domain payment response to API payment response
     */
    fun toApiResponse(domainResponse: PaymentResponse): ApiPaymentResponse {
        return ApiPaymentResponse(
            paymentId = domainResponse.paymentId,
            status = domainResponse.status,
            amount = domainResponse.amount,
            paymentMode = domainResponse.paymentMode,
            paymentType = domainResponse.paymentType,
            timestamp = OffsetDateTime.of(
                domainResponse.timestamp,
                ZoneOffset.UTC
            )
        )
    }
}
```

### 9.3 Implement Controller

Implement the controller interface generated from OpenAPI:

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

    override fun initiatePayment(userId: String, apiPaymentInitiateRequest: ApiPaymentInitiateRequest): ResponseEntity<ApiPaymentResponse> {
        // Validate input parameters
        if (userId.isBlank()) {
            throw IllegalArgumentException("userId cannot be blank")
        }
        
        // Convert API request to domain request
        val domainRequest = paymentApiMapper.toDomainRequest(apiPaymentInitiateRequest)
        
        // Call service to initiate payment
        val domainResponse = paymentService.initiatePayment(userId, domainRequest)
        
        // Convert domain response to API response
        val apiResponse = paymentApiMapper.toApiResponse(domainResponse)
        
        // Return response
        return ResponseEntity.ok(apiResponse)
    }
}
```

## 10. Testing

### 10.1 Unit Testing

Create unit tests for each layer:

```kotlin
// src/test/kotlin/com/payment/services/PaymentServiceImplTest.kt
package com.payment.services

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse
import com.payment.models.domain.Transaction
import com.payment.providers.PaymentProvider
import com.payment.providers.PaymentProviderFactory
import com.payment.repositories.TransactionRepository
import com.payment.services.impl.PaymentServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentServiceImplTest {

    private val idempotencyKeyService: IdempotencyKeyService = mockk()
    private val paymentProviderFactory: PaymentProviderFactory = mockk()
    private val paymentProvider: PaymentProvider = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    
    private val paymentService = PaymentServiceImpl(
        idempotencyKeyService,
        paymentProviderFactory,
        transactionRepository
    )
    
    @Test
    fun `initiatePayment should process payment and save transaction`() {
        // Given
        val userId = "user123"
        val idempotencyKey = "PAY123456"
        val request = PaymentRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        val paymentResponse = PaymentResponse(
            paymentId = idempotencyKey,
            status = "PENDING",
            amount = request.amount,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            timestamp = LocalDateTime.now()
        )
        
        // Mock dependencies
        every { idempotencyKeyService.generateIdempotencyKey() } returns idempotencyKey
        every { paymentProviderFactory.getProvider(request.paymentMode, request.paymentType) } returns paymentProvider
        every { paymentProvider.getName() } returns "UPI_PROVIDER"
        every { paymentProvider.processPayment(request, idempotencyKey) } returns paymentResponse
        
        val transactionSlot = slot<Transaction>()
        every { transactionRepository.save(capture(transactionSlot)) } answers {
            transactionSlot.captured
        }
        
        // When
        val result = paymentService.initiatePayment(userId, request)
        
        // Then
        assertEquals(idempotencyKey, result.paymentId)
        assertEquals("PENDING", result.status)
        assertEquals(request.amount, result.amount)
        
        // Verify transaction was saved
        verify(exactly = 1) { transactionRepository.save(any()) }
        assertEquals(idempotencyKey, transactionSlot.captured.idempotencyKey)
        assertEquals("PENDING", transactionSlot.captured.status)
        assertEquals(userId, transactionSlot.captured.userId)
        assertEquals(request.amount, transactionSlot.captured.amount)
        assertEquals(request.paymentMode, transactionSlot.captured.paymentMode)
    }
}
```

### 10.2 Integration Testing

Create integration tests for the API:

```kotlin
// src/test/kotlin/com/payment/controllers/PaymentControllerIntegrationTest.kt
package com.payment.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.payment.model.PaymentInitiateRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `initiatePayment should return payment response`() {
        // Given
        val request = PaymentInitiateRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            currency = "INR",
            metadata = mapOf("orderId" to "ORDER123")
        )
        
        // When/Then
        mockMvc.perform(
            post("/api/v1/payment/initiate")
                .header("userId", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paymentId").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.amount").value(100.00))
            .andExpect(jsonPath("$.paymentMode").value("UPI"))
            .andExpect(jsonPath("$.paymentType").value("GOOGLE_PAY"))
    }
    
    @Test
    fun `initiatePayment should return 400 when userId is missing`() {
        // Given
        val request = PaymentInitiateRequest(
            amount = BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY"
        )
        
        // When/Then
        mockMvc.perform(
            post("/api/v1/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
```

## 11. Concurrency and Idempotency Implementation

### 11.1 Update OpenAPI Specification for Idempotency

Update the OpenAPI specification to include the idempotency key header:

```yaml
paths:
  /payment/initiate:
    post:
      # Existing content...
      parameters:
        - name: userId
          in: header
          required: true
          schema:
            type: string
          description: ID of the user making the payment
        - name: Idempotency-Key
          in: header
          required: true
          schema:
            type: string
            format: uuid
          description: Client-generated unique key for idempotency
      # Rest of the existing content...
```

### 11.2 Database Constraints for Idempotency

Add a unique constraint to the transactions table in the migration script:

```sql
-- Add to the existing migration script
ALTER TABLE transactions ADD CONSTRAINT unique_idempotency_key UNIQUE (idempotency_key);
```

### 11.3 Update Transaction Model for Optimistic Locking

Add a version field to the Transaction model for optimistic concurrency control:

```kotlin
data class Transaction(
    val id: Long? = null,
    val idempotencyKey: String,
    var status: TransactionStatus,
    val userId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMode: String,
    val paymentType: String,
    val paymentProvider: String,
    var vendorTransactionId: String? = null,
    val metadata: Map<String, Any>,
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime,
    var version: Long = 0  // Version field for optimistic locking
)
```

### 11.4 Implement Redis-based Distributed Locking

Create a Redis lock service for distributed locking:

```kotlin
// src/main/kotlin/com/payment/services/RedisLockService.kt
package com.payment.services

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisLockService(private val redisTemplate: RedisTemplate<String, String>) {
    private val logger = LoggerFactory.getLogger(RedisLockService::class.java)
    private val lockTimeout = 10000L // 10 seconds
    
    /**
     * Acquire a distributed lock
     * @param lockKey Key to lock on
     * @return true if lock was acquired, false otherwise
     */
    fun acquireLock(lockKey: String): Boolean {
        val lockKeyWithPrefix = "lock:$lockKey"
        val success = redisTemplate.opsForValue()
            .setIfAbsent(lockKeyWithPrefix, "locked", Duration.ofMillis(lockTimeout))
        
        logger.info("Acquiring lock for key: $lockKey, success: $success")
        return success ?: false
    }
    
    /**
     * Release a distributed lock
     * @param lockKey Key to release
     */
    fun releaseLock(lockKey: String) {
        val lockKeyWithPrefix = "lock:$lockKey"
        redisTemplate.delete(lockKeyWithPrefix)
        logger.info("Released lock for key: $lockKey")
    }
}
```

### 11.5 Update PaymentService Interface

Update the PaymentService interface to accept client-provided idempotency keys:

```kotlin
interface PaymentService {
    /**
     * Initiate a payment transaction
     * @param userId ID of the user making the payment
     * @param idempotencyKey Client-provided idempotency key
     * @param request Payment request details
     * @return Payment response with transaction details
     */
    fun initiatePayment(userId: String, idempotencyKey: String, request: PaymentRequest): PaymentResponse
    
    // Other existing methods...
}
```

### 11.6 Update PaymentServiceImpl for Idempotency and Concurrency

Modify the PaymentServiceImpl to handle idempotency and concurrency:

```kotlin
@Service
class PaymentServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val paymentProviderFactory: PaymentProviderFactory,
    private val redisLockService: RedisLockService
) : PaymentService {

    private val logger = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    @Transactional
    override fun initiatePayment(userId: String, idempotencyKey: String, request: PaymentRequest): PaymentResponse {
        // Try to acquire a distributed lock
        if (!redisLockService.acquireLock(idempotencyKey)) {
            // Another instance is processing this request
            // Wait and check if the transaction exists
            Thread.sleep(500)
            val existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
            if (existingTransaction != null) {
                return createResponseFromTransaction(existingTransaction)
            }
            
            // If still no transaction, throw an exception
            throw ConcurrentModificationException("Another request with the same idempotency key is being processed")
        }
        
        try {
            // Check if payment mode and type are supported
            val paymentProvider = paymentProviderFactory.getProvider(request.paymentMode, request.paymentType)
                ?: throw IllegalArgumentException("Unsupported payment mode or type: ${request.paymentMode}/${request.paymentType}")
    
            // Check if this idempotency key has been used before
            val existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
            if (existingTransaction != null) {
                logger.info("Found existing transaction for idempotency key: $idempotencyKey")
                return createResponseFromTransaction(existingTransaction)
            }
    
            // Process payment with provider
            val providerResponse = paymentProvider.processPayment(request)
            
            // Create transaction record
            val transaction = Transaction(
                idempotencyKey = idempotencyKey,
                status = providerResponse.status,
                userId = userId,
                amount = request.amount,
                currency = request.currency,
                paymentMode = request.paymentMode,
                paymentType = request.paymentType,
                paymentProvider = paymentProvider.getName(),
                vendorTransactionId = providerResponse.vendorTransactionId,
                metadata = request.metadata,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC),
                updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            
            val savedTransaction = transactionRepository.save(transaction)
            logger.info("Saved transaction with ID: ${savedTransaction.id} and idempotency key: $idempotencyKey")
            
            return createResponseFromTransaction(savedTransaction)
        } finally {
            // Always release the lock
            redisLockService.releaseLock(idempotencyKey)
        }
    }
    
    private fun createResponseFromTransaction(transaction: Transaction): PaymentResponse {
        return PaymentResponse(
            paymentId = transaction.idempotencyKey,
            status = transaction.status,
            amount = transaction.amount,
            currency = transaction.currency,
            paymentMode = transaction.paymentMode,
            paymentType = transaction.paymentType,
            vendorTransactionId = transaction.vendorTransactionId,
            metadata = transaction.metadata
        )
    }
    
    // Other existing methods...
}
```

### 11.7 Update TransactionRepository Interface

Add methods for pessimistic locking in the repository interface:

```kotlin
interface TransactionRepository {
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdempotencyKeyForUpdate(idempotencyKey: String): Transaction?
    
    fun save(transaction: Transaction): Transaction
    
    // Other existing methods...
}
```

### 11.8 Update Controller Implementation

Update the PaymentController to handle idempotency keys:

```kotlin
@RestController
class PaymentControllerImpl(
    private val paymentService: PaymentService,
    private val paymentApiMapper: PaymentApiMapper
) : PaymentApi {

    @Override
    fun initiatePayment(
        @RequestHeader(value = "userId", required = true) userId: String,
        @RequestHeader(value = "Idempotency-Key", required = true) idempotencyKey: String,
        @Valid @RequestBody paymentInitiateRequest: PaymentInitiateRequest
    ): ResponseEntity<PaymentResponse> {
        // Convert API request to domain request
        val domainRequest = paymentApiMapper.toDomainRequest(paymentInitiateRequest)
        
        // Call service to initiate payment with idempotency key
        val serviceResponse = paymentService.initiatePayment(userId, idempotencyKey, domainRequest)
        
        // Convert to API response
        val apiResponse = paymentApiMapper.toApiResponse(serviceResponse)
        
        // Return response
        return ResponseEntity.ok(apiResponse)
    }
    
    // Other existing methods...
}
```

### 11.9 Testing Concurrency and Idempotency

Add test cases to verify concurrency and idempotency behavior:

```kotlin
@Test
fun `initiatePayment should return same response for duplicate idempotency keys`() {
    // Given
    val userId = "user123"
    val idempotencyKey = "key123"
    val paymentRequest = PaymentRequest(
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        paymentType = "GOOGLE_PAY"
    )
    
    val existingTransaction = Transaction(
        id = 1L,
        idempotencyKey = idempotencyKey,
        status = TransactionStatus.PENDING,
        userId = userId,
        amount = paymentRequest.amount,
        currency = "INR",
        paymentMode = paymentRequest.paymentMode,
        paymentType = paymentRequest.paymentType,
        paymentProvider = "UPI_PROVIDER",
        vendorTransactionId = "vendor123",
        metadata = emptyMap(),
        createdAt = OffsetDateTime.now(ZoneOffset.UTC),
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    )
    
    // When
    every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns existingTransaction
    every { redisLockService.acquireLock(idempotencyKey) } returns true
    every { redisLockService.releaseLock(idempotencyKey) } just Runs
    
    val response = paymentService.initiatePayment(userId, idempotencyKey, paymentRequest)
    
    // Then
    verify(exactly = 1) { transactionRepository.findByIdempotencyKey(idempotencyKey) }
    verify(exactly = 0) { paymentProviderFactory.getProvider(any(), any()) }
    verify(exactly = 0) { transactionRepository.save(any()) }
    
    assertEquals(idempotencyKey, response.paymentId)
    assertEquals(TransactionStatus.PENDING, response.status)
}

@Test
fun `initiatePayment should handle concurrent requests with same idempotency key`() {
    // Given
    val userId = "user123"
    val idempotencyKey = "key123"
    val paymentRequest = PaymentRequest(
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        paymentType = "GOOGLE_PAY"
    )
    
    // When
    every { redisLockService.acquireLock(idempotencyKey) } returns false
    every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
    
    // Then
    assertThrows<ConcurrentModificationException> {
        paymentService.initiatePayment(userId, idempotencyKey, paymentRequest)
    }
}
```

## 12. Best Practices for Idempotency and Concurrency

1. **Use UUIDs for idempotency keys**: Recommend clients use UUIDs for idempotency keys to avoid collisions.
2. **Set expiration for idempotency keys**: Consider expiring idempotency keys after a reasonable period (e.g., 24 hours).
3. **Document idempotency behavior**: Clearly document how idempotency works in your API documentation.
4. **Handle edge cases**: Consider what happens if a request times out but was actually processed.
5. **Implement monitoring**: Monitor for duplicate requests and lock acquisition failures.
6. **Use database constraints**: Ensure database-level constraints for idempotency keys.
7. **Implement both optimistic and pessimistic locking**: Use optimistic locking for normal operations and pessimistic locking for critical sections.
8. **Use distributed locks for multi-instance deployments**: Redis-based locks help coordinate across multiple service instances.
