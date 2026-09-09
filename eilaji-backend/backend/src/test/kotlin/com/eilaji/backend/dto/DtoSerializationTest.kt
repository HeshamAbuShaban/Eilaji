package com.eilaji.backend.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `UserDto serializes and deserializes with String UUID`() {
        val id = UUID.randomUUID().toString()
        val original = UserDto(
            id = id,
            email = "test@example.com",
            fullName = "Test User",
            phone = "+123456789",
            avatarUrl = null,
            role = "PATIENT",
            isVerified = true,
            createdAt = "2026-01-01T00:00:00Z"
        )
        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"$id\""))
        assertTrue(encoded.contains("test@example.com"))
        val decoded = json.decodeFromString<UserDto>(encoded)
        assertEquals(original, decoded)
        assertEquals(id, decoded.id)
        assertTrue(decoded.id is String)
    }

    @Test
    fun `MedicineDto serializes and deserializes with String UUID`() {
        val id = UUID.randomUUID().toString()
        val original = MedicineDto(
            id = id,
            titleAr = "دواء",
            titleEn = "Medicine",
            descriptionAr = "وصف",
            descriptionEn = "Description",
            imageUrl = "https://example.com/img.png",
            price = 12.5,
            manufacturer = "Pharma Inc",
            requiresPrescription = true,
            isActive = true,
            subcategoryNameAr = "فئة",
            subcategoryNameEn = "Category"
        )
        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"$id\""))
        val decoded = json.decodeFromString<MedicineDto>(encoded)
        assertEquals(original, decoded)
        assertEquals(id, decoded.id)
    }

    @Test
    fun `PrescriptionDto serializes and deserializes with String UUIDs`() {
        val id = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val pharmacyId = UUID.randomUUID().toString()
        val original = PrescriptionDto(
            id = id,
            userId = userId,
            pharmacyId = pharmacyId,
            pharmacyName = "My Pharmacy",
            imageUrl = "https://example.com/pres.png",
            notes = "Take care",
            status = "PENDING",
            quotedPrice = 99.99,
            pharmacistNotes = null,
            eilajiPlusRef = null,
            eilajiPlusStatus = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )
        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"$id\""))
        assertTrue(encoded.contains("\"$userId\""))
        assertTrue(encoded.contains("\"$pharmacyId\""))
        val decoded = json.decodeFromString<PrescriptionDto>(encoded)
        assertEquals(original, decoded)
        assertEquals(id, decoded.id)
        assertEquals(userId, decoded.userId)
        assertEquals(pharmacyId, decoded.pharmacyId)
    }

    @Test
    fun `PrescriptionDto handles null pharmacyId`() {
        val original = PrescriptionDto(
            id = UUID.randomUUID().toString(),
            userId = UUID.randomUUID().toString(),
            pharmacyId = null,
            pharmacyName = null,
            imageUrl = "https://example.com/pres2.png",
            notes = null,
            status = "PENDING",
            quotedPrice = null,
            pharmacistNotes = null,
            eilajiPlusRef = null,
            eilajiPlusStatus = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PrescriptionDto>(encoded)
        assertEquals(original, decoded)
        assertEquals(null, decoded.pharmacyId)
    }

    @Test
    fun `MedicineDto handles null optional fields`() {
        val id = UUID.randomUUID().toString()
        val original = MedicineDto(
            id = id,
            titleAr = "دواء",
            titleEn = "Medicine",
            descriptionAr = null,
            descriptionEn = null,
            imageUrl = null,
            price = null,
            manufacturer = null,
            requiresPrescription = false,
            isActive = true
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MedicineDto>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `OrderDto serializes with String UUID IDs`() {
        val original = OrderDto(
            id = UUID.randomUUID().toString(),
            prescriptionId = UUID.randomUUID().toString(),
            patientId = UUID.randomUUID().toString(),
            pharmacyId = UUID.randomUUID().toString(),
            pharmacyName = "Pharmacy",
            status = "PENDING",
            totalAmount = 50.0,
            paymentMethod = "CASH",
            paymentStatus = "PENDING",
            deliveryAddress = "123 Street",
            deliveryNotes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<OrderDto>(encoded)
        assertEquals(original, decoded)
        assertNotNull(decoded.id)
    }

    @Test
    fun `UserDto id remains String after roundtrip not numeric`() {
        val id = UUID.randomUUID().toString()
        val dto = UserDto(id, "a@b.com", "Name", null, null, "PATIENT", false, "2026-01-01T00:00:00Z")
        val jsonStr = json.encodeToString(dto)
        assertTrue(jsonStr.contains("\"id\":\"$id\"") || jsonStr.contains("\"id\": \"$id\"") || jsonStr.contains(id))
        val decoded = json.decodeFromString<UserDto>(jsonStr)
        assertTrue(decoded.id is String)
        assertEquals(id, decoded.id)
    }
}
