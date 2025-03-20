package com.example.fyp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CustomerSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_support)

        // Try using try-catch for debugging potential errors
        try {
            // Inquiry message input field
            val inquiryMessage: EditText = findViewById(R.id.inquiryMessage)

            // Send inquiry button logic
            val sendInquiryButton: Button = findViewById(R.id.sendInquiryButton)
            sendInquiryButton.setOnClickListener {
                val message = inquiryMessage.text.toString()

                if (message.isNotEmpty()) {
                    // Handle sending inquiry (e.g., via email, support backend, etc.)
                    Toast.makeText(this, getString(R.string.inquiry_sent), Toast.LENGTH_SHORT).show()

                    // Optionally clear the input field after sending the inquiry
                    inquiryMessage.text.clear()
                } else {
                    Toast.makeText(this, getString(R.string.enter_message_prompt), Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            // Catch any errors and print them to Logcat
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.error_occurred, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }
}
