package com.payment.models.domain

import java.time.LocalDateTime

/**
 * Domain model for payment mode
 */
data class PaymentMode(
    val id: Long,
    val modeCode: String,
    val modeName: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
