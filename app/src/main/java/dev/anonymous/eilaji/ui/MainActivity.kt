package dev.anonymous.eilaji.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ActivityMainBinding
import dev.anonymous.eilaji.ui.base.BaseActivity
import dev.anonymous.eilaji.ui.onBoarding.FragmentOnBoarding

//This ActivityMain are responsible for counting the Splash FragmentOnBoarding
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show SplashScreen by it androidx library
        installSplashScreen()
        initBinding()
        setContentView(binding.root)

        // Initialize Firebase Auth
        initFireAuth()

        /* splashScreen.setKeepOnScreenCondition(() -> true);
         new Handler().postDelayed(() ->
         splashScreen.setKeepOnScreenCondition(() -> false), 5000);*/

        beginOnBoardingScene()
    }

    private fun beginOnBoardingScene() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainActivityContainer, FragmentOnBoarding()).commit()
    }

    private fun initFireAuth() {
        auth = Firebase.auth
    }

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            //TODO HomeActivity >> Base
            startActivity(Intent(this@MainActivity, BaseActivity::class.java))
            finish()
        }
    }

}
