# Payment Gateway System - Low-Level Design (LLD)

## 1. Implementation Overview

The Payment Gateway System will be implemented as a Spring Boot application with RESTful APIs as specified in the requirements document. The implementation will focus on creating a robust payment processing system that supports multiple payment vendors, ensures transaction idempotency, and provides reconciliation capabilities.

### Implementation Approach
The implementation will follow the architecture outlined in the HLD, with a clear separation of concerns between controllers, services, and data access layers. The system will be built using Spring Boot with Kotlin as indicated by the project structure.

### Development Constraints
- The system must use PostgreSQL for data storage as specified in the requirements
- Redis must be used for generating unique payment identifiers
- The system must implement the exact API endpoints specified in the requirements

### Implementation Priorities
The requirements document does not specify implementation priorities. Based on the core functionality, the following implementation order can be derived:
1. Database schema implementation
2. Payment modes functionality
3. Payment initiation with idempotency
4. Payment status checking
5. Transaction reconciliation

## 2. Detailed Component Design

### 2.1 Service Implementation Details

#### Payment Controller
Based on the requirements, the following controllers will be implemented:

```kotlin
@RestController
@RequestMapping("/v1/payment")
class PaymentController(
    private val paymentService: PaymentService,
    private val paymentModesService: PaymentModesService
) {
    
    @GetMapping("/modes")
    fun getPaymentModes(
        @RequestHeader("userId") userId: String,
        @RequestHeader("productType") productType: String
    ): ResponseEntity<Map<String, Any>> {
        val modes = paymentModesService.getAvailablePaymentModes(userId, productType)
        return ResponseEntity.ok(modes)
    }
    
    @PostMapping("/initiate")
    fun initiatePayment(
        @RequestHeader("userId") userId: String,
        @RequestBody request: PaymentInitiateRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.initiatePayment(userId, request)
        return ResponseEntity.ok(payment)
    }
    
    @GetMapping("/status")
    fun getPaymentStatus(
        @RequestHeader("userId") userId: String,
        @RequestHeader("paymentID") paymentId: String
    ): ResponseEntity<PaymentStatusResponse> {
        val status = paymentService.getPaymentStatus(userId, paymentId)
        return ResponseEntity.ok(status)
    }
    
    // Reconciliation endpoint not explicitly defined in requirements
    @PostMapping("/reconcile")
    fun reconcilePayments(): ResponseEntity<List<PaymentStatusResponse>> {
        val reconciledPayments = paymentService.reconcilePendingTransactions()
        return ResponseEntity.ok(reconciledPayments)
    }
}
```

#### Payment Service
Based on the requirements, the payment service will implement the following functionality:

```kotlin
@Service
class PaymentServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val paymentProviderFactory: PaymentProviderFactory,
    private val notificationService: NotificationService
) : PaymentService {

    override fun initiatePayment(userId: String, request: PaymentInitiateRequest): PaymentResponse {
        // Check payment modes available for user and product type
        // This could be a call to the PaymentModesService
        
        // Generate unique payment idempotency key using Redis
        val paymentKey = generateUniquePaymentKey()
        
        // Call payment provider to initiate payment
        val provider = paymentProviderFactory.getProvider(request.transactionType)
        val vendorResponse = provider.initiatePayment(request.amount, paymentKey)
        
        // Store payment details in transactions table
        val transaction = Transaction(
            idempotencyKey = paymentKey,
            status = "pending",
            userId = userId,
            amount = request.amount,
            paymentMode = request.transactionType,
            vendorTransactionId = vendorResponse.transactionId,
            paymentProvider = provider.getName()
        )
        transactionRepository.save(transaction)
        
        // Return payment details
        return PaymentResponse(
            id = paymentKey,
            status = "pending",
            amount = request.amount,
            description = "Payment for order #123" // Note: This is hardcoded in the example response
        )
    }
    
    override fun getPaymentStatus(userId: String, paymentId: String): PaymentStatusResponse {
        val transaction = transactionRepository.findByIdempotencyKeyAndUserId(paymentId, userId)
            ?: throw ResourceNotFoundException("Payment not found")
            
        return PaymentStatusResponse(
            status = transaction.status
        )
    }
    
    override fun reconcilePendingTransactions(): List<PaymentStatusResponse> {
        val pendingTransactions = transactionRepository.findByStatus("pending")
        val reconciledPayments = mutableListOf<PaymentStatusResponse>()
        
        pendingTransactions.forEach { transaction ->
            val provider = paymentProviderFactory.getProvider(transaction.paymentMode)
            val vendorStatus = provider.checkStatus(transaction.vendorTransactionId)
            
            // Update transaction status
            transaction.status = vendorStatus
            transactionRepository.save(transaction)
            
            // Create reconciliation record
            val reconciliation = Reconciliation(
                vendorTransactionId = transaction.vendorTransactionId,
                vendorStatus = vendorStatus,
                createdAt = LocalDateTime.now()
            )
            reconciliationRepository.save(reconciliation)
            
            // Send notification to user
            notificationService.sendPaymentStatusNotification(transaction.userId, transaction.idempotencyKey, vendorStatus)
            
            reconciledPayments.add(PaymentStatusResponse(status = vendorStatus))
        }
        
        return reconciledPayments
    }
    
    private fun generateUniquePaymentKey(): String {
        // Use Redis to generate a unique key
        val value = redisTemplate.opsForValue().increment("payment:id") ?: 1
        val paymentKey = "PAY$value"
        
        // Check if key exists in database
        while (transactionRepository.existsByIdempotencyKey(paymentKey)) {
            // If exists, generate a new key
            val newValue = redisTemplate.opsForValue().increment("payment:id") ?: 1
            paymentKey = "PAY$newValue"
        }
        
        return paymentKey
    }
}
```

#### Payment Modes Service
Based on the requirements, the payment modes service will implement:

```kotlin
@Service
class PaymentModesServiceImpl(
    private val paymentModesRepository: PaymentModesRepository
) : PaymentModesService {

    override fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any> {
        // Check payment modes available for the user
        val userModes = getUserPaymentModes(userId)
        
        // Check payment modes available for the product type
        val productModes = getProductPaymentModes(productType)
        
        // Check available payment modes for the vendor
        val vendorModes = getVendorPaymentModes()
        
        // Intersection of all available modes
        val availableModes = calculateAvailableModes(userModes, productModes, vendorModes)
        
        // Format response according to requirements
        return formatPaymentModesResponse(availableModes)
    }
    
    private fun getUserPaymentModes(userId: String): List<PaymentMode> {
        // Implementation would retrieve user-specific payment modes
        // This is not detailed in the requirements
        return paymentModesRepository.findByUserId(userId)
    }
    
    private fun getProductPaymentModes(productType: String): List<PaymentMode> {
        // Implementation would retrieve product-specific payment modes
        // This is not detailed in the requirements
        return paymentModesRepository.findByProductType(productType)
    }
    
    private fun getVendorPaymentModes(): List<PaymentMode> {
        // Implementation would retrieve vendor-supported payment modes
        // This is not detailed in the requirements
        return paymentModesRepository.findAll()
    }
    
    private fun calculateAvailableModes(
        userModes: List<PaymentMode>,
        productModes: List<PaymentMode>,
        vendorModes: List<PaymentMode>
    ): List<PaymentMode> {
        // Logic to find the intersection of available modes
        // Implementation details not specified in requirements
        return userModes.filter { it in productModes && it in vendorModes }
    }
    
    private fun formatPaymentModesResponse(modes: List<PaymentMode>): Map<String, Any> {
        // Format according to the required response structure
        val response = mutableMapOf<String, Any>()
        
        // Group by payment mode type (UPI, CREDIT_CARD, DEBIT_CARD)
        val upiModes = modes.filter { it.paymentMode == "UPI" }
            .map { it.type }
            .toSet()
            
        if (upiModes.isNotEmpty()) {
            response["UPI"] = upiModes
        }
        
        if (modes.any { it.paymentMode == "CREDIT_CARD" }) {
            response["CREDIT_CARD"] = emptyMap<String, Any>()
        }
        
        if (modes.any { it.paymentMode == "DEBIT_CARD" }) {
            response["DEBIT_CARD"] = emptyMap<String, Any>()
        }
        
        return response
    }
}
```

#### Payment Provider Factory
Based on the requirements for supporting multiple payment vendors:

```kotlin
@Service
class PaymentProviderFactoryImpl : PaymentProviderFactory {

    private val providers = mapOf(
        "UPI" to UpiPaymentProvider(),
        "CREDIT_CARD" to CreditCardPaymentProvider(),
        "DEBIT_CARD" to DebitCardPaymentProvider()
    )
    
    override fun getProvider(paymentMode: String): PaymentProvider {
        return providers[paymentMode] ?: throw IllegalArgumentException("Unsupported payment mode: $paymentMode")
    }
}
```

#### Notification Service
Based on the requirement to send notifications to users:

```kotlin
@Service
class NotificationServiceImpl : NotificationService {

    override fun sendPaymentStatusNotification(userId: String, paymentId: String, status: String) {
        // Implementation for sending notifications
        // Details not specified in requirements
        log.info("Sending payment status notification to user $userId for payment $paymentId: $status")
        // Actual notification logic would go here
    }
}
```

### 2.2 API Implementation Details

#### Payment Modes API

**Endpoint**: `GET /v1/payment/modes`

**Request Headers**:
- userId: String
- productType: String

**Response Body**:
```json
{
    "UPI": {
        "GOOGLE_PAY", "CRED"
    },
    "CREDIT_CARD": {},
    "DEBIT_CARD": {}
}
```

**Error Handling**:
- 400 Bad Request: If required headers are missing
- 404 Not Found: If no payment modes are available
- 500 Internal Server Error: For server-side issues

#### Payment Initiation API

**Endpoint**: `POST /v1/payment/initiate`

**Request Headers**:
- userId: String

**Request Body**:
```json
{
    "amount": 100,
    "transactionType": "UPI/CREDIT_CARD",
    "credBlock": "afddasfasdd"
}
```

**Response Body**:
```json
{
    "id": "payment123",
    "status": "pending",
    "amount": 100,
    "description": "Payment for order #123"
}
```

**Error Handling**:
- 400 Bad Request: If request body is invalid
- 401 Unauthorized: If userId is invalid
- 500 Internal Server Error: For server-side issues

#### Payment Status API

**Endpoint**: `GET /v1/payment/status`

**Request Headers**:
- userId: String
- paymentID: String

**Response Body**:
```json
{
    "status": "Pending/success/failure"
}
```

**Error Handling**:
- 400 Bad Request: If required headers are missing
- 404 Not Found: If payment is not found
- 500 Internal Server Error: For server-side issues

#### Payment Reconciliation API

**Endpoint**: Not explicitly defined in requirements, assumed to be `POST /v1/payment/reconcile`

**Response Body**:
```json
[
    {
        "status": "success/failure"
    }
]
```

**Error Handling**:
- 500 Internal Server Error: For server-side issues

### 2.3 Database Implementation

Based on the requirements document, the following database schema will be implemented:

#### Transactions Table
```sql
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    vendor_transaction_id VARCHAR(255),
    payment_provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
```

#### Reconciliation Table
```sql
CREATE TABLE reconciliation (
    id SERIAL PRIMARY KEY,
    vendor_transaction_id VARCHAR(255) NOT NULL,
    vendor_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (vendor_transaction_id) REFERENCES transactions(vendor_transaction_id)
);

CREATE INDEX idx_reconciliation_vendor_transaction_id ON reconciliation(vendor_transaction_id);
```

#### Payment Modes Table
```sql
CREATE TABLE payment_modes (
    id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_modes_payment_mode ON payment_modes(payment_mode);
CREATE INDEX idx_payment_modes_type ON payment_modes(type);
```

## 3. Interface Specifications

### 3.1 API Contracts

#### PaymentService Interface
```kotlin
interface PaymentService {
    fun initiatePayment(userId: String, request: PaymentInitiateRequest): PaymentResponse
    fun getPaymentStatus(userId: String, paymentId: String): PaymentStatusResponse
    fun reconcilePendingTransactions(): List<PaymentStatusResponse>
}
```

#### PaymentModesService Interface
```kotlin
interface PaymentModesService {
    fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any>
}
```

#### PaymentProvider Interface
```kotlin
interface PaymentProvider {
    fun getName(): String
    fun initiatePayment(amount: BigDecimal, idempotencyKey: String): VendorResponse
    fun checkStatus(vendorTransactionId: String): String
}
```

#### NotificationService Interface
```kotlin
interface NotificationService {
    fun sendPaymentStatusNotification(userId: String, paymentId: String, status: String)
}
```

### 3.2 Data Interfaces

#### Data Transfer Objects (DTOs)

```kotlin
data class PaymentInitiateRequest(
    val amount: BigDecimal,
    val transactionType: String,
    val credBlock: String
)

data class PaymentResponse(
    val id: String,
    val status: String,
    val amount: BigDecimal,
    val description: String
)

data class PaymentStatusResponse(
    val status: String
)

data class VendorResponse(
    val transactionId: String,
    val status: String
)
```

#### Entity Classes

```kotlin
@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "idempotency_key", unique = true, nullable = false)
    val idempotencyKey: String,
    
    @Column(nullable = false)
    var status: String,
    
    @Column(name = "user_id", nullable = false)
    val userId: String,
    
    @Column(nullable = false)
    val amount: BigDecimal,
    
    @Column(name = "payment_mode", nullable = false)
    val paymentMode: String,
    
    @Column(name = "vendor_transaction_id")
    val vendorTransactionId: String?,
    
    @Column(name = "payment_provider", nullable = false)
    val paymentProvider: String,
    
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "reconciliation")
data class Reconciliation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "vendor_transaction_id", nullable = false)
    val vendorTransactionId: String,
    
    @Column(name = "vendor_status", nullable = false)
    val vendorStatus: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime
)

@Entity
@Table(name = "payment_modes")
data class PaymentMode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(unique = true, nullable = false)
    val uuid: String,
    
    @Column(name = "payment_mode", nullable = false)
    val paymentMode: String,
    
    @Column(nullable = false)
    val type: String,
    
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

## 4. Implementation Patterns

### 4.1 Design Patterns

Based on the requirements and HLD, the following design patterns will be implemented:

- **Factory Pattern**: For creating payment provider instances based on payment mode
- **Repository Pattern**: For data access abstraction
- **Strategy Pattern**: For different payment processing strategies based on payment mode
- **Idempotency Pattern**: For preventing duplicate transactions

### 4.2 Error Handling

The requirements document does not specify detailed error handling requirements. The following approach will be implemented:

```kotlin
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = ex.message ?: "Resource not found",
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }
    
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Bad request",
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "An unexpected error occurred",
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: LocalDateTime
)

class ResourceNotFoundException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)
```

## 5. Security Implementation

The requirements document does not specify detailed security requirements. Basic authentication is implied through the use of userId in API headers. The following minimal security implementation will be provided:

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/v1/payment/**").authenticated()
            .and()
            .httpBasic()
    }
    
    // User details service would be implemented here
    // Details not specified in requirements
}
```

## 6. Performance Implementation

The requirements document does not specify performance requirements. The following basic performance optimizations will be implemented:

- Database indexing on frequently queried fields
- Connection pooling for database connections
- Redis caching for frequently accessed data
- Asynchronous processing for non-critical operations

## 7. Testing Strategy

The requirements document does not specify testing requirements. The following testing approach will be implemented:

- Unit tests for service and repository layers
- Integration tests for API endpoints
- Mock tests for external dependencies (payment providers)
- Performance tests for critical flows

## 8. Deployment Considerations

The requirements document does not specify deployment requirements. Based on the use of Docker Compose in the project structure, the following deployment approach is assumed:

- Containerized deployment using Docker
- PostgreSQL and Redis as containerized services
- Environment-specific configuration using environment variables

## 9. Implementation Constraints

Based on the requirements document, the following implementation constraints are identified:

- PostgreSQL must be used for data storage
- Redis must be used for generating unique payment identifiers
- The system must handle concurrency issues
- The system must prevent double payments
- The system must support multiple payment vendors

## 10. Implementation Gaps

The following information is missing from the requirements and would need clarification:

- Detailed authentication and authorization mechanisms
- Specific payment provider integration details
- Error handling and recovery procedures
- Performance and scalability requirements
- Monitoring and logging requirements
- Specific notification mechanisms
- Detailed reconciliation logic and scheduling
