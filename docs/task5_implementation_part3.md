# Task 5: Payment Modes API Implementation Plan - Part 3

## 6. OpenAPI Specification

### 6.1 Update OpenAPI Specification

Update the OpenAPI specification in `src/main/resources/openapi/api.yaml` to include the payment modes endpoint:

```yaml
# Add this to the existing OpenAPI specification
paths:
  /payment/modes:
    get:
      summary: Get available payment modes
      description: Returns the available payment modes for a user and product type
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
          in: query
          required: true
          schema:
            type: string
          description: Type of product (e.g., MOBILE, ELECTRONICS)
      responses:
        '200':
          description: Payment modes retrieved successfully
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

# Add these schemas to the components/schemas section
components:
  schemas:
    PaymentModesResponse:
      type: object
      properties:
        paymentModes:
          type: array
          items:
            $ref: '#/components/schemas/PaymentMode'
    
    PaymentMode:
      type: object
      properties:
        code:
          type: string
          example: "UPI"
          description: Payment mode code
        name:
          type: string
          example: "Unified Payment Interface"
          description: Payment mode name
        description:
          type: string
          example: "Direct bank transfer using UPI"
          description: Payment mode description
        isPreferred:
          type: boolean
          example: false
          description: Whether this payment mode is preferred by the user
        types:
          type: array
          items:
            $ref: '#/components/schemas/PaymentType'
    
    PaymentType:
      type: object
      properties:
        code:
          type: string
          example: "GOOGLE_PAY"
          description: Payment type code
        name:
          type: string
          example: "Google Pay"
          description: Payment type name
        description:
          type: string
          example: "Pay using Google Pay UPI"
          description: Payment type description
        isPreferred:
          type: boolean
          example: false
          description: Whether this payment type is preferred by the user
```

## 7. Controller Layer Implementation

### 7.1 Generate OpenAPI Models and Interfaces

Run the OpenAPI generator to create the controller interfaces and models:

```bash
./gradlew openApiGenerate
```

This will update:
- Controller interfaces in `build/generated/src/main/kotlin/com/payment/api`
- API models in `build/generated/src/main/kotlin/com/payment/model`

### 7.2 Create API Model Mappers

Create mappers to convert between domain models and API models:

```kotlin
// src/main/kotlin/com/payment/mappers/PaymentModeApiMapper.kt
package com.payment.mappers

import com.payment.model.PaymentMode as ApiPaymentMode
import com.payment.model.PaymentType as ApiPaymentType
import com.payment.model.PaymentModesResponse
import org.springframework.stereotype.Component

@Component
class PaymentModeApiMapper {
    
    /**
     * Convert domain payment modes map to API payment modes response
     */
    fun toApiResponse(domainResponse: Map<String, Any>): PaymentModesResponse {
        val paymentModes = domainResponse["paymentModes"] as List<Map<String, Any>>
        
        val apiPaymentModes = paymentModes.map { mode ->
            val types = mode["types"] as List<Map<String, Any>>
            
            val apiTypes = types.map { type ->
                ApiPaymentType(
                    code = type["code"] as String,
                    name = type["name"] as String,
                    description = type["description"] as String,
                    isPreferred = type["isPreferred"] as Boolean
                )
            }
            
            ApiPaymentMode(
                code = mode["code"] as String,
                name = mode["name"] as String,
                description = mode["description"] as String,
                isPreferred = mode["isPreferred"] as Boolean,
                types = apiTypes
            )
        }
        
        return PaymentModesResponse(paymentModes = apiPaymentModes)
    }
}
```

### 7.3 Implement Controller

Implement the controller interface generated from OpenAPI:

```kotlin
// src/main/kotlin/com/payment/controllers/PaymentControllerImpl.kt
// Add this method to the existing PaymentControllerImpl class

@Autowired
private lateinit var paymentModesService: PaymentModesService

@Autowired
private lateinit var paymentModeApiMapper: PaymentModeApiMapper

override fun getPaymentModes(userId: String, productType: String): ResponseEntity<PaymentModesResponse> {
    // Validate input parameters
    if (userId.isBlank()) {
        throw IllegalArgumentException("userId cannot be blank")
    }
    
    if (productType.isBlank()) {
        throw IllegalArgumentException("productType cannot be blank")
    }
    
    // Call service to get available payment modes
    val domainResponse = paymentModesService.getAvailablePaymentModes(userId, productType)
    
    // Convert domain response to API response
    val apiResponse = paymentModeApiMapper.toApiResponse(domainResponse)
    
    // Return response
    return ResponseEntity.ok(apiResponse)
}
```

## 8. Testing

### 8.1 Unit Testing

Create unit tests for the payment modes service:

```kotlin
// src/test/kotlin/com/payment/services/PaymentModesServiceImplTest.kt
package com.payment.services

import com.payment.models.domain.PaymentMode
import com.payment.models.domain.PaymentType
import com.payment.models.domain.ProductPaymentMode
import com.payment.models.domain.UserPaymentPreference
import com.payment.repositories.PaymentModeRepository
import com.payment.repositories.PaymentTypeRepository
import com.payment.repositories.ProductPaymentModeRepository
import com.payment.repositories.UserPaymentPreferenceRepository
import com.payment.services.impl.PaymentModesServiceImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PaymentModesServiceImplTest {

    private val paymentModeRepository: PaymentModeRepository = mockk()
    private val paymentTypeRepository: PaymentTypeRepository = mockk()
    private val productPaymentModeRepository: ProductPaymentModeRepository = mockk()
    private val userPaymentPreferenceRepository: UserPaymentPreferenceRepository = mockk()
    
    private val paymentModesService = PaymentModesServiceImpl(
        paymentModeRepository,
        paymentTypeRepository,
        productPaymentModeRepository,
        userPaymentPreferenceRepository
    )
    
    @Test
    fun `getAvailablePaymentModes should return payment modes for product type`() {
        // Given
        val userId = "user123"
        val productType = "MOBILE"
        
        val paymentMode1 = PaymentMode(
            id = 1L,
            modeCode = "UPI",
            modeName = "Unified Payment Interface",
            description = "Direct bank transfer using UPI",
            isActive = true
        )
        
        val paymentMode2 = PaymentMode(
            id = 2L,
            modeCode = "CREDIT_CARD",
            modeName = "Credit Card",
            description = "Pay using credit card",
            isActive = true
        )
        
        val paymentType1 = PaymentType(
            id = 1L,
            modeId = 1L,
            typeCode = "GOOGLE_PAY",
            typeName = "Google Pay",
            description = "Pay using Google Pay UPI",
            isActive = true
        )
        
        val paymentType2 = PaymentType(
            id = 2L,
            modeId = 1L,
            typeCode = "CRED",
            typeName = "CRED",
            description = "Pay using CRED UPI",
            isActive = true
        )
        
        val paymentType3 = PaymentType(
            id = 3L,
            modeId = 2L,
            typeCode = "VISA",
            typeName = "Visa",
            description = "Pay using Visa credit card",
            isActive = true
        )
        
        val productPaymentModes = listOf(
            ProductPaymentMode(
                id = 1L,
                productType = productType,
                modeId = 1L,
                typeId = 1L,
                isActive = true
            ),
            ProductPaymentMode(
                id = 2L,
                productType = productType,
                modeId = 1L,
                typeId = 2L,
                isActive = true
            ),
            ProductPaymentMode(
                id = 3L,
                productType = productType,
                modeId = 2L,
                typeId = 3L,
                isActive = true
            )
        )
        
        val userPreferences = listOf(
            UserPaymentPreference(
                id = 1L,
                userId = userId,
                modeId = 1L,
                typeId = null,
                isPreferred = true,
                isBlocked = false
            )
        )
        
        // Mock dependencies
        every { productPaymentModeRepository.findByProductType(productType) } returns productPaymentModes
        every { paymentModeRepository.findById(1L) } returns paymentMode1
        every { paymentModeRepository.findById(2L) } returns paymentMode2
        every { paymentTypeRepository.findByModeId(1L) } returns listOf(paymentType1, paymentType2)
        every { paymentTypeRepository.findByModeId(2L) } returns listOf(paymentType3)
        every { userPaymentPreferenceRepository.findByUserId(userId) } returns userPreferences
        every { userPaymentPreferenceRepository.findBlockedByUserId(userId) } returns emptyList()
        every { userPaymentPreferenceRepository.findPreferredByUserId(userId) } returns userPreferences
        
        // When
        val result = paymentModesService.getAvailablePaymentModes(userId, productType)
        
        // Then
        assertTrue(result.containsKey("paymentModes"))
        val paymentModes = result["paymentModes"] as List<Map<String, Any>>
        assertEquals(2, paymentModes.size)
        
        // First payment mode should be UPI (preferred)
        val firstMode = paymentModes[0]
        assertEquals("UPI", firstMode["code"])
        assertEquals(true, firstMode["isPreferred"])
        
        // Second payment mode should be CREDIT_CARD (not preferred)
        val secondMode = paymentModes[1]
        assertEquals("CREDIT_CARD", secondMode["code"])
        assertEquals(false, secondMode["isPreferred"])
        
        // Check types
        val firstModeTypes = firstMode["types"] as List<Map<String, Any>>
        assertEquals(2, firstModeTypes.size)
        assertEquals("GOOGLE_PAY", firstModeTypes[0]["code"])
        assertEquals("CRED", firstModeTypes[1]["code"])
    }
    
    @Test
    fun `getAvailablePaymentModes should filter out blocked payment modes`() {
        // Given
        val userId = "user123"
        val productType = "MOBILE"
        
        val paymentMode1 = PaymentMode(
            id = 1L,
            modeCode = "UPI",
            modeName = "Unified Payment Interface",
            description = "Direct bank transfer using UPI",
            isActive = true
        )
        
        val paymentMode2 = PaymentMode(
            id = 2L,
            modeCode = "CREDIT_CARD",
            modeName = "Credit Card",
            description = "Pay using credit card",
            isActive = true
        )
        
        val productPaymentModes = listOf(
            ProductPaymentMode(
                id = 1L,
                productType = productType,
                modeId = 1L,
                typeId = null,
                isActive = true
            ),
            ProductPaymentMode(
                id = 2L,
                productType = productType,
                modeId = 2L,
                typeId = null,
                isActive = true
            )
        )
        
        val blockedPreferences = listOf(
            UserPaymentPreference(
                id = 1L,
                userId = userId,
                modeId = 2L,
                typeId = null,
                isPreferred = false,
                isBlocked = true
            )
        )
        
        // Mock dependencies
        every { productPaymentModeRepository.findByProductType(productType) } returns productPaymentModes
        every { paymentModeRepository.findById(1L) } returns paymentMode1
        every { paymentModeRepository.findById(2L) } returns paymentMode2
        every { paymentTypeRepository.findByModeId(1L) } returns emptyList()
        every { paymentTypeRepository.findByModeId(2L) } returns emptyList()
        every { userPaymentPreferenceRepository.findByUserId(userId) } returns blockedPreferences
        every { userPaymentPreferenceRepository.findBlockedByUserId(userId) } returns blockedPreferences
        every { userPaymentPreferenceRepository.findPreferredByUserId(userId) } returns emptyList()
        
        // When
        val result = paymentModesService.getAvailablePaymentModes(userId, productType)
        
        // Then
        val paymentModes = result["paymentModes"] as List<Map<String, Any>>
        assertEquals(1, paymentModes.size)
        assertEquals("UPI", paymentModes[0]["code"])
    }
}
```

### 8.2 Integration Testing

Create integration tests for the payment modes API:

```kotlin
// src/test/kotlin/com/payment/controllers/PaymentControllerIntegrationTest.kt
// Add these test methods to the existing test class

@Test
fun `getPaymentModes should return payment modes`() {
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/modes")
            .header("userId", "user123")
            .param("productType", "MOBILE")
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.paymentModes").isArray)
}

@Test
fun `getPaymentModes should return 400 when userId is missing`() {
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/modes")
            .param("productType", "MOBILE")
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isBadRequest)
}

@Test
fun `getPaymentModes should return 400 when productType is missing`() {
    // When/Then
    mockMvc.perform(
        get("/api/v1/payment/modes")
            .header("userId", "user123")
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isBadRequest)
}
```

## 9. Execution Plan

Follow these steps to implement the Payment Modes API:

1. Create the database migration script for payment modes tables
2. Run the migration and generate jOOQ classes
3. Create domain models for payment modes
4. Create mappers for payment modes
5. Implement the repository layer for payment modes
6. Implement the payment modes service
7. Update the OpenAPI specification with the payment modes endpoint
8. Generate controller interfaces and models from OpenAPI
9. Create API model mappers for payment modes
10. Implement the controller method for getting payment modes
11. Write unit and integration tests
12. Test the API end-to-end

## 10. Deliverables

- Database migration script for payment modes tables
- Domain models: PaymentMode, PaymentType, ProductPaymentMode, UserPaymentPreference
- Mappers: PaymentModeMapper, PaymentTypeMapper, PaymentModeApiMapper
- Repositories: PaymentModeRepository, PaymentTypeRepository, ProductPaymentModeRepository, UserPaymentPreferenceRepository
- Services: PaymentModesService, PaymentModesServiceImpl
- Updated OpenAPI specification for the Payment Modes API
- Updated controller implementation with getPaymentModes method
- Tests: Unit tests and integration tests

## 11. Payment Modes Algorithm

The algorithm for retrieving available payment modes follows these steps:

1. Get all payment modes supported for the specified product type from the product_payment_modes table
2. Get user preferences (blocked and preferred payment modes) from the user_payment_preferences table
3. Filter out payment modes that are blocked by the user
4. For each payment mode:
   a. Get all payment types for this mode
   b. Filter out payment types not supported for this product
   c. Filter out payment types blocked by the user
   d. Sort payment types with preferred types first
5. Sort payment modes with preferred modes first
6. Return the structured response with payment modes and their types

This algorithm ensures that:
- Only payment modes supported for the product type are returned
- User preferences (blocked and preferred) are respected
- Preferred payment modes and types are sorted first
- The response is structured in a hierarchical format (modes -> types)
