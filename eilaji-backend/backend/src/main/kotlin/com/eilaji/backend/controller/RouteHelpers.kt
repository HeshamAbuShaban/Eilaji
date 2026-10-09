package com.eilaji.backend.controller

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Escapes LIKE wildcards so user input cannot force a sequential scan. */
internal fun escapeLike(value: String): String {
    val escaped = value.trim().take(64)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}

/**
 * Bounding box around (lat, lng) for `radiusKm`. Filtering inside this box in SQL keeps the
 * candidate set small; the exact distance is then computed for those rows only.
 */
internal fun boundingBox(lat: Double, lng: Double, radiusKm: Double): BoundingBox {
    val latDelta = radiusKm / 110.574
    val lngDelta = radiusKm / (111.320 * Math.cos(Math.toRadians(lat)).coerceAtLeast(0.000001))
    return BoundingBox(lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta)
}

internal data class BoundingBox(val minLat: Double, val maxLat: Double, val minLng: Double, val maxLng: Double)

/**
 * Signs an outbound webhook body the same way the inbound endpoint verifies it.
 * Format: "sha256=" + hex(hmac_sha256(secret, rawBody))
 */
internal fun signWebhookBody(secret: String, body: String): String {
    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
    mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return "sha256=" + mac.doFinal(body.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/** Constant-time compare of a client signature against the expected value. */
internal fun verifyWebhookSignature(secret: String, body: String, signature: String?): Boolean {
    if (signature.isNullOrBlank() || secret.isBlank()) return false
    val expected = signWebhookBody(secret, body).toByteArray(Charsets.UTF_8)
    val provided = signature.toByteArray(Charsets.UTF_8)
    return MessageDigest.isEqual(expected, provided)
}

