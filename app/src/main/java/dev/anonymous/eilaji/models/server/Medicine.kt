package dev.anonymous.eilaji.models.server

data class Medicine(val id:String, val imageUrl:String, val title:String, val price:Double, val details: String, val alternativesMedicine: ArrayList<String>, val idCategory:String, val idSubCategory:String, val isFavorite: Boolean){
    constructor() : this("", "", "",0.0,"", ArrayList(),"","",false)
    // Secondary constructor for Fire-store deserialization
}
