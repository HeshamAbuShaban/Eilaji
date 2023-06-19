package dev.anonymous.eilaji.ui.other.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import dev.anonymous.eilaji.R


class AlternativesActivity : AppCompatActivity() {
    private lateinit var alternativesViewModel: AlternativesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alternatives)

        setupViewModel()
        decideWhichScreen()
    }

    private fun decideWhichScreen() {
        alternativesViewModel.navController.observe(this) {
            it?.let { nonNullNavController ->
                when (intent.getStringExtra("fragmentType")) {
                    "add_address" -> nonNullNavController.navigate(R.id.navigation_add_address)
                    "favorites" -> nonNullNavController.navigate(R.id.navigation_favorites)
                    "medicine" -> nonNullNavController.navigate(R.id.navigation_medicine)
                    "search" -> nonNullNavController.navigate(R.id.navigation_search)
                    "map" -> nonNullNavController.navigate(R.id.navigation_map)
                }
            }
        }
    }

    private fun setupViewModel() {
        alternativesViewModel = ViewModelProvider(this)[AlternativesViewModel::class.java]
        val navController: NavController =
            findNavController(R.id.nav_host_fragment_activity_alternatives)
        alternativesViewModel.setNavController(navController)
    }
}