package com.payment.providers

import org.springframework.stereotype.Component

/**
 * Factory for selecting the appropriate payment provider
 */
@Component
class PaymentProviderFactory(private val paymentProviders: List<PaymentProvider>) {
    
    /**
     * Get payment provider for the specified payment mode and type
     * @param paymentMode Payment mode (e.g., UPI, CREDIT_CARD)
     * @param paymentType Payment type (e.g., GOOGLE_PAY, VISA)
     * @return Appropriate payment provider or null if none found
     */
    fun getProvider(paymentMode: String, paymentType: String): PaymentProvider? {
        return paymentProviders.find { it.supports(paymentMode, paymentType) }
    }
    
    /**
     * Get payment provider by name
     * @param providerName Provider name
     * @return Payment provider or null if not found
     */
    fun getProviderByName(providerName: String): PaymentProvider? {
        return paymentProviders.find { it.getName() == providerName }
    }
}
