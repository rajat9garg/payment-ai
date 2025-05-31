# Task 2: Payment Initiation API Implementation Plan - Part 3

## 6. Payment Provider Implementation

### 6.1 Create Payment Provider Interface

Define the interface for payment providers:

```kotlin
// src/main/kotlin/com/payment/providers/PaymentProvider.kt
package com.payment.providers

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse

interface PaymentProvider {
    /**
     * Get the name of the payment provider
     */
    fun getName(): String
    
    /**
     * Check if the provider supports a specific payment mode and type
     * 
     * @param paymentMode The payment mode (UPI, CREDIT_CARD, etc.)
     * @param paymentType The payment type (GOOGLE_PAY, VISA, etc.)
     * @return True if the provider supports the payment mode and type
     */
    fun supports(paymentMode: String, paymentType: String): Boolean
    
    /**
     * Process a payment request
     * 
     * @param request The payment request
     * @param idempotencyKey The idempotency key for the transaction
     * @return The payment response
     */
    fun processPayment(request: PaymentRequest, idempotencyKey: String): PaymentResponse
    
    /**
     * Check the status of a payment
     * 
     * @param vendorTransactionId The vendor-specific transaction ID
     * @return The payment response with current status
     */
    fun checkPaymentStatus(vendorTransactionId: String): PaymentResponse
}
```

### 6.2 Implement Concrete Payment Providers

Implement the UPI payment provider:

```kotlin
// src/main/kotlin/com/payment/providers/impl/UpiPaymentProvider.kt
package com.payment.providers.impl

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse
import com.payment.providers.PaymentProvider
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class UpiPaymentProvider : PaymentProvider {
    
    override fun getName(): String = "UPI_PROVIDER"
    
    override fun supports(paymentMode: String, paymentType: String): Boolean {
        return paymentMode == "UPI" && (paymentType == "GOOGLE_PAY" || paymentType == "CRED")
    }
    
    override fun processPayment(request: PaymentRequest, idempotencyKey: String): PaymentResponse {
        // In a real implementation, this would call the UPI payment gateway
        // For this mock implementation, we'll simulate a successful payment
        
        // Generate a mock vendor transaction ID
        val vendorTransactionId = "UPI_${UUID.randomUUID()}"
        
        // Return a successful payment response
        return PaymentResponse(
            paymentId = idempotencyKey,
            status = "PENDING", // Initial status is pending
            amount = request.amount,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            timestamp = LocalDateTime.now()
        )
    }
    
    override fun checkPaymentStatus(vendorTransactionId: String): PaymentResponse {
        // In a real implementation, this would call the UPI payment gateway to check status
        // For this mock implementation, we'll randomly return SUCCESS or PENDING
        
        val status = if (Math.random() > 0.3) "SUCCESS" else "PENDING"
        
        return PaymentResponse(
            paymentId = "MOCK_ID",
            status = status,
            amount = java.math.BigDecimal("100.00"),
            paymentMode = "UPI",
            paymentType = "GOOGLE_PAY",
            timestamp = LocalDateTime.now()
        )
    }
}
```

Implement the credit card payment provider:

```kotlin
// src/main/kotlin/com/payment/providers/impl/CreditCardPaymentProvider.kt
package com.payment.providers.impl

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse
import com.payment.providers.PaymentProvider
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class CreditCardPaymentProvider : PaymentProvider {
    
    override fun getName(): String = "CREDIT_CARD_PROVIDER"
    
    override fun supports(paymentMode: String, paymentType: String): Boolean {
        return paymentMode == "CREDIT_CARD" && (paymentType == "VISA" || paymentType == "MASTERCARD")
    }
    
    override fun processPayment(request: PaymentRequest, idempotencyKey: String): PaymentResponse {
        // In a real implementation, this would call the credit card payment gateway
        // For this mock implementation, we'll simulate a successful payment
        
        // Generate a mock vendor transaction ID
        val vendorTransactionId = "CC_${UUID.randomUUID()}"
        
        // Return a successful payment response
        return PaymentResponse(
            paymentId = idempotencyKey,
            status = "PENDING", // Initial status is pending
            amount = request.amount,
            paymentMode = request.paymentMode,
            paymentType = request.paymentType,
            timestamp = LocalDateTime.now()
        )
    }
    
    override fun checkPaymentStatus(vendorTransactionId: String): PaymentResponse {
        // In a real implementation, this would call the credit card payment gateway to check status
        // For this mock implementation, we'll randomly return SUCCESS or PENDING
        
        val status = if (Math.random() > 0.3) "SUCCESS" else "PENDING"
        
        return PaymentResponse(
            paymentId = "MOCK_ID",
            status = status,
            amount = java.math.BigDecimal("100.00"),
            paymentMode = "CREDIT_CARD",
            paymentType = "VISA",
            timestamp = LocalDateTime.now()
        )
    }
}
```

### 6.3 Implement Payment Provider Factory

Create a factory to select the appropriate payment provider:

```kotlin
// src/main/kotlin/com/payment/providers/PaymentProviderFactory.kt
package com.payment.providers

import org.springframework.stereotype.Component

@Component
class PaymentProviderFactory(private val paymentProviders: List<PaymentProvider>) {
    
    /**
     * Get a payment provider that supports the specified payment mode and type
     * 
     * @param paymentMode The payment mode (UPI, CREDIT_CARD, etc.)
     * @param paymentType The payment type (GOOGLE_PAY, VISA, etc.)
     * @return The appropriate payment provider
     * @throws IllegalArgumentException if no provider supports the payment mode and type
     */
    fun getProvider(paymentMode: String, paymentType: String): PaymentProvider {
        return paymentProviders.find { it.supports(paymentMode, paymentType) }
            ?: throw IllegalArgumentException("No payment provider found for mode: $paymentMode and type: $paymentType")
    }
}
```

## 7. Service Layer Implementation

### 7.1 Update Payment Service Interface

Update the payment service interface to include payment initiation:

```kotlin
// src/main/kotlin/com/payment/services/PaymentService.kt
package com.payment.services

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse

interface PaymentService {
    /**
     * Initiate a payment transaction
     * 
     * @param userId The ID of the user making the payment
     * @param request The payment request details
     * @return The payment response
     */
    fun initiatePayment(userId: String, request: PaymentRequest): PaymentResponse
    
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

### 7.2 Implement Payment Service

Implement the payment service:

```kotlin
// src/main/kotlin/com/payment/services/impl/PaymentServiceImpl.kt
package com.payment.services.impl

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse
import com.payment.models.domain.Transaction
import com.payment.providers.PaymentProviderFactory
import com.payment.repositories.TransactionRepository
import com.payment.services.IdempotencyKeyService
import com.payment.services.PaymentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentServiceImpl(
    private val idempotencyKeyService: IdempotencyKeyService,
    private val paymentProviderFactory: PaymentProviderFactory,
    private val transactionRepository: TransactionRepository
) : PaymentService {

    @Transactional
    override fun initiatePayment(userId: String, request: PaymentRequest): PaymentResponse {
        // Generate a unique idempotency key
        val idempotencyKey = idempotencyKeyService.generateIdempotencyKey()
        
        // Get the appropriate payment provider
        val paymentProvider = paymentProviderFactory.getProvider(request.paymentMode, request.paymentType)
        
        // Process the payment with the provider
        val paymentResponse = paymentProvider.processPayment(request, idempotencyKey)
        
        // Save the transaction to the database
        val transaction = Transaction(
            idempotencyKey = idempotencyKey,
            status = paymentResponse.status,
            userId = userId,
            amount = request.amount,
            paymentMode = request.paymentMode,
            vendorTransactionId = null, // Will be updated later during reconciliation
            paymentProvider = paymentProvider.getName(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        transactionRepository.save(transaction)
        
        return paymentResponse
    }
    
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
}
```
