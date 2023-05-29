package dev.anonymous.eilaji;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import dev.anonymous.eilaji.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        splashScreen.setKeepOnScreenCondition(() -> true);
//        new Handler().postDelayed(() ->
//                splashScreen.setKeepOnScreenCondition(() -> false), 5000);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainActivityContainer, new FragmentOnBoarding())
                .commit();

    }
}
