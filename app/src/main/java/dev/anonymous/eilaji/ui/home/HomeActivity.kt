package dev.anonymous.eilaji.ui.home

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.firebase.FirebaseController

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        // temp sign out button for testing
        val btnSignOut: Button = findViewById(R.id.temp_signOut)
        btnSignOut.setOnClickListener {
            FirebaseController.getInstance().signOut()
            finish()
        }
    }
}