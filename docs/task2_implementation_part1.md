# Task 2: Payment Initiation API Implementation Plan - Part 1

## 1. Overview

This document provides the first part of a detailed implementation plan for the Payment Initiation API, which allows clients to initiate payment transactions with various payment methods.

### API Endpoint
- **POST /v1/payment/initiate**
- **Headers**: userId
- **Request Body**: Payment details including amount, currency, payment mode, etc.
- **Response**: Payment transaction details with status

### Implementation Approach
Following the OpenAPI-first approach and the established project rules, we'll implement this API in a structured manner, starting with the Redis setup for idempotency key generation.

## 2. Redis Setup

### 2.1 Docker Compose Configuration

Update the docker-compose.yml file to include Redis:

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_USER: payment
      POSTGRES_PASSWORD: payment
      POSTGRES_DB: payment
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes

volumes:
  postgres-data:
  redis-data:
```

### 2.2 Redis Configuration

Create a Redis configuration class:

```kotlin
// src/main/kotlin/com/payment/config/RedisConfig.kt
package com.payment.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Value("\${spring.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port:6379}")
    private var redisPort: Int = 0

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val configuration = RedisStandaloneConfiguration(redisHost, redisPort)
        return LettuceConnectionFactory(configuration)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        return template
    }
}
```

### 2.3 Update Application Properties

Add Redis configuration to application.yml:

```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment
    username: payment
    password: payment
    driver-class-name: org.postgresql.Driver
  redis:
    host: localhost
    port: 6379
```

## 3. Database Implementation

### 3.1 Database Migration

Create a migration script for the transactions table:

```sql
-- src/main/resources/db/migration/V2__create_transactions_table.sql
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    idempotency_key VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    vendor_transaction_id VARCHAR(100),
    payment_provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
```

### 3.2 Run Migration and Generate jOOQ Classes

Execute the Flyway migration and generate jOOQ classes:

```bash
./gradlew flywayMigrate
./gradlew generateJooq
```
