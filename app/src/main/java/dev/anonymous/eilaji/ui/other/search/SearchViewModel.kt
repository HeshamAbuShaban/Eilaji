package dev.anonymous.eilaji.ui.other.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.server.Medicine

class SearchViewModel: ViewModel() {
    private val _medicinesData  = MutableLiveData<ArrayList<Medicine>>()
    val medicineData : LiveData<ArrayList<Medicine>> = _medicinesData
    fun setMedicinesData(medicinesData: ArrayList<Medicine>) {
        _medicinesData.value = medicinesData
    }
    // -------------------------------------------------------------
    private val _pharmaciesData  = MutableLiveData<ArrayList<Pharmacy>>()
    val pharmaciesData : LiveData<ArrayList<Pharmacy>> = _pharmaciesData
    fun setPharmaciesDataData(pharmaciesData: ArrayList<Pharmacy>) {
        _pharmaciesData.value = pharmaciesData
    }

}