package dev.anonymous.eilaji.models.server

data class SubCategory(val id: String, val idCategory: String, val imageUrl: String, val title: String){
    constructor() : this("", "", "","")
    // Secondary constructor for Fire-store deserialization
}