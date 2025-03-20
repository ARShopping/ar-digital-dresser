package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_account, container, false)

        // Profile Picture
        val profilePicture: ImageView = rootView.findViewById(R.id.profilePicture)
        profilePicture.setImageResource(R.drawable.profile_user_svgrepo_com) // Set profile picture

        // User Information
        val userName: TextView = rootView.findViewById(R.id.userName)
        userName.text = getString(R.string.username_placeholder) // Use string resource

        val userEmail: TextView = rootView.findViewById(R.id.userEmail)
        userEmail.text = getString(R.string.email_placeholder) // Use string resource

        // Edit Profile Button
        val editProfileButton: Button = rootView.findViewById(R.id.editProfileButton)
        editProfileButton.setOnClickListener {
            // Navigate to EditProfileActivity
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Customer Support Button
        val customerSupportButton: Button = rootView.findViewById(R.id.customerSupportButton)
        customerSupportButton.setOnClickListener {
            // Navigate to CustomerSupportActivity
            val intent = Intent(requireActivity(), CustomerSupportActivity::class.java)
            startActivity(intent)
        }

        // Sign Out Button
        val signOutButton: Button = rootView.findViewById(R.id.signOutButton)
        signOutButton.setOnClickListener {
            // Show sign-out message
            Toast.makeText(context, getString(R.string.signing_out), Toast.LENGTH_SHORT).show()

            // Sign out user (Firebase)
            auth.signOut()

            // Navigate back to SigninActivity
            val intent = Intent(requireActivity(), SigninActivity::class.java)
            startActivity(intent)
            requireActivity().finish() // Close the AccountFragment
        }

        return rootView
    }
}
