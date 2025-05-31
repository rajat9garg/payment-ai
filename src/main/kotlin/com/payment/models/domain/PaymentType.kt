package com.payment.models.domain

import java.time.LocalDateTime

/**
 * Domain model for payment type
 */
data class PaymentType(
    val id: Long,
    val modeId: Long,
    val typeCode: String,
    val typeName: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
