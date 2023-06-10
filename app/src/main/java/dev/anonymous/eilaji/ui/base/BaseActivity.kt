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
    private lateinit var viewModel: BaseViewModel
    private var menuRes: Int = R.menu.home_menu

    private lateinit var controller: NavController
    private val listener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            menuRes = when (destination.id) {
                R.id.navigation_home -> {
                    binding.fabSendPrescription.hide()
                    R.menu.home_menu
                }

                R.id.navigation_categories -> {
                    binding.fabSendPrescription.hide()
                    R.menu.category_menu
                }

                R.id.navigation_notifications -> {
                    binding.fabSendPrescription.show()
                    0
                }

                else -> {
                    binding.fabSendPrescription.hide()
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
        viewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        viewModel.setNavController(findNavController(R.id.nav_host_fragment_activity_base))
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_base)
        viewModel.setNavController(navController)

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
        viewModel.navController.observe(this) { navController ->
            binding.navView.setupWithNavController(navController)
            controller = navController
        }

//        val navigationActions: MutableMap<Int, () -> Unit> = HashMap()
//        navigationActions[R.id.navigation_home] = {
//            viewModel.onHomeNavigationSelected()
//        }
//        navigationActions[R.id.navigation_dashboard] = {
//            viewModel.onDashboardNavigationSelected()
//        }
//        navigationActions[R.id.navigation_notifications] = {
//            viewModel.onNotificationsNavigationSelected()
//        }
//        navigationActions[R.id.navigation_profile] = {
//            viewModel.onProfileNavigationSelected()
//        }
//        navigationActions[R.id.navigation_categories] = {
//            viewModel.onCategoriesNavigationSelected()
//        }
//
//        binding.navView.setOnItemSelectedListener { item ->
//            val itemId = item.itemId
//            if (navigationActions.containsKey(itemId)) {
//                navigationActions[itemId]
//                true
//            } else {
//                false
//            }
//        }
//
//        // Set the default selected item
//        binding.navView.selectedItemId = R.id.navigation_home
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (menuRes != 0) {
            menuInflater.inflate(menuRes, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_menu -> {
                Toast.makeText(applicationContext, "search_menu", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.notification_menu -> {
                Toast.makeText(applicationContext, "notification_menu", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.pharmacies_map_menu -> {
                Toast.makeText(applicationContext, "pharmacies_map_menu", Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        controller.addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        controller.removeOnDestinationChangedListener(listener)
        super.onPause()
    }
}
