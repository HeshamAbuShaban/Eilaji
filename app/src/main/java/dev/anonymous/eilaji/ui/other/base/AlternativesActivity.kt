package dev.anonymous.eilaji.ui.other.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.storage.enums.FragmentsKeys


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
                    FragmentsKeys.add_address.name -> nonNullNavController.navigate(R.id.navigation_add_address)
                    FragmentsKeys.favorites.name -> nonNullNavController.navigate(R.id.navigation_favorites)
                    FragmentsKeys.medicine.name -> nonNullNavController.navigate(R.id.navigation_medicine)
                    FragmentsKeys.reminder.name -> nonNullNavController.navigate(R.id.navigation_reminders_list)
                    FragmentsKeys.search.name -> nonNullNavController.navigate(R.id.navigation_search)
                    FragmentsKeys.map.name -> nonNullNavController.navigate(R.id.navigation_map)
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