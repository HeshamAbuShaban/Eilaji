package com.eilaji.backend.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteHelpersTest {

    @Test
    fun `escapeLike neutralises wildcards`() {
        // A user searching for "100%" must not turn it into a match-everything pattern.
        assertEquals("%100\\%%", escapeLike("100%"))
        assertEquals("%a\\_b%", escapeLike("a_b"))
        assertEquals("%para%", escapeLike("para"))
        assertEquals("", escapeLike("   ").replace("%", ""))
    }

    @Test
    fun `escapeLike bounds the query length`() {
        val pattern = escapeLike("a".repeat(500))
        assertTrue(pattern.length <= 66)
    }

    @Test
    fun `bounding box covers the radius and stays ordered`() {
        val box = boundingBox(31.5, 34.47, 10.0)
        assertTrue(box.minLat < 31.5 && 31.5 < box.maxLat)
        assertTrue(box.minLng < 34.47 && 34.47 < box.maxLng)
        // ~10km around Gaza is roughly 0.09 degrees of latitude.
        assertTrue(kotlin.math.abs(box.maxLat - box.minLat) < 0.25)
    }

    @Test
    fun `webhook signature verifies only for a matching body`() {
        val secret = "webhook-secret-at-least-32-characters-long"
        val body = """{"prescriptionId":"abc","quotedPrice":12.5}"""
        val signature = signWebhookBody(secret, body)
        assertTrue(verifyWebhookSignature(secret, body, signature))
        assertFalse(verifyWebhookSignature(secret, body + " ", signature))
        assertFalse(verifyWebhookSignature("another-secret-of-sufficient-length-32", body, signature))
        assertFalse(verifyWebhookSignature(secret, body, null))
        assertFalse(verifyWebhookSignature(secret, body, ""))
        assertFalse(verifyWebhookSignature("", body, signature))
    }
}
