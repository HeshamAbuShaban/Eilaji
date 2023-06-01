package dev.anonymous.eilaji.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.anonymous.eilaji.ui.onBoarding.FragmentOnBoarding
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ActivityMainBinding
import dev.anonymous.eilaji.ui.base.BaseActivity

//This ActivityMain are responsible for counting the Splash FragmentOnBoarding
class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        // Show SplashScreen by it androidx library
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        // Initialize Firebase Auth
        auth = Firebase.auth

        // splashScreen.setKeepOnScreenCondition(() -> true);
        // new Handler().postDelayed(() ->
        // splashScreen.setKeepOnScreenCondition(() -> false), 5000);
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainActivityContainer, FragmentOnBoarding()).commit()
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //TODO HomeActivity >> Base
            startActivity(Intent(this@MainActivity, BaseActivity::class.java))
            finish()
        }
    }

}
