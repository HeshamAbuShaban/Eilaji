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

    private var navigated = false

    private fun decideWhichScreen() {
        alternativesViewModel.navController.observe(this) {
            it?.let { nonNullNavController ->
                if (navigated) return@let
                navigated = true
                val type = intent.getStringExtra("fragmentType")
                if (type == null) {
                    finish()
                    return@let
                }
                val startId = try { nonNullNavController.graph.startDestinationId } catch (_: Exception) { R.id.navigation_medicine }
                fun navWithPop(dest: Int, args: Bundle?) {
                    try {
                        val opts = androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(startId, true)
                            .setEnterAnim(android.R.anim.slide_in_left)
                            .setExitAnim(android.R.anim.slide_out_right)
                            .build()
                        if (args == null) nonNullNavController.navigate(dest, null, opts)
                        else nonNullNavController.navigate(dest, args, opts)
                    } catch (_: Exception) {
                        try { nonNullNavController.navigate(dest, args) } catch (_: Exception) { finish() }
                    }
                }
                when (type) {
                    FragmentsKeys.add_address.name -> navWithPop(R.id.navigation_add_address, null)
                    FragmentsKeys.favorites.name -> navWithPop(R.id.navigation_favorites, null)
                    FragmentsKeys.medicine.name -> {
                        val mid = intent.getStringExtra("medicineId")
                        val b = Bundle().apply { if (mid != null) putString("medicineId", mid) }
                        navWithPop(R.id.navigation_medicine, b)
                    }
                    FragmentsKeys.reminder.name -> navWithPop(R.id.navigation_reminders_list, null)
                    FragmentsKeys.search.name -> navWithPop(R.id.navigation_search, null)
                    FragmentsKeys.map.name -> navWithPop(R.id.navigation_map, null)
                    FragmentsKeys.messaging.name -> navWithPop(R.id.navigation_messaging, getArgsMessaging())
                    FragmentsKeys.subCategories.name -> navWithPop(R.id.navigation_sub_categories_fragment, getArgsSubCategories())
                    FragmentsKeys.sendToPharmacy.name -> navWithPop(R.id.navigation_send_to_pharmacy_fragment, getArgsSendToPharmacy())
                    FragmentsKeys.checkout.name -> {
                        val b = Bundle().apply {
                            putString("medicineId", intent.getStringExtra("medicineId"))
                            putInt("quantity", intent.getIntExtra("quantity", 1))
                            putString("pharmacyId", intent.getStringExtra("pharmacyId"))
                            putString("pharmacyName", intent.getStringExtra("pharmacyName"))
                        }
                        navWithPop(R.id.navigation_checkout, b)
                    }
                    else -> finish()
                }
            }
        }
    }

    private fun getArgsSendToPharmacy(): Bundle {
        val args = Bundle()
        args.putFloat("lat", intent.getFloatExtra("lat", 0.0F))
        args.putFloat("lng", intent.getFloatExtra("lng", 0.0F))
        args.putString("stringUri", intent.getStringExtra("stringUri"))
        args.putString("description", intent.getStringExtra("description"))
        return args
    }

    private fun getArgsSubCategories(): Bundle {
        val args = Bundle()
        args.putString("categoryId", intent.getStringExtra("categoryId"))
        args.putString("categoryTitle", intent.getStringExtra("categoryTitle") ?: "Category")
        return args
    }

    private fun getArgsMessaging(): Bundle {
        val args = Bundle()
        args.putString("chatId", intent.getStringExtra("chatId"))
        args.putString("receiverUid", intent.getStringExtra("receiverUid"))
        args.putString("receiverFullName", intent.getStringExtra("receiverFullName") ?: "Pharmacy")
        args.putString("receiverUrlImage", intent.getStringExtra("receiverUrlImage"))
        args.putString("receiverToken", intent.getStringExtra("receiverToken"))
        args.putString("stringUri", intent.getStringExtra("stringUri"))
        args.putString("description", intent.getStringExtra("description"))
        return args
    }

    private fun setupViewModel() {
        alternativesViewModel = ViewModelProvider(this)[AlternativesViewModel::class.java]
        val navController: NavController =
            findNavController(R.id.nav_host_fragment_activity_alternatives)
        alternativesViewModel.setNavController(navController)
    }
}