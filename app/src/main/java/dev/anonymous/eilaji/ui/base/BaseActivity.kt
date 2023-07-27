package dev.anonymous.eilaji.ui.base

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Explode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ActivityBaseBinding
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity


class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    private lateinit var baseViewModel: BaseViewModel

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
        val navController = findNavController(R.id.nav_host_fragment_activity_base)
        //this gives a value for the view model instance type NavController
        baseViewModel.setNavController(navController)

    }

    private fun setupActionBarWithNavController() {
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_chatting,
                R.id.navigation_send_prescription,
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
        baseViewModel.navController.value?.let { nonNullNavController ->
            /**
             * @author$hesham_abu_shaban
             * here, we're linking to the (bottom navigation view) built-in method
             * of configuring, to work with the navController that stored in the baseViewModel.
             */
            binding.navView.setupWithNavController(nonNullNavController)
        }
    }

    // ************ ~these are the menu builder methods~ ************
    private fun updateToolbarMenu() {
        val toolbar = binding.includeAppBarLayoutBase.toolbarApp
        baseViewModel.navController.value?.let { nonNullNavController ->
            // Update toolbar menu based on the selected bottom navigation item
            nonNullNavController.addOnDestinationChangedListener { _, destination, _ ->
                val menuResource = when (destination.id) {
                    R.id.navigation_home -> R.menu.home_menu
                    R.id.navigation_categories -> R.menu.category_menu
                    // Add more destinations and their associated menu resources here
                    else -> 0
                }
                toolbar.menu.clear()
                if (menuResource != 0) {
                    toolbar.inflateMenu(menuResource)
                }
            }

            // Handle bottom navigation item selection
            binding.navView.setOnItemSelectedListener { menuItem ->
                val handled =
                    NavigationUI.onNavDestinationSelected(menuItem, nonNullNavController)
                if (handled) {
                    invalidateOptionsMenu()
                }
                handled
            }


        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.empty_menu, menu) // Inflate an empty menu
        updateToolbarMenu() // Update the toolbar menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.search_menu_item -> {
            showToast("search_menu")
            with(window){
                enterTransition = Explode()
                exitTransition = Explode()
            }

            val intent = Intent(this@BaseActivity, AlternativesActivity::class.java)
            intent.putExtra(
                "fragmentType",
                FragmentsKeys.search.name
            ) // Set the fragment type as "search" or "map"
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@BaseActivity).toBundle())
            true
        }

        R.id.notification_menu_item -> {
            showToast("notification_menu")
            val intent = Intent(this, AlternativesActivity::class.java)
            intent.putExtra(
                "fragmentType",
                FragmentsKeys.reminder.name
            ) // Set the fragment type as "search" or "map"
            startActivity(intent)
            true
        }

        R.id.pharmacies_map_menu_item -> {
            showToast("pharmacies_map_menu")
            val intent = Intent(this, AlternativesActivity::class.java)
            /*intent.putExtra(
                "fragmentType",
                FragmentsKeys.add_address.name
            )*/
            intent.putExtra(
                "fragmentType",
                FragmentsKeys.map.name
            )
            startActivity(intent)
            true
        }

        R.id.share_menu_item -> {
            showToast("share_menu_item")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
                putExtra(Intent.EXTRA_TEXT, "I found this amazing app that I wanted to share with you. Download it from [app store link].")
            }

            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(shareIntent, "Share the app"))
            } else {
                Toast.makeText(this@BaseActivity, "No app found to share", Toast.LENGTH_SHORT)
                    .show()
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }


    // ************ ~these are some lifecycle methods~ ************

    /*override fun onResume() {
        super.onResume()
        baseViewModel.isNavControllerAvailable.observe(this) { isAvailable ->
            if (isAvailable) {
                baseViewModel.navController.value?.addOnDestinationChangedListener(
                    destinationChangedListener
                )
            }
        }
    }*/

    /*override fun onPause() {
//        baseViewModel.clearNavController()
        super.onPause()
    }*/


    // ************ ~these are some helper methods~ ************

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}

