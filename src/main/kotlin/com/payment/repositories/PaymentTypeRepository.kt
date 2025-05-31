package com.payment.repositories

import com.payment.models.domain.PaymentType

/**
 * Repository interface for payment types
 */
interface PaymentTypeRepository {
    /**
     * Find all active payment types
     * @return List of active payment types
     */
    fun findAllActive(): List<PaymentType>
    
    /**
     * Find payment types by mode ID
     * @param modeId Payment mode ID
     * @return List of payment types for the specified mode
     */
    fun findByModeId(modeId: Long): List<PaymentType>
    
    /**
     * Find payment type by ID
     * @param id Payment type ID
     * @return Payment type or null if not found
     */
    fun findById(id: Long): PaymentType?
    
    /**
     * Find payment type by mode ID and type code
     * @param modeId Payment mode ID
     * @param typeCode Payment type code
     * @return Payment type or null if not found
     */
    fun findByModeIdAndCode(modeId: Long, typeCode: String): PaymentType?
}
