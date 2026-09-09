package com.eilaji.backend.service

import com.eilaji.backend.dto.OrderDto
import com.eilaji.backend.security.SecurityUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OrderServiceTest {

    @Test
    fun `order ids are String UUIDs and validate correctly`() {
        val orderId = UUID.randomUUID().toString()
        val prescriptionId = UUID.randomUUID().toString()
        val patientId = UUID.randomUUID().toString()
        val pharmacyId = UUID.randomUUID().toString()
        assertTrue(SecurityUtils.isValidUuid(orderId))
        assertTrue(SecurityUtils.isValidUuid(prescriptionId))
        assertTrue(SecurityUtils.isValidUuid(patientId))
        assertTrue(SecurityUtils.isValidUuid(pharmacyId))
        val dto = OrderDto(
            id = orderId,
            prescriptionId = prescriptionId,
            patientId = patientId,
            pharmacyId = pharmacyId,
            pharmacyName = "Test Pharmacy",
            status = "PENDING",
            totalAmount = 100.0,
            paymentMethod = "CASH",
            paymentStatus = "PENDING",
            deliveryAddress = "123 Main St",
            deliveryNotes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        assertEquals(orderId, dto.id)
        assertTrue(dto.id is String)
    }

    @Test
    fun `invalid order id fails UUID validation`() {
        assertTrue(!SecurityUtils.isValidUuid("not-a-uuid"))
        assertTrue(!SecurityUtils.isValidUuid("12345"))
    }
}
