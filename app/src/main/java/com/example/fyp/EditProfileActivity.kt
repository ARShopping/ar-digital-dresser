package com.example.fyp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    private lateinit var userNameEditText: EditText
    private lateinit var userEmailEditText: EditText
    private lateinit var currentPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var saveChangesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize the UI elements
        userNameEditText = findViewById(R.id.editUserName)
        userEmailEditText = findViewById(R.id.editUserEmail)
        currentPasswordEditText = findViewById(R.id.editCurrentPassword)
        newPasswordEditText = findViewById(R.id.editNewPassword)
        confirmPasswordEditText = findViewById(R.id.editConfirmPassword)
        saveChangesButton = findViewById(R.id.saveChangesButton)

        // Pre-fill fields if necessary (e.g., from shared preferences or database)
        // userNameEditText.setText("Current Username")
        // userEmailEditText.setText("Current Email")

        // Save changes button logic
        saveChangesButton.setOnClickListener {
            val newUserName = userNameEditText.text.toString()
            val newEmail = userEmailEditText.text.toString()
            val currentPassword = currentPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newUserName.isNotBlank() && newEmail.isNotBlank() && currentPassword.isNotBlank() &&
                newPassword.isNotBlank() && confirmPassword.isNotBlank()) {

                // Check if new password and confirm password match
                if (newPassword == confirmPassword) {
                    // Here, you would add logic to verify the current password (e.g., check with backend)

                    // Update the profile (in SharedPreferences, database, or backend)
                    // For now, just show a toast
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

                    // Navigate back to the previous screen (e.g., AccountFragment or MainActivity)
                    finish() // This will close the EditProfileActivity
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show a message if fields are empty
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
