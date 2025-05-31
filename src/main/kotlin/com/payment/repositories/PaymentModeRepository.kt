package com.payment.repositories

import com.payment.models.domain.PaymentMode

/**
 * Repository interface for payment modes
 */
interface PaymentModeRepository {
    /**
     * Find all active payment modes
     * @return List of active payment modes
     */
    fun findAllActive(): List<PaymentMode>
    
    /**
     * Find payment mode by ID
     * @param id Payment mode ID
     * @return Payment mode or null if not found
     */
    fun findById(id: Long): PaymentMode?
    
    /**
     * Find payment mode by code
     * @param modeCode Payment mode code
     * @return Payment mode or null if not found
     */
    fun findByCode(modeCode: String): PaymentMode?
}
