package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class GetStarted : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Ensure edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ✅ Set content view
        setContentView(R.layout.activity_get_started)

        // ✅ Initialize button
        val btnGetStarted: Button = findViewById(R.id.btnGetStarted)

        // ✅ Set button click listener
        btnGetStarted.setOnClickListener {
            navigateToSignIn()
        }
    }

    // ✅ Separate function for navigation (clean code practice)
    private fun navigateToSignIn() {
        startActivity(Intent(this, SigninActivity::class.java))
        finish()  // Close this activity to prevent going back to it
    }
}
