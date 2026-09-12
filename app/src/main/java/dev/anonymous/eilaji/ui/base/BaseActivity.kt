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
import dev.anonymous.eilaji.data.repository.CartRepository
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
        setupVMWNavController()
        setupActionBarWithNavController()
        setupBottomNavigationView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        refreshCartBadge()
    }

    private fun setupVMWNavController() {
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        val navController = findNavController(R.id.nav_host_fragment_activity_base)
        baseViewModel.setNavController(navController)
    }

    private fun setupActionBarWithNavController() {
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_categories,
                R.id.navigation_chatting,
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
        val navController = findNavController(R.id.nav_host_fragment_activity_base)
        binding.navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, _, _ -> refreshCartBadge() }
    }

    private fun setupFab() {
        binding.fabPrescription?.setOnClickListener {
            val nav = baseViewModel.navController.value ?: findNavController(R.id.nav_host_fragment_activity_base)
            try {
                nav.navigate(R.id.navigation_send_prescription)
            } catch (_: Exception) {
                Toast.makeText(this, "Send Prescription", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshCartBadge() {
        val count = try { CartRepository.getInstance(this).getCount() } catch (_: Exception) { 0 }
        if (count > 0) {
            val badge = binding.navView.getOrCreateBadge(R.id.navigation_chatting)
            badge.isVisible = true
            badge.number = count
            badge.backgroundColor = getColor(R.color.primary_color)
            badge.badgeTextColor = getColor(R.color.white)
        } else {
            try { binding.navView.removeBadge(R.id.navigation_chatting) } catch (_: Exception) {}
            try { binding.navView.removeBadge(R.id.navigation_home) } catch (_: Exception) {}
        }
    }

    private var toolbarListenerRegistered = false

    private fun updateToolbarMenu() {
        if (toolbarListenerRegistered) return
        toolbarListenerRegistered = true
        val toolbar = binding.includeAppBarLayoutBase.toolbarApp
        val navController = findNavController(R.id.nav_host_fragment_activity_base)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuResource = when (destination.id) {
                R.id.navigation_home -> R.menu.home_menu
                R.id.navigation_categories -> R.menu.category_menu
                else -> 0
            }
            toolbar.menu.clear()
            if (menuResource != 0) {
                toolbar.inflateMenu(menuResource)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.empty_menu, menu)
        updateToolbarMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.search_menu_item -> {
            showToast("search_menu")
            with(window) {
                enterTransition = Explode()
                exitTransition = Explode()
            }
            val intent = Intent(this@BaseActivity, AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.search.name)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@BaseActivity).toBundle())
            true
        }
        R.id.notification_menu_item -> {
            showToast("notification_menu")
            val intent = Intent(this, AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.reminder.name)
            startActivity(intent)
            true
        }
        R.id.pharmacies_map_menu_item -> {
            showToast("pharmacies_map_menu")
            Intent(this@BaseActivity, AlternativesActivity::class.java).apply {
                putExtra("fragmentType", FragmentsKeys.map.name)
                startActivity(this)
            }
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
                Toast.makeText(this@BaseActivity, "No app found to share", Toast.LENGTH_SHORT).show()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
