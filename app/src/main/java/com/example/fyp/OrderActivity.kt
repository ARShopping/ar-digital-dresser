package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OrderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        // Initialize button
        val btnStartShopping: Button = findViewById(R.id.btnStartShopping)

        // Set click listener for the button
        btnStartShopping.setOnClickListener {
            navigateToMainActivity() // Navigate without showing a toast
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close OrderActivity to prevent returning to it
    }
}
