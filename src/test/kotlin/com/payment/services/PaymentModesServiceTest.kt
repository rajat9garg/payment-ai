package com.payment.services

import com.payment.models.domain.PaymentMode
import com.payment.models.domain.PaymentType
import com.payment.repositories.PaymentModeRepository
import com.payment.repositories.PaymentTypeRepository
import com.payment.services.impl.PaymentModesServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PaymentModesServiceTest {

    @Mock
    private lateinit var paymentModeRepository: PaymentModeRepository

    @Mock
    private lateinit var paymentTypeRepository: PaymentTypeRepository

    @InjectMocks
    private lateinit var paymentModesService: PaymentModesServiceImpl

    @Test
    fun `getPaymentModes should return all active payment modes with their types`() {
        // Given
        val now = LocalDateTime.now()
        
        val paymentMode1 = PaymentMode(
            id = 1L,
            modeCode = "UPI",
            modeName = "Unified Payment Interface",
            description = "Direct bank transfer using UPI",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        val paymentMode2 = PaymentMode(
            id = 2L,
            modeCode = "CREDIT_CARD",
            modeName = "Credit Card",
            description = "Pay using credit card",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        val paymentType1 = PaymentType(
            id = 1L,
            modeId = 1L,
            typeCode = "GOOGLE_PAY",
            typeName = "Google Pay",
            description = "Pay using Google Pay UPI",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        val paymentType2 = PaymentType(
            id = 2L,
            modeId = 2L,
            typeCode = "VISA",
            typeName = "Visa",
            description = "Pay using Visa credit card",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        // When
        `when`(paymentModeRepository.findAllActive()).thenReturn(listOf(paymentMode1, paymentMode2))
        `when`(paymentTypeRepository.findByModeId(1L)).thenReturn(listOf(paymentType1))
        `when`(paymentTypeRepository.findByModeId(2L)).thenReturn(listOf(paymentType2))
        
        val result = paymentModesService.getPaymentModes()
        
        // Then
        assertTrue(result.containsKey("paymentModes"))
        val paymentModes = result["paymentModes"] as List<Map<String, Any>>
        assertEquals(2, paymentModes.size)
        
        // Check first payment mode
        val firstMode = paymentModes[0]
        assertEquals("UPI", firstMode["modeCode"])
        assertEquals("Unified Payment Interface", firstMode["modeName"])
        assertEquals("Direct bank transfer using UPI", firstMode["description"])
        assertEquals(true, firstMode["isActive"])
        
        // Check payment types of first mode
        val firstModeTypes = firstMode["paymentTypes"] as List<Map<String, Any>>
        assertEquals(1, firstModeTypes.size)
        assertEquals("GOOGLE_PAY", firstModeTypes[0]["typeCode"])
        assertEquals("Google Pay", firstModeTypes[0]["typeName"])
        
        // Check second payment mode
        val secondMode = paymentModes[1]
        assertEquals("CREDIT_CARD", secondMode["modeCode"])
        
        // Check payment types of second mode
        val secondModeTypes = secondMode["paymentTypes"] as List<Map<String, Any>>
        assertEquals(1, secondModeTypes.size)
        assertEquals("VISA", secondModeTypes[0]["typeCode"])
    }
}
