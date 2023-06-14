package dev.anonymous.eilaji.ui.other.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class AlternativesViewModel:ViewModel() {

    private val _navController = MutableLiveData<NavController?>()

    val navController: LiveData<NavController?> = _navController

    fun setNavController(navController: NavController) {
        _navController.value = navController
    }

    /*private fun navigateToDestination(destinationId: Int) {
        _navController.value?.navigate(destinationId)
    }*/

    // STOPSHIP: Todo try to use these later on
    /*private fun navigateToDestination2(destinationId: Int) {
        navController.value?.navigate(destinationId)
    }

    fun navigateToSearchFragment() {
        Log.d(TAG, "navigateToSearchFragment() called")
        navigateToDestination(R.id.navigation_search)
    }*/

}