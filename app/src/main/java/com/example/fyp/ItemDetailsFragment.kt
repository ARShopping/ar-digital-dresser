package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class ItemDetailsFragment : Fragment() {

    private lateinit var itemImageView: ImageView
    private lateinit var itemNameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_item_details, container, false)

        itemImageView = view.findViewById(R.id.imageViewItem)
        itemNameTextView = view.findViewById(R.id.textViewItemName)

        // Retrieve arguments passed from CategoryFragment
        val itemName = arguments?.getString("itemName") ?: "Unknown"
        val itemImage = arguments?.getString("itemImage") ?: ""

        // Set item details
        itemNameTextView.text = itemName

        // Load image using Glide
        Glide.with(this)
            .load(itemImage)
            .placeholder(R.drawable.hoodie1) // Replace with your placeholder image
            .into(itemImageView)

        return view
    }
}
