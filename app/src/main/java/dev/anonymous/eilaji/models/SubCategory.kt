package dev.anonymous.eilaji.models

data class SubCategory(
    var id: String,
    var imageUrl: String,
    var title: String,
    var categoryId: String,
) {
    constructor() : this("", "", "", "")
}
