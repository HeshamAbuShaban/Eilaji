package dev.anonymous.eilaji.models

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import android.content.Context
import dev.anonymous.eilaji.ui.user_interface.home.HomeViewModel

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
