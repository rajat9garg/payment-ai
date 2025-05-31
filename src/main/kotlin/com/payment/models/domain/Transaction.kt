package com.payment.models.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Domain model for a payment transaction
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
    val id: Long? = null,
    val idempotencyKey: String,
    var status: TransactionStatus,
    val userId: String,
    val amount: BigDecimal,
    val currency: String = "INR",
    val paymentMode: String,
    val paymentType: String,
    val paymentProvider: String,
    var vendorTransactionId: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    var version: Long = 0  // Version field for optimistic locking
) {
    /**
     * Check if the transaction is in a terminal state
     */
    fun isTerminalState(): Boolean {
        return status in listOf(
            TransactionStatus.SUCCEEDED,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED
        )
    }
}

/**
 * Transaction status enum
 */
enum class TransactionStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromString(value: String): TransactionStatus {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                PENDING
            }
        }
    }
}
