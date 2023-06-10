package dev.anonymous.eilaji.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class BaseViewModel : ViewModel() {

    private val _navController = MutableLiveData<NavController?>()
    val navController: LiveData<NavController?> = _navController

    private val _isNavControllerAvailable = MutableLiveData<Boolean>()
    val isNavControllerAvailable: LiveData<Boolean> = _isNavControllerAvailable

    fun setNavController(navController: NavController) {
        _navController.value = navController
        _isNavControllerAvailable.value = true
    }

    fun clearNavController() {
        _navController.value = null
        _isNavControllerAvailable.value = false
    }

}
