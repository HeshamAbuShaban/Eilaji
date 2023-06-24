package dev.anonymous.eilaji.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.ui.base.BaseActivity
import dev.anonymous.eilaji.utils.FragmentsKeys

//This ActivityMain are responsible for counting the Splash FragmentOnBoarding
class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: MainViewModel
//    private late-init var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show SplashScreen by it androidx library
        installSplashScreen()
        setContentView(R.layout.activity_main)


        //Setup the NavHost for the main into the viewModel
        setupVMWNavController()
        // check if the onBoarding has shown or not
        shouldShowOnBoarding()

        //checks if the user came from the active position to the gateway again
        isUserFromLogout()// auto checks the intent values

//        @Deprecated
//        beginOnBoardingScene()
    }

    // Initializer Methods
    private fun setupVMWNavController() {
        //  initialize the view-model instance
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        //instance of the nav for more use
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        //this gives a value for the view model instance type NavController
        mainViewModel.setNavController(navController)
    }

    private fun isUserFromLogout() {
        mainViewModel.navController.observe(this) {
            it?.let { nonNullNavController ->
                when (intent.getStringExtra("logoutTrigger")) {
                    FragmentsKeys.logout.name -> {
                        //to remove the OnBoarding from the back stack of the nav graph
                        nonNullNavController.popBackStack()// this is important line for logic purpose
                        nonNullNavController.navigate(R.id.navigation_Login)
                    }
                }
            }
        }
    }

    // Need More Work
    private fun shouldShowOnBoarding() {
        val sharedPreferences: AppSharedPreferences = AppSharedPreferences.getInstance(this)
        val isDone = sharedPreferences.isDoneWithOnBoarding
        val startDestination = if (isDone) R.id.navigation_Login else R.id.navigation_onBoarding
        mainViewModel.navController.value?.graph?.setStartDestination(startDestination)
        if (isDone) {
            mainViewModel.navController.value?.navigate(R.id.navigation_Login)
        } else {
            mainViewModel.navController.value?.navigate(R.id.navigation_onBoarding)
        }
        if (!isDone) {
            sharedPreferences.doneWithOnBoarding()
        }
    }


    // LifeCycle usage to check the user status
    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }


    /**
     * This method [checkUserStatus]
     * Checks if user is signed in aka (non-null) and update UI accordingly.
     * */
    private fun checkUserStatus() {
        if (FirebaseController.getInstance().getCurrentUser() != null) {

            /**
             *
             * @Deprecated this practice has no bottom view nor toolbar
             * [mainViewModel.navController.value?.navigate(R.id.override_navigation_home)]
             *
             * */
            val intent = Intent(this@MainActivity, BaseActivity::class.java)
            startActivity(intent);finish()
        }
    }

}
