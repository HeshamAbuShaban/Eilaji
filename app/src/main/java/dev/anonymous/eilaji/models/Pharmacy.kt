package dev.anonymous.eilaji.models

data class Pharmacy(
    var uid: String,
    var pharmacy_image_url: String,
    var pharmacy_name: String,
    var phone: String,
    var address: String,
    var lat: Double,
    var lng: Double,
    var token: String,
    var ratingAvg: Double = 0.0,
    var totalRatings: Int = 0,
    var isOpen: Boolean = true,
    var distanceKm: Double? = null
) {
    constructor() : this("", "", "", "", "", 0.0, 0.0, "")
    // Secondary constructor for Fire-store deserialization
}