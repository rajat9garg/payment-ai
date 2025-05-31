# Task 4: Payment Reconciliation API Implementation Plan - Part 2

## 5. Notification Service Implementation

### 5.1 Create Notification Service Interface

Define the interface for the notification service:

```kotlin
// src/main/kotlin/com/payment/services/NotificationService.kt
package com.payment.services

import com.payment.models.domain.Transaction

interface NotificationService {
    /**
     * Send a notification about a payment status change
     * 
     * @param transaction The transaction with updated status
     * @param previousStatus The previous status of the transaction
     * @return True if the notification was sent successfully
     */
    fun sendPaymentStatusNotification(transaction: Transaction, previousStatus: String): Boolean
}
```

### 5.2 Implement Notification Service

Implement a mock notification service:

```kotlin
// src/main/kotlin/com/payment/services/impl/NotificationServiceImpl.kt
package com.payment.services.impl

import com.payment.models.domain.Transaction
import com.payment.services.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationServiceImpl : NotificationService {
    
    private val logger = LoggerFactory.getLogger(NotificationServiceImpl::class.java)
    
    /**
     * Send a notification about a payment status change
     * This is a mock implementation that logs the notification
     * 
     * @param transaction The transaction with updated status
     * @param previousStatus The previous status of the transaction
     * @return True if the notification was sent successfully
     */
    override fun sendPaymentStatusNotification(transaction: Transaction, previousStatus: String): Boolean {
        // In a real implementation, this would send an email, SMS, or push notification
        // For this mock implementation, we'll just log the notification
        
        logger.info(
            "NOTIFICATION to user ${transaction.userId}: " +
            "Your payment ${transaction.idempotencyKey} status changed from $previousStatus to ${transaction.status}"
        )
        
        return true
    }
}
```

## 6. Payment Service Implementation

### 6.1 Update Payment Service Interface

Update the payment service interface to include reconciliation:

```kotlin
// src/main/kotlin/com/payment/services/PaymentService.kt
package com.payment.services

import com.payment.models.domain.PaymentRequest
import com.payment.models.domain.PaymentResponse

interface PaymentService {
    // ... existing methods
    
    /**
     * Reconcile pending transactions
     * 
     * @return List of payment responses for reconciled transactions
     */
    fun reconcilePendingTransactions(): List<PaymentResponse>
}
```

### 6.2 Implement Reconciliation Logic

Implement the reconciliation logic in the payment service:

```kotlin
// src/main/kotlin/com/payment/services/impl/PaymentServiceImpl.kt
// Add this method to the existing PaymentServiceImpl class

@Autowired
private lateinit var reconciliationRepository: ReconciliationRepository

@Autowired
private lateinit var notificationService: NotificationService

/**
 * Reconcile pending transactions
 * Algorithm:
 * 1. Fetch all transactions with "pending" status
 * 2. For each transaction:
 *    a. Get appropriate payment provider
 *    b. Call provider to check status (mock response)
 *    c. Update transaction status in database
 *    d. Create reconciliation record
 *    e. Send notification to user
 * 
 * @return List of payment responses for reconciled transactions
 */
@Transactional
override fun reconcilePendingTransactions(): List<PaymentResponse> {
    // Step 1: Fetch all pending transactions
    val pendingTransactions = transactionRepository.findByStatus("PENDING")
    
    // Step 2: Process each pending transaction
    val reconciledTransactions = pendingTransactions.mapNotNull { transaction ->
        try {
            // Step 2a: Get the appropriate payment provider
            // Since we don't store payment type in the transaction, we'll use a default type
            val defaultType = when (transaction.paymentMode) {
                "UPI" -> "GOOGLE_PAY"
                "CREDIT_CARD" -> "VISA"
                "DEBIT_CARD" -> "VISA"
                else -> throw IllegalArgumentException("Unsupported payment mode: ${transaction.paymentMode}")
            }
            
            val paymentProvider = paymentProviderFactory.getProvider(transaction.paymentMode, defaultType)
            
            // Step 2b: Check payment status with provider
            // In a real implementation, we would use the vendorTransactionId
            // For this mock implementation, we'll use a random status
            val vendorTransactionId = transaction.vendorTransactionId ?: "MOCK_ID"
            val paymentResponse = paymentProvider.checkPaymentStatus(vendorTransactionId)
            
            // Step 2c: Update transaction status if changed
            if (paymentResponse.status != transaction.status) {
                val previousStatus = transaction.status
                transaction.status = paymentResponse.status
                transactionRepository.update(transaction)
                
                // Step 2d: Create reconciliation record
                val reconciliation = Reconciliation(
                    transactionId = transaction.id!!,
                    previousStatus = previousStatus,
                    currentStatus = transaction.status,
                    reconciledAt = LocalDateTime.now(),
                    notificationSent = false
                )
                reconciliationRepository.save(reconciliation)
                
                // Step 2e: Send notification to user
                val notificationSent = notificationService.sendPaymentStatusNotification(transaction, previousStatus)
                
                // Update reconciliation record with notification status
                if (notificationSent) {
                    reconciliation.copy(notificationSent = true).let {
                        reconciliationRepository.update(it)
                    }
                }
                
                // Return payment response for reconciled transaction
                PaymentResponse(
                    paymentId = transaction.idempotencyKey,
                    status = transaction.status,
                    amount = transaction.amount,
                    paymentMode = transaction.paymentMode,
                    paymentType = defaultType,
                    timestamp = LocalDateTime.now()
                )
            } else {
                // No status change, no reconciliation needed
                null
            }
        } catch (e: Exception) {
            // Log error and continue with next transaction
            logger.error("Error reconciling transaction ${transaction.idempotencyKey}: ${e.message}")
            null
        }
    }
    
    return reconciledTransactions
}
```

## 7. Scheduled Reconciliation Job

### 7.1 Create Reconciliation Scheduler

Create a scheduler to periodically reconcile pending transactions:

```kotlin
// src/main/kotlin/com/payment/schedulers/ReconciliationScheduler.kt
package com.payment.schedulers

import com.payment.services.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReconciliationScheduler(private val paymentService: PaymentService) {
    
    private val logger = LoggerFactory.getLogger(ReconciliationScheduler::class.java)
    
    /**
     * Scheduled job to reconcile pending transactions
     * Runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    fun reconcilePendingTransactions() {
        logger.info("Starting scheduled reconciliation of pending transactions")
        
        try {
            val reconciledTransactions = paymentService.reconcilePendingTransactions()
            logger.info("Reconciled ${reconciledTransactions.size} transactions")
        } catch (e: Exception) {
            logger.error("Error during scheduled reconciliation: ${e.message}")
        }
    }
}
```

### 7.2 Enable Scheduling

Enable scheduling in the application:

```kotlin
// src/main/kotlin/com/payment/PaymentApplication.kt
package com.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PaymentApplication

fun main(args: Array<String>) {
    runApplication<PaymentApplication>(*args)
}
```
