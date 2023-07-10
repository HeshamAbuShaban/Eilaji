package dev.anonymous.eilaji.models.server

data class Ad(val id: String, val imageUrl: String, val title: String) {
    constructor() : this("", "", "")
    // Secondary constructor for Fire-store deserialization
}