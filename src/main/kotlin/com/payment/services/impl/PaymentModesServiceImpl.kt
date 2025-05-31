package com.payment.services.impl

import com.payment.repositories.PaymentModeRepository
import com.payment.repositories.PaymentTypeRepository
import com.payment.services.PaymentModesService
import org.springframework.stereotype.Service

/**
 * Implementation of PaymentModesService
 */
@Service
class PaymentModesServiceImpl(
    private val paymentModeRepository: PaymentModeRepository,
    private val paymentTypeRepository: PaymentTypeRepository
) : PaymentModesService {

    /**
     * Get all available payment modes
     * @return Map containing payment modes data
     */
    override fun getPaymentModes(): Map<String, Any> {
        // Get all active payment modes
        val paymentModes = paymentModeRepository.findAllActive()
        
        // Convert payment modes to response format
        val paymentModesResponse = paymentModes.map { mode ->
            // Get payment types for this mode
            val paymentTypes = paymentTypeRepository.findByModeId(mode.id)
            
            // Convert payment types to response format
            val paymentTypesResponse = paymentTypes.map { type ->
                mapOf(
                    "typeCode" to type.typeCode,
                    "typeName" to type.typeName,
                    "description" to (type.description ?: ""),
                    "isActive" to type.isActive
                )
            }
            
            // Create payment mode response
            mapOf(
                "modeCode" to mode.modeCode,
                "modeName" to mode.modeName,
                "description" to (mode.description ?: ""),
                "isActive" to mode.isActive,
                "paymentTypes" to paymentTypesResponse
            )
        }
        
        // Return response
        return mapOf("paymentModes" to paymentModesResponse)
    }
}
