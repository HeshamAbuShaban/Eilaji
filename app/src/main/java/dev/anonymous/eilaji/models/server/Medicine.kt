package dev.anonymous.eilaji.models.server

data class Medicine(val id:String, val imageUrl:String, val title:String, val priceString: String, val details: String, val alternativesMedicine: ArrayList<String>, val idCategory:String, val idSubCategory:String, val isFavorite: Boolean){
    constructor() : this("", "", "","0.0","", ArrayList(),"","",false)
    // Secondary constructor for Fire-store deserialization
        val price: Double
        get() = priceString.toDouble()  // Convert the priceString to Double when accessing the price

}
