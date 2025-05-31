package com.payment.repositories.impl

import com.payment.infrastructure.jooq.tables.PaymentTypes
import com.payment.models.domain.PaymentType
import com.payment.repositories.PaymentTypeRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Implementation of PaymentTypeRepository using jOOQ
 */
@Repository
class PaymentTypeRepositoryImpl(private val dslContext: DSLContext) : PaymentTypeRepository {

    /**
     * Find all active payment types
     * @return List of active payment types
     */
    override fun findAllActive(): List<PaymentType> {
        val paymentTypes = PaymentTypes.PAYMENT_TYPES
        
        return dslContext.selectFrom(paymentTypes)
            .where(paymentTypes.IS_ACTIVE.eq(true))
            .fetch()
            .map { record ->
                PaymentType(
                    id = record.id.toLong(),
                    modeId = record.modeId.toLong(),
                    typeCode = record.typeCode,
                    typeName = record.typeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }

    /**
     * Find payment types by mode ID
     * @param modeId Payment mode ID
     * @return List of payment types for the specified mode
     */
    override fun findByModeId(modeId: Long): List<PaymentType> {
        val paymentTypes = PaymentTypes.PAYMENT_TYPES
        
        return dslContext.selectFrom(paymentTypes)
            .where(paymentTypes.MODE_ID.eq(modeId.toInt()))
            .and(paymentTypes.IS_ACTIVE.eq(true))
            .fetch()
            .map { record ->
                PaymentType(
                    id = record.id.toLong(),
                    modeId = record.modeId.toLong(),
                    typeCode = record.typeCode,
                    typeName = record.typeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }

    /**
     * Find payment type by ID
     * @param id Payment type ID
     * @return Payment type or null if not found
     */
    override fun findById(id: Long): PaymentType? {
        val paymentTypes = PaymentTypes.PAYMENT_TYPES
        
        return dslContext.selectFrom(paymentTypes)
            .where(paymentTypes.ID.eq(id.toInt()))
            .fetchOne()
            ?.let { record ->
                PaymentType(
                    id = record.id.toLong(),
                    modeId = record.modeId.toLong(),
                    typeCode = record.typeCode,
                    typeName = record.typeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }

    /**
     * Find payment type by mode ID and type code
     * @param modeId Payment mode ID
     * @param typeCode Payment type code
     * @return Payment type or null if not found
     */
    override fun findByModeIdAndCode(modeId: Long, typeCode: String): PaymentType? {
        val paymentTypes = PaymentTypes.PAYMENT_TYPES
        
        return dslContext.selectFrom(paymentTypes)
            .where(paymentTypes.MODE_ID.eq(modeId.toInt()))
            .and(paymentTypes.TYPE_CODE.eq(typeCode))
            .fetchOne()
            ?.let { record ->
                PaymentType(
                    id = record.id.toLong(),
                    modeId = record.modeId.toLong(),
                    typeCode = record.typeCode,
                    typeName = record.typeName,
                    description = record.description,
                    isActive = record.isActive,
                    createdAt = record.createdAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) },
                    updatedAt = record.updatedAt?.let { LocalDateTime.parse(it.toString().replace(" ", "T")) }
                )
            }
    }
}
