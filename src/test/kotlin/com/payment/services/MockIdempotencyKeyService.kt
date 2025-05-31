package com.payment.services

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Mock implementation of IdempotencyKeyService for testing
 * This service generates predictable keys for testing and doesn't require Redis
 */
@Service
@Primary
@Profile("test")
class MockIdempotencyKeyService : IdempotencyKeyService {
    
    private var counter = 0
    
    override fun generateKey(prefix: String, ttlInSeconds: Long): String {
        return "test-idempotency-key-${++counter}"
    }

    override fun keyExists(key: String): Boolean {
        TODO("Not yet implemented")
    }

    fun isKeyUsed(key: String): Boolean {
        return false
    }
    
    fun markKeyAsUsed(key: String): Boolean {
        return true
    }
}
