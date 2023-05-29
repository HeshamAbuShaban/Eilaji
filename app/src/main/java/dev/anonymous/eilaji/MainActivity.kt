package dev.anonymous.eilaji

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.anonymous.eilaji.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen: SplashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
// Initialize Firebase Auth
        auth = Firebase.auth
//        splashScreen.setKeepOnScreenCondition(() -> true);
//        new Handler().postDelayed(() ->
//                splashScreen.setKeepOnScreenCondition(() -> false), 5000);
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainActivityContainer, FragmentOnBoarding()).commit()
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this@MainActivity,HomeActivity::class.java))
            finish()
        }
    }

}
