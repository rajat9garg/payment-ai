# Technical Context

**Created:** 2025-05-24  
**Status:** ACTIVE  
**Author:** Rajat Garg  
**Last Modified:** 2025-05-31
**Last Updated By:** Cascade AI Assistant

## Table of Contents
- [Technology Stack](#technology-stack)
- [Development Environment](#development-environment)
- [Build & Deployment](#build--deployment)
- [Infrastructure](#infrastructure)
- [Development Workflow](#development-workflow)
- [Testing Strategy](#testing-strategy)
- [Monitoring & Logging](#monitoring--logging)
- [Security Considerations](#security-considerations)
- [Idempotency & Concurrency Control](#idempotency--concurrency-control)

## Technology Stack
### Core Technologies
- **Backend Framework:** Spring Boot 3.5.0
- **Database:** PostgreSQL 16.9
- **Programming Languages:** Kotlin 1.9.25, Java 17
- **Build Tool:** Gradle 8.13
- **Caching & Distributed Locking:** Redis 7.0+

### Database & ORM
- **Database System:** PostgreSQL 16.9
- **ORM Framework:** JOOQ 3.19.3
- **Database Migration:** Flyway 9.16.1
  - Migration Location: `src/main/resources/db/migration`
  - Schema: `public`
  - Migration Table: `flyway_schema_history`
  - Custom Tasks: `migrateBrahmaTables`, `cleanBrahmaFlyway`
  - Baseline on Migrate: `true`

### API Layer
- **Framework:** Spring WebFlux with Kotlin Coroutines
- **API Documentation:** OpenAPI 3.0 (Swagger)
- **API First Approach:** All APIs defined in `api.yaml`
- **Base Path:** `/api/v1`
- **Key Endpoints:**
  - `POST /payment/initiate` - Initiate a payment with idempotency key
  - `GET /payment/modes` - List all active payment modes and types
  - `GET /health` - Health check endpoint

### JOOQ Configuration
- **Version:** 3.19.3 (OSS Edition)
- **Generated Code Location:** `build/generated/jooq`
- **Target Package:** `com.payment.generated.jooq`
- **Key Features:**
  - Record Generation
  - Immutable POJOs
  - Kotlin Data Classes
  - Java Time Types
  - Custom Converter for Timestamp <-> LocalDateTime

### Redis Configuration
- **Version:** 7.0+
- **Connection Factory:** Lettuce
- **Connection Pool:** Commons Pool2
- **Key Features:**
  - Distributed Locking
  - Connection Pooling
  - SSL Support
  - Password Authentication
  - Custom Serializers

### Testing
- **Unit Testing:** JUnit 5, MockK, Kotlin Test
- **Integration Testing:** Spring Boot Test, Testcontainers
- **Test Coverage:** 90%+
- **Test Structure:
  - Unit tests in `src/test/kotlin`
  - Integration tests with `@SpringBootTest`
  - Test data in respective test classes

### Dependencies
#### Runtime Dependencies
- `org.springframework.boot:spring-boot-starter-jooq` - JOOQ integration for Spring Boot
- `org.jooq:jooq:3.19.3` - JOOQ core library
- `org.postgresql:postgresql:42.6.0` - PostgreSQL JDBC driver
- `org.flywaydb:flyway-core` - Database migration tool
- `com.fasterxml.jackson.module:jackson-module-kotlin` - Kotlin support for Jackson
- `org.springframework.boot:spring-boot-starter-data-redis` - Spring Data Redis with Lettuce client
- `org.springframework.boot:spring-boot-starter-web` - Spring Web MVC for REST APIs
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` - Kotlin coroutines support
- `org.jetbrains.kotlinx:kotlinx-coroutines-reactor` - Coroutines integration with Reactor

#### Test Dependencies
- `org.springframework.boot:spring-boot-starter-test` - Spring Boot test support
- `io.mockk:mockk:1.13.10` - Mocking library for Kotlin
- `com.h2database:h2:2.2.224` - H2 in-memory database for testing
- `org.junit.platform:junit-platform-launcher` - JUnit 5 test launcher

## Development Environment
### Prerequisites
- Java 17 or higher
- Gradle 8.13
- Docker (for running PostgreSQL and Redis)
- PostgreSQL 16.9
- Redis 7.0+

### Setup Instructions
1. Clone the repository
2. Start required services using Docker Compose:
   ```bash
   docker-compose up -d postgres redis
   ```
3. Build the project:
   ```bash
   ./gradlew clean build
   ```
4. Run the application:
   ```bash
   ./gradlew bootRun
   ```

## Idempotency & Concurrency Control
### Idempotency Implementation
- **Approach:** Client-provided idempotency keys via HTTP headers
- **Header Name:** `Idempotency-Key`
- **Format:** UUID (required)
- **Storage:** Persisted in `transactions` table
- **Lifecycle:** Keys are stored indefinitely
- **Behavior:**
  - First request with a key is processed normally
  - Subsequent requests with the same key return the cached response
  - Concurrent requests with the same key are coordinated via Redis locks

### Concurrency Control
#### Redis Distributed Locking
- **Purpose:** Coordinate concurrent requests across multiple application instances
- **Implementation:** `RedisLockService` using Spring Data Redis
- **Lock Acquisition:**
  - Attempt to SET key with NX (not exists) option
  - Set expiration to prevent deadlocks
- **Lock Release:**
  - DEL key if owned by current instance
  - Implemented in try-finally block to ensure release
- **Configuration:**
  - Lock expiration: 30 seconds
  - Retry attempts: 3
  - Retry delay: 100ms

#### Database Locking
- **Optimistic Locking:**
  - `version` column in `transactions` table
  - Incremented on each update via database trigger
  - Used in `save` and `updateStatus` methods
  - Retry mechanism for concurrent updates
- **Pessimistic Locking:**
  - `SELECT FOR UPDATE` when reading transactions for updates
  - Prevents concurrent modifications during transaction processing
  - Used in `findByIdempotencyKeyForUpdate` method

#### Error Handling
- **Duplicate Key Violations:**
  - Gracefully handled by catching `DataIntegrityViolationException`
  - Returns existing transaction instead of failing
- **Optimistic Lock Failures:**
  - Caught via `OptimisticLockingFailureException`
  - Implements retry mechanism with exponential backoff
- **Redis Lock Failures:**
  - Returns 409 Conflict response
  - Includes retry-after header

### Database Schema
- **transactions Table:**
  ```sql
  CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    vendor_transaction_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
  );
  ```

- **Version Increment Trigger:**
  ```sql
  CREATE OR REPLACE FUNCTION increment_version()
  RETURNS TRIGGER AS $$
  BEGIN
    NEW.version = OLD.version + 1;
    RETURN NEW;
  END;
  $$ LANGUAGE plpgsql;

  CREATE TRIGGER transactions_version_trigger
  BEFORE UPDATE ON transactions
  FOR EACH ROW
  EXECUTE FUNCTION increment_version();
  ```

### Implementation Details
#### Repository Layer
- **TransactionRepository Interface:**
  - `findByIdempotencyKey(idempotencyKey: String): Transaction?`
  - `findByIdempotencyKeyForUpdate(idempotencyKey: String): Transaction?`
  - `save(transaction: Transaction): Transaction`
  - `updateStatus(id: Long, status: TransactionStatus, version: Long): Boolean`

- **TransactionRepositoryImpl:**
  - Manually defines VERSION field due to jOOQ generation lag
  - Uses DSL.field("version", Long::class.java) for queries
  - Implements optimistic locking in save method
  - Uses FOR UPDATE clause for pessimistic locking

#### Service Layer
- **PaymentService Interface:**
  - `initiatePayment(userId: String, request: PaymentRequest, idempotencyKey: String): PaymentResponse`
  - `getPaymentStatus(idempotencyKey: String): PaymentResponse`

- **PaymentServiceImpl:**
  - Checks for existing transaction with idempotency key
  - Acquires Redis distributed lock
  - Double-checks transaction existence after lock acquisition
  - Processes payment with provider
  - Handles concurrent insert attempts
  - Releases Redis lock in finally block

#### Controller Layer
- **PaymentController:**
  - Accepts Idempotency-Key header as UUID
  - Passes idempotency key to service layer
  - Returns appropriate HTTP status codes
  - Includes error handling for concurrent requests

### Testing Strategy
- **Unit Tests:**
  - `PaymentServiceIdempotencyTest` - Tests idempotency behavior
  - `PaymentControllerTest` - Tests controller handling of idempotency key
  - `TransactionRepositoryImplTest` - Tests optimistic and pessimistic locking

- **Integration Tests:**
  - Tests concurrent requests with same idempotency key
  - Tests Redis lock acquisition failures
  - Tests database constraint violations

## Infrastructure
### Local Development
- **Docker Compose:**
  ```yaml
  version: '3.8'
  services:
    postgres:
      image: postgres:15.13
      environment:
        POSTGRES_USER: postgres
        POSTGRES_PASSWORD: postgres
        POSTGRES_DB: payment
      ports:
        - "5432:5432"
      volumes:
        - postgres-data:/var/lib/postgresql/data
    
    redis:
      image: redis:7.0
      command: redis-server --requirepass redis
      ports:
        - "6379:6379"
      volumes:
        - redis-data:/data
  
  volumes:
    postgres-data:
    redis-data:
  ```

### Staging/Production
- Kubernetes-based deployment
- Redis Sentinel for high availability
- PostgreSQL with replication
- Horizontal scaling of application instances
- Load balancing with sticky sessions disabled

## Monitoring & Logging
### Idempotency Metrics
- **Redis Lock Acquisition Rate:** Percentage of successful lock acquisitions
- **Duplicate Request Rate:** Percentage of requests with existing idempotency keys
- **Lock Contention Rate:** Frequency of concurrent requests with same key
- **Lock Duration:** Time spent holding Redis locks

### Logging
- **Idempotency Key Processing:**
  - Log idempotency key with each request (masked for privacy)
  - Log lock acquisition success/failure
  - Log duplicate request detection
  - Log optimistic locking retries

## Security Considerations
### Idempotency Keys
- Keys should be unpredictable UUIDs
- Keys are considered sensitive data (masked in logs)
- No validation of key ownership (relies on client security)
- Keys are stored indefinitely (consider cleanup policy for production)
