package dev.anonymous.eilaji.models

data class Pharmacy(
    var uid: String,
    var imageUrl: String,
    var name: String,
    var phone: String,
    var address: String,
    var lat: Double,
    var lng: Double,
    var token: String
) {
    constructor() : this("", "", "", "", "", 0.0, 0.0, "")
    // Secondary constructor for Fire-store deserialization
}