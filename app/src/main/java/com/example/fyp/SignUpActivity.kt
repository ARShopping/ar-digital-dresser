package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var signUpButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var loginLink: TextView

    private lateinit var nameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText

    private lateinit var passwordToggle: ImageView
    private lateinit var confirmPasswordToggle: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth & Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Initialize UI Elements
        signUpButton = findViewById(R.id.signUpButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        loginLink = findViewById(R.id.loginLink)

        nameInput = findViewById(R.id.name)
        usernameInput = findViewById(R.id.username)
        emailInput = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        confirmPasswordInput = findViewById(R.id.confirmPassword)

        passwordToggle = findViewById(R.id.passwordToggle)
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle)

        // Password Visibility Toggle
        setupPasswordVisibilityToggle(passwordInput, passwordToggle)
        setupPasswordVisibilityToggle(confirmPasswordInput, confirmPasswordToggle)

        // Firebase Email Sign-Up
        signUpButton.setOnClickListener {
            val fullName = nameInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (fullName.isNotEmpty() && username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    registerUser(fullName, username, email, password)
                } else {
                    showToast("Passwords do not match!")
                }
            } else {
                showToast("Please fill all fields!")
            }
        }

        // Google Sign-In
        googleSignInButton.setOnClickListener {
            startGoogleSignIn()
        }

        // Navigate to Login Screen
        loginLink.setOnClickListener {
            startActivity(Intent(this, SigninActivity::class.java))
            finish()
        }

        // Initialize Google One Tap Sign-In
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }

    // Firebase Email Sign-Up Method
    private fun registerUser(fullName: String, username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserData(user, fullName, username, email)
                    navigateToMain() // Navigate to MainActivity after successful sign-up
                } else {
                    showToast("Sign-Up Failed: ${task.exception?.message}")
                }
            }
    }

    // Save User Data to Firebase Realtime Database
    private fun saveUserData(user: FirebaseUser?, fullName: String, username: String, email: String) {
        if (user != null) {
            val userId = user.uid
            val userMap = mapOf(
                "fullName" to fullName,
                "username" to username,
                "email" to email
            )
            database.child(userId).setValue(userMap)
                .addOnFailureListener {
                    showToast("Failed to save user data, but sign-up is successful!")
                }
        }
    }

    // Start Google Sign-In
    private fun startGoogleSignIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender,
                        1001, null, 0, 0, 0, null
                    )
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Couldn't start Google Sign-In", e)
                }
            }
            .addOnFailureListener {
                showToast("Google Sign-In Failed!")
            }
    }

    // Handle Google Sign-In Result
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                showToast("Google Sign-In Failed!")
            }
        }
    }

    // Authenticate Google User with Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserData(user, user?.displayName ?: "", user?.uid ?: "", user?.email ?: "")
                    navigateToMain() // Navigate to MainActivity after successful Google sign-in
                } else {
                    showToast("Google Sign-In Failed: ${task.exception?.message}")
                }
            }
    }

    // Navigate to MainActivity
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // Show Toast Messages
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Password Visibility Toggle
    private fun setupPasswordVisibilityToggle(editText: EditText, toggleButton: ImageView) {
        toggleButton.setOnClickListener {
            if (editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleButton.setImageResource(R.drawable.close_eye_svgrepo_com)
            } else {
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleButton.setImageResource(R.drawable.open_eye_svgrepo_com)
            }
            editText.setSelection(editText.text.length) // Move cursor to end
        }
    }
}
