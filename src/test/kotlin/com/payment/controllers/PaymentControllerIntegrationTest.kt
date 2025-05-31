package com.payment.controllers

import com.payment.config.TestRedisConfiguration
import com.payment.models.domain.PaymentMode
import com.payment.models.domain.PaymentType
import com.payment.repositories.PaymentModeRepository
import com.payment.repositories.PaymentTypeRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfiguration::class)
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var paymentModeRepository: PaymentModeRepository

    @MockBean
    private lateinit var paymentTypeRepository: PaymentTypeRepository

    @Test
    fun `getPaymentModes should return payment modes`() {
        // Given
        val now = LocalDateTime.now(ZoneOffset.UTC)
        
        val paymentMode = PaymentMode(
            id = 1L,
            modeCode = "UPI",
            modeName = "Unified Payment Interface",
            description = "Direct bank transfer using UPI",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        val paymentType = PaymentType(
            id = 1L,
            modeId = 1L,
            typeCode = "GOOGLE_PAY",
            typeName = "Google Pay",
            description = "Pay using Google Pay UPI",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        // When
        `when`(paymentModeRepository.findAllActive()).thenReturn(listOf(paymentMode))
        `when`(paymentTypeRepository.findByModeId(1L)).thenReturn(listOf(paymentType))
        
        // Then
        mockMvc.perform(
            get("/api/v1/payment/modes")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.paymentModes").isArray)
            .andExpect(jsonPath("$.paymentModes[0].modeCode").value("UPI"))
            .andExpect(jsonPath("$.paymentModes[0].modeName").value("Unified Payment Interface"))
            .andExpect(jsonPath("$.paymentModes[0].paymentTypes").isArray)
            .andExpect(jsonPath("$.paymentModes[0].paymentTypes[0].typeCode").value("GOOGLE_PAY"))
    }
}
