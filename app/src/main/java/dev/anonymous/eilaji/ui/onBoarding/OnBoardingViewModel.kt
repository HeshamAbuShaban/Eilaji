package dev.anonymous.eilaji.ui.onBoarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OnBoardingViewModel : ViewModel() {
    private val _currentPage = MutableLiveData<Int>()
    val currentPage: LiveData<Int> get() = _currentPage

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> get() = _navigateToLogin

    var previousPage = 0

    fun setCurrentPage(position: Int) {
        _currentPage.value = position
    }

    fun onNextButtonClicked() {
        _navigateToLogin.value = true
    }
}