package com.eilaji.backend.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SecurityUtilsTest {

    @Test
    fun `validateEmail accepts valid emails`() {
        assertTrue(SecurityUtils.validateEmail("test@example.com"))
        assertTrue(SecurityUtils.validateEmail("user.name+tag@example.co.uk"))
        assertTrue(SecurityUtils.validateEmail("user123@test.org"))
        assertTrue(SecurityUtils.validateEmail("a@b"))
    }

    @Test
    fun `validateEmail rejects invalid emails`() {
        assertFalse(SecurityUtils.validateEmail("invalid"))
        assertFalse(SecurityUtils.validateEmail("invalid@"))
        assertFalse(SecurityUtils.validateEmail("@example.com"))
        assertFalse(SecurityUtils.validateEmail(""))
        assertFalse(SecurityUtils.validateEmail("test@"))
    }

    @Test
    fun `validatePasswordStrength returns empty for strong password`() {
        val errors = SecurityUtils.validatePasswordStrength("StrongPass123!")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validatePasswordStrength reports all weaknesses for weak password`() {
        val errors = SecurityUtils.validatePasswordStrength("short")
        assertTrue(errors.contains("Password must be at least 12 characters long"))
        assertTrue(errors.contains("Password must contain at least one uppercase letter"))
        assertTrue(errors.contains("Password must contain at least one number"))
        assertTrue(errors.contains("Password must contain at least one special character"))
    }

    @Test
    fun `validatePasswordStrength detects missing uppercase`() {
        val errors = SecurityUtils.validatePasswordStrength("alllowercase1!")
        assertTrue(errors.contains("Password must contain at least one uppercase letter"))
        assertFalse(errors.contains("Password must contain at least one lowercase letter"))
    }

    @Test
    fun `validatePasswordStrength detects missing lowercase`() {
        val errors = SecurityUtils.validatePasswordStrength("ALLUPPERCASE1!")
        assertTrue(errors.contains("Password must contain at least one lowercase letter"))
    }

    @Test
    fun `validatePasswordStrength detects missing digit`() {
        val errors = SecurityUtils.validatePasswordStrength("NoDigitsHere!!")
        assertTrue(errors.contains("Password must contain at least one number"))
    }

    @Test
    fun `validatePasswordStrength detects missing special character`() {
        val errors = SecurityUtils.validatePasswordStrength("NoSpecial123Aa")
        assertTrue(errors.contains("Password must contain at least one special character"))
    }

    @Test
    fun `validatePasswordStrength detects too short even if other criteria met`() {
        val errors = SecurityUtils.validatePasswordStrength("Aa1!")
        assertTrue(errors.contains("Password must be at least 12 characters long"))
    }

    @Test
    fun `isValidUuid accepts valid UUIDs`() {
        val uuid = UUID.randomUUID().toString()
        assertTrue(SecurityUtils.isValidUuid(uuid))
        assertTrue(SecurityUtils.isValidUuid(uuid.uppercase()))
        assertTrue(SecurityUtils.isValidUuid("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(SecurityUtils.isValidUuid("123E4567-E89B-12D3-A456-426614174000"))
    }

    @Test
    fun `isValidUuid rejects invalid UUIDs`() {
        assertFalse(SecurityUtils.isValidUuid("not-a-uuid"))
        assertFalse(SecurityUtils.isValidUuid(""))
        assertFalse(SecurityUtils.isValidUuid("550e8400-e29b-41d4-a716-44665544000"))
        assertFalse(SecurityUtils.isValidUuid("550e8400-e29b-41d4-a716-44665544000g"))
        assertFalse(SecurityUtils.isValidUuid("123"))
        assertFalse(SecurityUtils.isValidUuid("550e8400e29b41d4a716446655440000"))
    }

    @Test
    fun `limitLength returns input when within limit`() {
        assertEquals("hello", SecurityUtils.limitLength("hello", 10))
        assertEquals("hello", SecurityUtils.limitLength("hello", 5))
        assertEquals("", SecurityUtils.limitLength("", 5))
    }

    @Test
    fun `limitLength throws when exceeds max`() {
        assertThrows(IllegalArgumentException::class.java) {
            SecurityUtils.limitLength("hello world", 5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SecurityUtils.limitLength("a".repeat(101), 100)
        }
    }

    @Test
    fun `limitLength uses fieldName in error message`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SecurityUtils.limitLength("toolong", 3, "email")
        }
        assertTrue(ex.message!!.contains("email"))
        assertTrue(ex.message!!.contains("3"))
    }
}
