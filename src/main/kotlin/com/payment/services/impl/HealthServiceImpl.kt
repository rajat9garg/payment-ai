package com.payment.services.impl

import com.payment.services.HealthService
import org.springframework.stereotype.Service

@Service
class HealthServiceImpl : HealthService {
    override fun checkHealth(): String {
        return "UP"
    }
}
