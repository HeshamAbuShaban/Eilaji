package dev.anonymous.eilaji.ui.base.user_interface.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.MedicineDto

class HomeViewModel : ViewModel() {
    private val _adsList = MutableLiveData<ArrayList<Ad>>()
    val adsList: LiveData<ArrayList<Ad>> = _adsList
    fun setAdsList(adsList: ArrayList<Ad>) { _adsList.value = adsList }

    private val _categories = MutableLiveData<List<CategoryDto>>()
    val categories: LiveData<List<CategoryDto>> = _categories
    fun setCategories(list: List<CategoryDto>) { _categories.value = list }

    private val _bestSellers = MutableLiveData<List<MedicineDto>>()
    val bestSellers: LiveData<List<MedicineDto>> = _bestSellers
    fun setBestSellers(list: List<MedicineDto>) { _bestSellers.value = list }

    private val _nearbyPharmacies = MutableLiveData<List<dev.anonymous.eilaji.network.PharmacyDto>>()
    val nearbyPharmacies: LiveData<List<dev.anonymous.eilaji.network.PharmacyDto>> = _nearbyPharmacies
    fun setNearbyPharmacies(list: List<dev.anonymous.eilaji.network.PharmacyDto>) { _nearbyPharmacies.value = list }
}



