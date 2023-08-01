package dev.anonymous.eilaji.ui.other.sub_categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.models.server.SubCategory

class SubCategoriesViewModel : ViewModel() {
    private val _subCategoriesList = MutableLiveData<ArrayList<SubCategory>>()
    val subCategoriesList: LiveData<ArrayList<SubCategory>> = _subCategoriesList

    fun setSubCategoriesList(subCategoriesListList: ArrayList<SubCategory>) {
        _subCategoriesList.value = subCategoriesListList
    }
    //--------------------------------------------------
    private val _medicineList = MutableLiveData<ArrayList<Medicine>>()
    val medicineList: LiveData<ArrayList<Medicine>> = _medicineList

    fun setMedicineList(medicineList: ArrayList<Medicine>) {
        _medicineList.value = medicineList
    }
}