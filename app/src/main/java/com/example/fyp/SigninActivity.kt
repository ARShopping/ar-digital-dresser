package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.databinding.ActivitySigninBinding
import com.google.firebase.auth.FirebaseAuth

class SigninActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false // Track password visibility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Set click listener for login button
        binding.loginButton.setOnClickListener {
            performLogin()
        }

        // Set click listener for "Forgot Password?" TextView
        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for "Don't have an account? Sign Up" link
        binding.newAccountText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Set the "Done" action for the keyboard on password EditText
        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin() // Call the login method when "Done" is pressed
                true
            } else {
                false
            }
        }

        // Implement password toggle
        binding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    // Function to toggle password visibility
    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passwordToggle.setImageResource(R.drawable.close_eye_svgrepo_com)
        } else {
            // Show password
            binding.password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passwordToggle.setImageResource(R.drawable.open_eye_svgrepo_com)
        }
        isPasswordVisible = !isPasswordVisible

        // Move cursor to the end after toggling
        binding.password.setSelection(binding.password.text.length)
    }

    // Function to perform login with Firebase Authentication
    private fun performLogin() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        // Input validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication: Sign in user
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                    // Check if the email is the admin email
                    if (email == "ardigitaldresser@gmail.com") {
                        // Redirect to Admin Dashboard
                        val intent = Intent(this, AdminDashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Close SigninActivity
                    } else {
                        // Redirect to MainActivity for regular users
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Close SigninActivity
                    }

                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
