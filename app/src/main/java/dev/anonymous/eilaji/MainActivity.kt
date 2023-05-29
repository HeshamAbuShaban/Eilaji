package dev.anonymous.eilaji

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.anonymous.eilaji.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen: SplashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

//        splashScreen.setKeepOnScreenCondition(() -> true);
//        new Handler().postDelayed(() ->
//                splashScreen.setKeepOnScreenCondition(() -> false), 5000);
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainActivityContainer, FragmentOnBoarding())
            .commit()
    }
}