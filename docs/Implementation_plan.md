IMPORTANT
- NO SECURITY AT ALL (THIS IS TEST PROJECT I DON't want security on any layer DB, Controller etc userID will come in header)
- CONTROLLERS ALWAYS implemented using openAPI
- IF You are introducing a new tech like redis or mongo create its implemenation first (add it in docker compose and then implement config and client)
- INCLUDE ALGORITHM FOR ANY tasks that are needed in the task plan

Task1: Payment Modes API
    - Create Migrations for the database tables:
        - payment_modes table with id, uuid, payment_mode, type fields
    - Run the migrations using Flyway and generate jOOQ classes for type-safe SQL
    - Create Models and mappers for payment modes between domain model and database
    - Implement Repository Layer:
        - Create PaymentModesRepository interface
        - Implement PaymentModesRepositoryImpl using jOOQ
    - Implement Service Layer:
        - Create PaymentModesService interface
        - Implement PaymentModesServiceImpl with logic to:
            - Check payment modes available for the user
            - Check payment modes available for the product type
            - Check available payment modes for the vendor
            - Return formatted JSON response
    - Implement Controller Layer:
        - Use Open API to write api specs and then generate interfaces and models
        - Implement those interfaces for controllers and then implement the mappers between openApi generated models and domain models 
        - Create PaymentModesController with GET /v1/payment/modes endpoint
        - Implement request validation and error handling
    - Write unit and integration tests
    - Update OpenAPI specification in api.yaml

Task2: Payment Initiation API
    - Add Redis to docker-compose.yml
    - Implement Redis configuration and client for payment ID generation
    - Create Migrations for the database tables:
        - transactions table with all required fields
    - Create Models and mappers for transactions between domain model and database
    - Implement Repository Layer:
        - Create TransactionRepository interface
        - Implement TransactionRepositoryImpl using jOOQ
    - Implement Payment Provider Layer:
        - Create PaymentProvider interface
        - Create concrete implementations for different payment methods (UPI, CREDIT_CARD, etc.)
        - Implement PaymentProviderFactory for provider selection
    - Implement Service Layer:
        - Create PaymentService interface
        - Implement PaymentServiceImpl with logic to:
            - Generate unique payment idempotency key using Redis
            - Call payment provider to initiate payment
            - Store transaction details in database
        - Algorithm for idempotency key generation:
            1. Use Redis INCR to generate a sequential number
            2. Format as "PAY{number}"
            3. Check if key exists in database
            4. If exists, repeat from step 1
            5. If not exists, use as payment ID
    - Implement Controller Layer:
        - Update OpenAPI specification with payment initiation endpoint
        - Generate interfaces and models from OpenAPI spec
        - Implement PaymentController with POST /v1/payment/initiate endpoint
        - Create mappers between OpenAPI models and domain models
        - Implement request validation and error handling
    - Write unit and integration tests

Task3: Payment Status API
    - Implement Service Layer:
        - Add getPaymentStatus method to PaymentService interface
        - Implement logic in PaymentServiceImpl to:
            - Query transaction status from database
            - Return formatted status response
    - Implement Controller Layer:
        - Update OpenAPI specification with payment status endpoint
        - Generate interfaces and models from OpenAPI spec
        - Add GET /v1/payment/status endpoint implementation to PaymentController
        - Create or update mappers between OpenAPI models and domain models
        - Implement request validation and error handling
    - Write unit and integration tests

Task4: Payment Reconciliation API
    - Create Migrations for the database tables:
        - reconciliation table with required fields
    - Create Models and mappers for reconciliation records between domain model and database
    - Implement Repository Layer:
        - Create ReconciliationRepository interface
        - Implement ReconciliationRepositoryImpl using jOOQ
    - Implement Notification Service:
        - Create NotificationService interface
        - Implement NotificationServiceImpl with mock notification logic
    - Implement Service Layer:
        - Add reconcilePendingTransactions method to PaymentService interface
        - Implement logic in PaymentServiceImpl to:
            - Query pending transactions
            - Check status with payment providers
            - Update transaction status
            - Create reconciliation records
            - Send notifications to users
        - Algorithm for reconciliation:
            1. Fetch all transactions with "pending" status
            2. For each transaction:
               a. Get appropriate payment provider
               b. Call provider to check status (mock response)
               c. Update transaction status in database
               d. Create reconciliation record
               e. Send notification to user
    - Implement Controller Layer:
        - Update OpenAPI specification with reconciliation endpoint
        - Generate interfaces and models from OpenAPI spec
        - Add POST /v1/payment/reconcile endpoint to PaymentController
        - Create or update mappers between OpenAPI models and domain models
        - Implement request validation and error handling
    - Implement Scheduled Job:
        - Create ReconciliationScheduler to periodically reconcile pending transactions
    - Write unit and integration tests

Task5: Testing and Documentation
    - Implement Integration Tests:
        - Create test fixtures and data
        - Test complete API flows
        - Test concurrency scenarios
    - Implement Performance Tests:
        - Test system under load
        - Verify idempotency under concurrent requests
    - Complete API Documentation:
        - Finalize OpenAPI specification
        - Generate API documentation
        - Create usage examples
    - Create Deployment Documentation:
        - Document environment setup
        - Document configuration options
        - Create deployment scripts