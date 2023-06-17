package dev.anonymous.eilaji.models

class MedicineModel(
    var id: String,
    var image: Int,
    var name: String,
    var price: Double,
    var details: String,
    var ratings: RatingsModel,
    var alternativeMedicinesId: ArrayList<String>,
    var isFavorite: Boolean
)

