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
                    FragmentsKeys.messaging.name -> {
                        nonNullNavController.navigate(R.id.navigation_messaging, getArgsMessaging())
                    }

                    FragmentsKeys.subCategories.name -> {
                        nonNullNavController.navigate(
                            R.id.navigation_sub_categories_fragment, getArgsSubCategories()
                        )
                    }

                    FragmentsKeys.sendToPharmacy.name -> {
                        nonNullNavController.navigate(
                            R.id.navigation_send_to_pharmacy_fragment,
                            getArgsSendToPharmacy()
                        )
                    }
                }
            }
        }
    }

    private fun getArgsSendToPharmacy(): Bundle {
        val lat: Float = intent.getFloatExtra("lat", 0.0F)
        val lng: Float = intent.getFloatExtra("lng", 0.0F)
        val stringUri: String = intent.getStringExtra("stringUri")!!
        val description: String = intent.getStringExtra("description")!!

        val args = Bundle()
        args.putFloat("lat", lat)
        args.putFloat("lng", lng)
        args.putString("stringUri", stringUri)
        args.putString("description", description)

        return args
    }

    private fun getArgsSubCategories(): Bundle {
        val categoryId: String = intent.getStringExtra("categoryId")!!
        val categoryTitle: String = intent.getStringExtra("categoryTitle")!!

        val args = Bundle()
        args.putString("categoryId", categoryId)
        args.putString("categoryTitle", categoryTitle)

        return args
    }

    private fun getArgsMessaging(): Bundle {
        val chatId: String = intent.getStringExtra("chatId")!!
        val receiverUid: String = intent.getStringExtra("receiverUid")!!
        val receiverFullName: String = intent.getStringExtra("receiverFullName")!!
        val receiverUrlImage: String = intent.getStringExtra("receiverUrlImage")!!
        val receiverToken: String = intent.getStringExtra("receiverToken")!!

        val args = Bundle()
        args.putString("chatId", chatId)
        args.putString("receiverUid", receiverUid)
        args.putString("receiverFullName", receiverFullName)
        args.putString("receiverUrlImage", receiverUrlImage)
        args.putString("receiverToken", receiverToken);
        args.putString("stringUri", null);
        args.putString("description", null);

        return args
    }

    private fun setupViewModel() {
        alternativesViewModel = ViewModelProvider(this)[AlternativesViewModel::class.java]
        val navController: NavController =
            findNavController(R.id.nav_host_fragment_activity_alternatives)
        alternativesViewModel.setNavController(navController)
    }
}