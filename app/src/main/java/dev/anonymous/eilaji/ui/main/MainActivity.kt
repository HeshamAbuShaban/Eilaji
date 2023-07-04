package dev.anonymous.eilaji.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.base.BaseActivity

//This ActivityMain are responsible for counting the Splash FragmentOnBoarding
class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: MainViewModel
    private var isFirstTime: Boolean = true
//    private late-init var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show SplashScreen by it androidx library
        installSplashScreen()
        setContentView(R.layout.activity_main)
        // bring the isFirstTime value from the shared
        isFirstTime = AppSharedPreferences.getInstance(this).isHeFirstTime

        //Setup the NavHost for the main into the viewModel
        setupVMWNavController()
        // check if the onBoarding has shown or not
        shouldShowOnBoarding()

        // checks if the user came from the active position to the gateway again
        isUserFromLogout()// auto checks the intent values

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isFirstTime) {
                    finish()
                }
            }
        })
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

    // Temporary Notice that  the @ checkUserStatus will always shadow this case of the onStart
    /*private fun isChangePassword() {
        mainViewModel.navController.observe(this) {
            it?.let { nonNullNavController ->
                when (intent.getStringExtra("fragmentType")) {
                    FragmentsKeys.changePassword.name -> {
                        nonNullNavController.navigate(R.id.navigation_forgotPassword)
                    }
                }
            }
        }
    }*/

    private fun isUserFromLogout() {
        mainViewModel.navController.observe(this) {
            it?.let {
                when (intent.getStringExtra("logoutTrigger")) {
                    FragmentsKeys.logout.name -> {
                        // clear the backstack
                        mainViewModel.navController.value?.popBackStack()
                        // action to show the login
                        navigateToLoginScreen()
                        // empty the sharedPreferences just the Login
                        AppSharedPreferences.getInstance(this).setHeIsFirstTimeOut()
                    }
                }
            }
        }
    }

    // Need More Work
    private fun shouldShowOnBoarding() {
        val sharedPreferences: AppSharedPreferences = AppSharedPreferences.getInstance(this)
        val isDone = sharedPreferences.isDoneWithOnBoarding

        // Set the appropriate start destination based on whether onBoarding is done or not
        val startDestination = if (isDone) R.id.navigation_Login else R.id.navigation_onBoarding
        mainViewModel.navController.value?.graph?.setStartDestination(startDestination)

        // Check if onBoarding is done
        if (isDone) {
            // onBoarding is done, so navigate to the login screen
            navigateToLoginScreen()
        } else {
            // onBoarding is not done, so navigate to the onBoarding screen
            navigateToOnBoardingScreen()
        }

//        // Update the shared preferences to mark onBoarding as done if it's the first time
//        if (!isDone) {
//            sharedPreferences.doneWithOnBoarding()
//            AppSharedPreferences.getInstance(this).setHeIsFirstTimeDone()
//        }
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
            navigateToHomeScreen()
        }
    }

    private fun navigateToHomeScreen() {
        // Navigate to the home screen
        val intent = Intent(this@MainActivity, BaseActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginScreen() {
        // Show the login screen
        mainViewModel.navController.value?.navigate(R.id.navigation_Login)
    }

    private fun navigateToOnBoardingScreen() {
        // Navigate to the onBoarding screen
        mainViewModel.navController.value?.navigate(R.id.navigation_onBoarding)
    }

}
