package com.payment.mappers

import learn.ai.generated.model.PaymentMode as ApiPaymentMode
import learn.ai.generated.model.PaymentType as ApiPaymentType
import learn.ai.generated.model.PaymentModesResponse
import org.springframework.stereotype.Component

/**
 * Mapper for converting between domain models and API models for payment modes
 */
@Component
class PaymentModeApiMapper {
    
    /**
     * Convert service response to API response
     * @param serviceResponse Map containing payment modes data from service
     * @return PaymentModesResponse API model
     */
    fun toApiResponse(serviceResponse: Map<String, Any>): PaymentModesResponse {
        @Suppress("UNCHECKED_CAST")
        val paymentModes = serviceResponse["paymentModes"] as List<Map<String, Any>>
        
        val apiPaymentModes = paymentModes.map { mode ->
            @Suppress("UNCHECKED_CAST")
            val paymentTypes = mode["paymentTypes"] as List<Map<String, Any>>
            
            val apiPaymentTypes = paymentTypes.map { type ->
                ApiPaymentType(
                    typeCode = type["typeCode"] as String,
                    typeName = type["typeName"] as String,
                    description = type["description"] as String,
                    isActive = type["isActive"] as Boolean
                )
            }
            
            ApiPaymentMode(
                modeCode = mode["modeCode"] as String,
                modeName = mode["modeName"] as String,
                description = mode["description"] as String,
                isActive = mode["isActive"] as Boolean,
                paymentTypes = apiPaymentTypes
            )
        }
        
        return PaymentModesResponse(paymentModes = apiPaymentModes)
    }
}
