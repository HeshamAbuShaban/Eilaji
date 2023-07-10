package dev.anonymous.eilaji.models.server

data class Category(val id: String, val imageUrl: String, val title: String) {
    constructor() : this("", "", "")
    // Secondary constructor for Fire-store deserialization
}
