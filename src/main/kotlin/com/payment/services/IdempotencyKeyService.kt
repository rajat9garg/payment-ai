package com.payment.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Service for generating and validating idempotency keys
 */
interface IdempotencyKeyService {
    /**
     * Generate a unique idempotency key
     * @param prefix Key prefix (e.g., "PAY")
     * @param ttlInSeconds Time to live in seconds (default: 24 hours)
     * @return Unique idempotency key
     */
    fun generateKey(prefix: String = "PAY", ttlInSeconds: Long = 24 * 60 * 60): String

    /**
     * Check if a key exists
     * @param key Key to check
     * @return true if key exists, false otherwise
     */
    fun keyExists(key: String): Boolean
}

/**
 * Implementation of IdempotencyKeyService using Redis
 */
@Service
class IdempotencyKeyServiceImpl(
    private val redisTemplate: StringRedisTemplate
) : IdempotencyKeyService {

    companion object {
        private const val IDEMPOTENCY_KEY_PREFIX = "idempotency:"
        private const val COUNTER_KEY = "payment:counter"
        private const val MAX_RETRIES = 10
    }

    override fun generateKey(prefix: String, ttlInSeconds: Long): String {
        var retries = 0
        var key: String
        
        do {
            // Generate a new key using Redis INCR
            val counter = redisTemplate.opsForValue().increment(COUNTER_KEY) ?: 
                throw IllegalStateException("Failed to generate payment ID: Redis counter increment failed")
            
            key = "${prefix}${counter}"
            val fullKey = "$IDEMPOTENCY_KEY_PREFIX$key"
            
            // Try to set the key with NX (only if not exists)
            val set = redisTemplate.opsForValue().setIfAbsent(
                fullKey, 
                "1", 
                ttlInSeconds, 
                TimeUnit.SECONDS
            )
            
            if (set == true) {
                return key
            }
            
            retries++
        } while (retries < MAX_RETRIES)
        
        throw IllegalStateException("Failed to generate unique payment ID after $MAX_RETRIES attempts")
    }

    override fun keyExists(key: String): Boolean {
        return redisTemplate.hasKey("$IDEMPOTENCY_KEY_PREFIX$key") ?: false
    }
}
