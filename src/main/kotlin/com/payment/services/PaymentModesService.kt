package com.payment.services

/**
 * Service interface for payment modes
 */
interface PaymentModesService {
    /**
     * Get all available payment modes
     * @return Map containing payment modes data
     */
    fun getPaymentModes(): Map<String, Any>
}
