package dev.anonymous.eilaji.models

data class PharmacyModel(
    var image: Int,
    var name: String,
    var doctorsNames: ArrayList<String>,
    var phone: String,
    var address: String,
    var lat: Double,
    var lng: Double
)