package com.payment.services

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Service for distributed locking using Redis
 * Used to ensure concurrency control across multiple service instances
 */
@Service
class RedisLockService(private val redisTemplate: RedisTemplate<String, String>) {
    private val logger = LoggerFactory.getLogger(RedisLockService::class.java)
    private val lockTimeout = 10000L // 10 seconds
    
    /**
     * Acquire a distributed lock
     * @param lockKey Key to lock on
     * @return true if lock was acquired, false otherwise
     */
    fun acquireLock(lockKey: String): Boolean {
        val lockKeyWithPrefix = "lock:$lockKey"
        val success = redisTemplate.opsForValue()
            .setIfAbsent(lockKeyWithPrefix, "locked", Duration.ofMillis(lockTimeout))
        
        logger.info("Acquiring lock for key: $lockKey, success: $success")
        return success ?: false
    }
    
    /**
     * Release a distributed lock
     * @param lockKey Key to release
     */
    fun releaseLock(lockKey: String) {
        val lockKeyWithPrefix = "lock:$lockKey"
        redisTemplate.delete(lockKeyWithPrefix)
        logger.info("Released lock for key: $lockKey")
    }
}
