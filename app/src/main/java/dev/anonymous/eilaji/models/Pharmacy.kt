package dev.anonymous.eilaji.models

data class Pharmacy(
    var pharmacy_image_url: String,
    var name: String,
    var doctorsNames: ArrayList<String>,
    var phone: String,
    var address: String,
    var lat: Double,
    var lng: Double
){
    constructor() : this("", "", ArrayList(),"","",0.0,0.0)
    // Secondary constructor for Fire-store deserialization
}