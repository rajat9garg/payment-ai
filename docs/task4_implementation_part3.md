# Task 4: Payment Reconciliation API Implementation Plan - Part 3

## 8. OpenAPI Specification

### 8.1 Update OpenAPI Specification

Update the OpenAPI specification in `src/main/resources/openapi/api.yaml` to include the payment reconciliation endpoint:

```yaml
# Add this to the existing OpenAPI specification
paths:
  /payment/reconcile:
    post:
      summary: Reconcile pending payments
      description: Reconciles all pending payment transactions and returns the list of reconciled transactions
      operationId: reconcilePayments
      tags:
        - Payment
      responses:
        '200':
          description: Reconciliation completed successfully
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PaymentResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

## 9. Controller Layer Implementation

### 9.1 Generate OpenAPI Models and Interfaces

Run the OpenAPI generator to create the controller interfaces and models:

```bash
./gradlew openApiGenerate
```

This will update:
- Controller interfaces in `build/generated/src/main/kotlin/com/payment/api`
- API models in `build/generated/src/main/kotlin/com/payment/model`

### 9.2 Update Controller Implementation

Update the controller implementation to include the payment reconciliation endpoint:

```kotlin
// src/main/kotlin/com/payment/controllers/PaymentControllerImpl.kt
// Add this method to the existing PaymentControllerImpl class

override fun reconcilePayments(): ResponseEntity<List<ApiPaymentResponse>> {
    // Call service to reconcile pending transactions
    val domainResponses = paymentService.reconcilePendingTransactions()
    
    // Convert domain responses to API responses
    val apiResponses = domainResponses.map { paymentApiMapper.toApiResponse(it) }
    
    // Return response
    return ResponseEntity.ok(apiResponses)
}
```

## 10. Testing

### 10.1 Unit Testing

Create unit tests for the reconciliation service:

```kotlin
// src/test/kotlin/com/payment/services/PaymentServiceImplTest.kt
// Add these test methods to the existing test class

@Test
fun `reconcilePendingTransactions should update status of pending transactions`() {
    // Given
    val pendingTransaction1 = Transaction(
        id = 1L,
        idempotencyKey = "PAY123",
        status = "PENDING",
        userId = "user123",
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        vendorTransactionId = "VENDOR123",
        paymentProvider = "UPI_PROVIDER",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    val pendingTransaction2 = Transaction(
        id = 2L,
        idempotencyKey = "PAY456",
        status = "PENDING",
        userId = "user456",
        amount = BigDecimal("200.00"),
        paymentMode = "CREDIT_CARD",
        vendorTransactionId = "VENDOR456",
        paymentProvider = "CREDIT_CARD_PROVIDER",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    val pendingTransactions = listOf(pendingTransaction1, pendingTransaction2)
    
    val paymentResponse1 = PaymentResponse(
        paymentId = "PAY123",
        status = "SUCCESS",
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        paymentType = "GOOGLE_PAY",
        timestamp = LocalDateTime.now()
    )
    
    val paymentResponse2 = PaymentResponse(
        paymentId = "PAY456",
        status = "PENDING", // No change in status
        amount = BigDecimal("200.00"),
        paymentMode = "CREDIT_CARD",
        paymentType = "VISA",
        timestamp = LocalDateTime.now()
    )
    
    // Mock dependencies
    every { transactionRepository.findByStatus("PENDING") } returns pendingTransactions
    
    val upiProvider = mockk<PaymentProvider>()
    every { upiProvider.getName() } returns "UPI_PROVIDER"
    every { upiProvider.checkPaymentStatus(any()) } returns paymentResponse1.copy(status = "SUCCESS")
    
    val creditCardProvider = mockk<PaymentProvider>()
    every { creditCardProvider.getName() } returns "CREDIT_CARD_PROVIDER"
    every { creditCardProvider.checkPaymentStatus(any()) } returns paymentResponse2
    
    every { paymentProviderFactory.getProvider("UPI", "GOOGLE_PAY") } returns upiProvider
    every { paymentProviderFactory.getProvider("CREDIT_CARD", "VISA") } returns creditCardProvider
    
    every { transactionRepository.update(any()) } returnsArgument 0
    every { reconciliationRepository.save(any()) } returnsArgument 0
    every { notificationService.sendPaymentStatusNotification(any(), any()) } returns true
    
    // When
    val result = paymentService.reconcilePendingTransactions()
    
    // Then
    assertEquals(1, result.size) // Only one transaction had a status change
    assertEquals("SUCCESS", result[0].status)
    assertEquals("PAY123", result[0].paymentId)
    
    // Verify transaction was updated
    verify(exactly = 1) { transactionRepository.update(any()) }
    
    // Verify reconciliation record was created
    verify(exactly = 1) { reconciliationRepository.save(any()) }
    
    // Verify notification was sent
    verify(exactly = 1) { notificationService.sendPaymentStatusNotification(any(), any()) }
}

@Test
fun `reconcilePendingTransactions should handle exceptions gracefully`() {
    // Given
    val pendingTransaction = Transaction(
        id = 1L,
        idempotencyKey = "PAY123",
        status = "PENDING",
        userId = "user123",
        amount = BigDecimal("100.00"),
        paymentMode = "UPI",
        vendorTransactionId = "VENDOR123",
        paymentProvider = "UPI_PROVIDER",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    val pendingTransactions = listOf(pendingTransaction)
    
    // Mock dependencies
    every { transactionRepository.findByStatus("PENDING") } returns pendingTransactions
    every { paymentProviderFactory.getProvider("UPI", "GOOGLE_PAY") } throws RuntimeException("Provider error")
    
    // When
    val result = paymentService.reconcilePendingTransactions()
    
    // Then
    assertTrue(result.isEmpty())
}
```

### 10.2 Integration Testing

Create integration tests for the payment reconciliation API:

```kotlin
// src/test/kotlin/com/payment/controllers/PaymentControllerIntegrationTest.kt
// Add this test method to the existing test class

@Test
fun `reconcilePayments should return reconciled transactions`() {
    // When/Then
    mockMvc.perform(
        post("/api/v1/payment/reconcile")
            .contentType(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
}
```

## 11. Execution Plan

Follow these steps to implement the Payment Reconciliation API:

1. Create the database migration script for the reconciliation table
2. Run the migration and generate jOOQ classes
3. Create domain models and mappers for reconciliation
4. Implement the reconciliation repository
5. Implement the notification service
6. Update the payment service with reconciliation logic
7. Create the scheduled reconciliation job
8. Update the OpenAPI specification with the reconciliation endpoint
9. Generate controller interfaces and models from OpenAPI
10. Implement the controller method for reconciliation
11. Write unit and integration tests
12. Test the API end-to-end

## 12. Deliverables

- Database migration script for reconciliation table
- Domain models: Reconciliation
- Mappers: ReconciliationMapper
- Repository: ReconciliationRepository, ReconciliationRepositoryImpl
- Services: NotificationService, NotificationServiceImpl
- Updated PaymentService with reconciliation logic
- Scheduler: ReconciliationScheduler
- Updated OpenAPI specification for the Payment Reconciliation API
- Updated controller implementation with reconcilePayments method
- Tests: Unit tests and integration tests

## 13. Reconciliation Algorithm

The reconciliation process follows this algorithm:

1. Fetch all transactions with "PENDING" status from the database
2. For each pending transaction:
   a. Determine the payment provider based on the payment mode
   b. Call the provider to check the current status of the transaction
   c. If the status has changed:
      i. Update the transaction status in the database
      ii. Create a reconciliation record with previous and current status
      iii. Send a notification to the user about the status change
      iv. Update the reconciliation record with notification status
   d. If the status has not changed, no action is needed
3. Return a list of all transactions that were reconciled (had status changes)

This algorithm ensures that:
- Only pending transactions are reconciled
- Status changes are properly tracked in the reconciliation table
- Users are notified of payment status changes
- The reconciliation process is idempotent (can be run multiple times without side effects)
- Failed reconciliations for individual transactions don't affect others
