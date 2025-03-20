package com.example.fyp

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Check if Admin is Logged In
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email != "ardigitaldresser@gmail.com") {
            Toast.makeText(this, "Unauthorized Access. Please Log in as Admin.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SigninActivity::class.java))
            finish()
            return
        }

        // Set Click Listeners with Animations
        setupCardAnimation(binding.manageUsersCard) {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        setupCardAnimation(binding.manageInventoryCard) {
            startActivity(Intent(this, ManageInventoryActivity::class.java))
        }

        setupCardAnimation(binding.manageOrdersCard) {
            startActivity(Intent(this, ManageOrdersActivity::class.java))
        }

        setupCardAnimation(binding.viewReportsCard) {
            startActivity(Intent(this, ViewReportsActivity::class.java))
        }

        // Logout Button
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SigninActivity::class.java))
            finish()
        }
    }

    private fun setupCardAnimation(cardView: androidx.cardview.widget.CardView, action: () -> Unit) {
        cardView.setOnClickListener {
            val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1f)
            val animator = ObjectAnimator.ofPropertyValuesHolder(cardView, scaleX, scaleY)
            animator.duration = 200
            animator.start()

            // Perform action after animation
            cardView.postDelayed({ action.invoke() }, 200)
        }
    }
}
