package dev.anonymous.eilaji.ui.base

import android.content.Intent
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
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity


// This BaseActivity is in charge of the user interface pieces, often known as (the manager of our app screens).
class BaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaseBinding
    private lateinit var baseViewModel: BaseViewModel
    private var menuRes: Int = R.menu.home_menu

    // To be Removed
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

        setSupportActionBar(binding.includeAppBarLayoutBase.toolbarApp)

        //these are some of the main method with high needed functionality
        setupVMWNavController()
        setupActionBarWithNavController()
        setupBottomNavigationView()
    }

    // ************ ~These are some of the primary methods with the most utility.~ ************
    private fun setupVMWNavController() {
        //  initialize the view-model instance
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        //this gives a value for the view model instance type NavController
        baseViewModel.setNavController(findNavController(R.id.nav_host_fragment_activity_base))
    }

    private fun setupActionBarWithNavController() {
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_categories,
                R.id.navigation_profile,
            )
        )
        baseViewModel.navController.observe(this) {
            it?.let { nonNullNavController ->
                setupActionBarWithNavController(nonNullNavController, appBarConfiguration)
            }
        }
    }

    private fun setupBottomNavigationView() {
        baseViewModel.navController.observe(this) { navController ->
            navController?.let { nonNullNavController ->
                /**
                 * @author$hesham_abu_shaban
                 * here, we're linking to the (bottom navigation view) built-in method
                 * of configuring, to work with the navController that stored in the baseViewModel.
                 */
                binding.navView.setupWithNavController(nonNullNavController)
            }
        }
    }


    // ************ ~these are the menu builder methods~ ************
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
            val intent = Intent(this, AlternativesActivity::class.java)
            intent.putExtra("fragmentType", "favorites")
            startActivity(intent)
            viewModelStore
            true
        }

        R.id.pharmacies_map_menu -> {
            showToast("pharmacies_map_menu")
//            baseViewModel.navigateToMap()
            val intent = Intent(this, AlternativesActivity::class.java)
            intent.putExtra("fragmentType", "add_address") // Set the fragment type as "search" or "map"
            startActivity(intent)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }


    // ************ ~these are some lifecycle methods~ ************

    override fun onResume() {
        super.onResume()
//        controller.addOnDestinationChangedListener(destinationChangedListener)
        baseViewModel.isNavControllerAvailable.observe(this) { isAvailable ->
            if (isAvailable) {
                baseViewModel.navController.value?.addOnDestinationChangedListener(
                    destinationChangedListener
                )
            }
        }
    }

    override fun onPause() {
//        controller.removeOnDestinationChangedListener(destinationChangedListener)
        baseViewModel.clearNavController()
        super.onPause()
    }


    // ************ ~these are some helper methods~ ************
    private fun hideSendPrescriptionFab() {
        binding.fabSendPrescription.hide()
    }

    private fun showSendPrescriptionFab() {
        binding.fabSendPrescription.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}

