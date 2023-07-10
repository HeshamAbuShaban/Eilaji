package dev.anonymous.eilaji.ui.base.user_interface.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.server.Ad

class HomeViewModel : ViewModel() {
    private val _adsList = MutableLiveData<ArrayList<Ad>>()
    val adsList: LiveData<ArrayList<Ad>> = _adsList

    fun setAdsList(adsList: ArrayList<Ad>) {
        _adsList.value = adsList
    }
}



