package dev.anonymous.eilaji.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ActivityBaseBinding


// This BaseActivity is in charge of the user interface pieces, often known as (the manager of our app screens).
class BaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaseBinding
    private lateinit var baseViewModel: BaseViewModel
    private var menuRes: Int = R.menu.home_menu

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            menuRes = when (destination.id) {
                R.id.navigation_home -> {
                    hideSendPrescriptionFab()
                    R.menu.home_menu
                }

                R.id.navigation_categories -> {
                    hideSendPrescriptionFab()
                    R.menu.category_menu
                }

                R.id.navigation_notifications -> {
                    showSendPrescriptionFab()
                    0
                }

                else -> {
                    hideSendPrescriptionFab()
                    0
                }
            }
            invalidateOptionsMenu()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupViewModel()
        setupNavigation()
        setupBottomNavigationView()
    }

    private fun setupViewModel() {
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        baseViewModel.setNavController(findNavController(R.id.nav_host_fragment_activity_base))
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_base)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_categories,
                R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupBottomNavigationView() {
        baseViewModel.navController.observe(this) { navController ->
            navController?.let { nonNullNavController ->
                binding.navView.setupWithNavController(nonNullNavController)
            }
        }
    }

    private fun hideSendPrescriptionFab() {
        binding.fabSendPrescription.hide()
    }

    private fun showSendPrescriptionFab() {
        binding.fabSendPrescription.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (menuRes != 0) {
            menuInflater.inflate(menuRes, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.search_menu -> {
            showToast("search_menu")
            true
        }

        R.id.notification_menu -> {
            showToast("notification_menu")
            true
        }

        R.id.pharmacies_map_menu -> {
            showToast("pharmacies_map_menu")
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
//        controller.addOnDestinationChangedListener(destinationChangedListener)
        baseViewModel.isNavControllerAvailable.observe(this) { isAvailable ->
            if (isAvailable) {
                baseViewModel.navController.value?.addOnDestinationChangedListener(destinationChangedListener)
            }
        }
    }

    override fun onPause() {
//        controller.removeOnDestinationChangedListener(destinationChangedListener)
        baseViewModel.clearNavController()
        super.onPause()
    }
}

