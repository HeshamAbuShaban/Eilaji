package dev.anonymous.eilaji.ui.other.sub_categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.SubcategoryDto

class SubCategoriesViewModel : ViewModel() {
    private val _subCategoriesList = MutableLiveData<List<SubcategoryDto>>()
    val subCategoriesList: LiveData<List<SubcategoryDto>> = _subCategoriesList

    fun setSubCategoriesList(subCategoriesList: List<SubcategoryDto>) {
        _subCategoriesList.value = subCategoriesList
    }

    private val _medicineList = MutableLiveData<List<MedicineDto>>()
    val medicineList: LiveData<List<MedicineDto>> = _medicineList

    fun setMedicineList(medicineList: List<MedicineDto>) {
        _medicineList.value = medicineList
    }
}
