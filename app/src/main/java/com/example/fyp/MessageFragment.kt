package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class MessageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        // Initialize buttons
        val btnChat: Button = view.findViewById(R.id.btnChat)
        val btnOrder: Button = view.findViewById(R.id.btnOrder)
        val btnNotification: Button = view.findViewById(R.id.btnNotification)
        val btnExplore: Button = view.findViewById(R.id.btnExplore)

        // Set up click listeners
        btnChat.setOnClickListener {
            startActivity(Intent(activity, ChatsActivity::class.java))
        }

        btnOrder.setOnClickListener {
            startActivity(Intent(activity, OrderActivity::class.java))
        }

        btnNotification.setOnClickListener {
            startActivity(Intent(activity, NotificationActivity::class.java))
        }

        btnExplore.setOnClickListener {
            startActivity(Intent(activity, MainActivity::class.java))
        }

        return view
    }
}
