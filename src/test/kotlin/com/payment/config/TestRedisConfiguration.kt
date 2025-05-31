package com.payment.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.mockito.Mockito.mock

/**
 * Test configuration for Redis to use in integration tests
 * This configuration provides mock beans for Redis components to avoid
 * actual Redis connections during tests
 */
@TestConfiguration
class TestRedisConfiguration {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return mock(RedisConnectionFactory::class.java)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = redisConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        return template
    }
}
