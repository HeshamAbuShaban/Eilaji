package com.eilaji.backend.security

import java.util.regex.Pattern

object SecurityUtils {
    // Regex patterns for input validation
    private val VALID_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$")
    private val VALID_PATH_SEGMENT = Pattern.compile("^[a-zA-Z0-9._-]+$")
    private val VALID_EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")
    private val VALID_PHONE = Pattern.compile("^[0-9+\\-()\\s]+$")
    private val SQL_KEYWORDS = Pattern.compile("(?i)(drop|delete|insert|update|select|union|alter|create|truncate)")

    /**
     * Sanitize filename to prevent path traversal attacks
     */
    fun sanitizeFilename(filename: String): String {
        if (filename.isBlank()) {
            throw IllegalArgumentException("Filename cannot be blank")
        }

        // Remove any path traversal sequences
        var sanitized = filename.replace("../", "")
        sanitized = sanitized.replace("..\\\\", "")
        sanitized = sanitized.replace("/", "")
        sanitized = sanitized.replace("\\\\", "")
        sanitized = sanitized.replace("%00", "")

        // Validate against allowed characters
        if (!VALID_FILENAME.matcher(sanitized).matches()) {
            throw IllegalArgumentException("Invalid filename: $filename")
        }

        return sanitized
    }

    /**
     * Validate path segment (for paths, IDs, etc.)
     */
    fun validatePathSegment(segment: String, fieldName: String = "path segment"): Boolean {
        if (segment.isBlank()) {
            throw IllegalArgumentException("$fieldName cannot be blank")
        }
        if (segment.contains("../") || segment.contains("..\\\\")) {
            throw IllegalArgumentException("Path traversal detected in $fieldName")
        }
        return VALID_PATH_SEGMENT.matcher(segment).matches()
    }

    /**
     * Validate email format
     */
    fun validateEmail(email: String): Boolean {
        return VALID_EMAIL.matcher(email).matches()
    }

    /**
     * Validate phone number format
     */
    fun validatePhone(phone: String): Boolean {
        return VALID_PHONE.matcher(phone).matches()
    }

    /**
     * Check for potential SQL injection patterns
     */
    fun containsSqlInjection(input: String): Boolean {
        return SQL_KEYWORDS.matcher(input).find()
    }

    /**
     * Sanitize user input to prevent XSS (for HTML output)
     */
    fun sanitizeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }

    /**
     * Validate UUID format
     */
    fun isValidUuid(uuid: String): Boolean {
        return uuid.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
    }

    /**
     * Limit string length to prevent buffer overflow
     */
    fun limitLength(input: String, maxLength: Int, fieldName: String = "input"): String {
        if (input.length > maxLength) {
            throw IllegalArgumentException("$fieldName exceeds maximum length of $maxLength characters")
        }
        return input.take(maxLength)
    }

    /**
     * Validate password strength
     */
    fun validatePasswordStrength(password: String): List<String> {
        val errors = mutableListOf<String>()

        if (password.length < 12) {
            errors.add("Password must be at least 12 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain at least one special character")
        }

        return errors
    }

    /**
     * Check if password is in common password list (basic check)
     */
    fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "123456", "123456789", "qwerty", "abc123",
            "password1", "12345678", "111111", "1234567890", "1234567"
        )
        return commonPasswords.contains(password.lowercase())
    }
}