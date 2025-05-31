package com.payment.repositories.impl

import com.payment.infrastructure.jooq.tables.PaymentModes
import com.payment.models.domain.PaymentMode
import com.payment.repositories.PaymentModeRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Implementation of PaymentModeRepository using jOOQ
 */
@Repository
class PaymentModeRepositoryImpl(private val dslContext: DSLContext) : PaymentModeRepository {

    /**
     * Find all active payment modes
     * @return List of active payment modes
     */
    override fun findAllActive(): List<PaymentMode> {
        val paymentModes = PaymentModes.PAYMENT_MODES
        
        return dslContext.selectFrom(paymentModes)
            .where(paymentModes.IS_ACTIVE.eq(true))
            .fetch()
            .map { record ->
                PaymentMode(
                    id = record.id.toLong(),
                    modeCode = record.modeCode,
                    modeName = record.modeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }

    /**
     * Find payment mode by ID
     * @param id Payment mode ID
     * @return Payment mode or null if not found
     */
    override fun findById(id: Long): PaymentMode? {
        val paymentModes = PaymentModes.PAYMENT_MODES
        
        return dslContext.selectFrom(paymentModes)
            .where(paymentModes.ID.eq(id.toInt()))
            .fetchOne()
            ?.let { record ->
                PaymentMode(
                    id = record.id.toLong(),
                    modeCode = record.modeCode,
                    modeName = record.modeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }

    /**
     * Find payment mode by code
     * @param modeCode Payment mode code
     * @return Payment mode or null if not found
     */
    override fun findByCode(modeCode: String): PaymentMode? {
        val paymentModes = PaymentModes.PAYMENT_MODES
        
        return dslContext.selectFrom(paymentModes)
            .where(paymentModes.MODE_CODE.eq(modeCode))
            .fetchOne()
            ?.let { record ->
                PaymentMode(
                    id = record.id.toLong(),
                    modeCode = record.modeCode,
                    modeName = record.modeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }
}
