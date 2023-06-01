package dev.anonymous.eilaji.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dev.anonymous.eilaji.R

class BaseViewModel : ViewModel() {

    private val _navController = MutableLiveData<NavController>()
    val navController: LiveData<NavController> = _navController

    fun setNavController(navController: NavController) {
        _navController.value = navController
    }

    fun onHomeNavigationSelected() {
        navigateToDestination(R.id.navigation_home)
    }

    fun onDashboardNavigationSelected() {
        navigateToDestination(R.id.navigation_dashboard)
    }

    fun onNotificationsNavigationSelected() {
        navigateToDestination(R.id.navigation_notifications)
    }

    private fun navigateToDestination(destinationId: Int) {
        _navController.value?.navigate(destinationId)
    }
}
