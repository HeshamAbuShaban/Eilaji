package dev.anonymous.eilaji.ui.base.user_interface.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.server.Category

class CategoriesViewModel: ViewModel() {

    private val _categoryList = MutableLiveData<ArrayList<Category>>()
    val categoryList: LiveData<ArrayList<Category>> = _categoryList

    fun setCategoryList(categoryList: ArrayList<Category>) {
        _categoryList.value = categoryList
    }
}