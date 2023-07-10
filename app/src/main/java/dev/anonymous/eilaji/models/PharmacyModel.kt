package dev.anonymous.eilaji.models

data class PharmacyModel(
    var uid: String,
    var imageUrl: String,
    var name: String,
    var phone: String,
    var address: String,
    var lat: Double,
    var lng: Double,
    var token: String
)