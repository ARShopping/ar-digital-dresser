package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.fyp.databinding.ActivityProductDetailsBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding
    private lateinit var database: DatabaseReference
    private var selectedSize: String = "S" // Default size
    private var productId: String? = null
    private var categoryId: String? = null
    private lateinit var cartDatabase: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getStringExtra("productId")
        categoryId = intent.getStringExtra("categoryId")

        if (productId.isNullOrEmpty() || categoryId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase Database References
        database = FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)
        cartDatabase = FirebaseDatabase.getInstance().getReference("cart").child(userId ?: "")

        // Load product details
        loadProductDetails()

        // Set Click Listeners
        binding.buttonTryOn.setOnClickListener {
            launchARTryOn()
        }

        binding.buttonBuyNow.setOnClickListener {
            handleBuyNow()
        }

        binding.buttonAddToCart.setOnClickListener {
            addToCartDirectly()
        }
    }

    /** 🔹 Load product details from Firebase */
    private fun loadProductDetails() {
        database.child(productId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(applicationContext, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val productName = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                val productPrice = snapshot.child("price").getValue(String::class.java) ?: "0"
                val productImage = snapshot.child("image").getValue(String::class.java) ?: ""
                val productDescription = snapshot.child("description").getValue(String::class.java) ?: "No description available"

                binding.textViewProductName.text = productName
                binding.textViewProductPrice.text = "PKR $productPrice"
                binding.textViewProductDescription.text = productDescription
                Glide.with(applicationContext).load(productImage).into(binding.imageViewProduct)

                // Use productImage as overlay image for AR try-on
                binding.buttonTryOn.tag = productImage

                val availableSizes = snapshot.child("sizes").children.mapNotNull { it.getValue(String::class.java) }
                binding.sizesChipGroup.removeAllViews()
                availableSizes.forEach { size ->
                    val chip = Chip(this@ProductDetailsActivity).apply {
                        text = size
                        isCheckable = true
                        setOnClickListener { selectedSize = size }
                    }
                    binding.sizesChipGroup.addView(chip)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to load product details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** 🔹 Launch AR Try-On with 2D overlay image */
    private fun launchARTryOn() {
        val imageUrl = binding.buttonTryOn.tag as? String
        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "2D try-on image not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ARTryOnActivity::class.java)
        intent.putExtra("modelUrl", imageUrl) // ARTryOnActivity expects "modelUrl" as key
        startActivity(intent)
    }

    /** 🔹 Handle Buy Now button */
    private fun handleBuyNow() {
        val priceText = binding.textViewProductPrice.text?.toString() ?: ""

        if (priceText.isEmpty()) {
            Toast.makeText(this, "Error: Product price not available", Toast.LENGTH_SHORT).show()
            return
        }

        val numericPrice = priceText.replace("PKR ", "").trim()

        val intent = Intent(this, DeliveryActivity::class.java).apply {
            putExtra("productName", binding.textViewProductName.text.toString())
            putExtra("productPrice", numericPrice)
            putExtra("selectedSize", selectedSize)
        }
        startActivity(intent)
    }

    /** 🔹 Directly Add to Cart */
    private fun addToCartDirectly() {
        if (userId == null) {
            Toast.makeText(this, "Please login to add items to cart", Toast.LENGTH_SHORT).show()
            return
        }

        val cartItem = hashMapOf(
            "productId" to productId,
            "categoryId" to categoryId,
            "productName" to binding.textViewProductName.text.toString(),
            "productPrice" to binding.textViewProductPrice.text.toString().replace("PKR ", "").trim(),
            "selectedSize" to selectedSize,
            "quantity" to 1
        )

        cartDatabase.child(productId!!).setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Added to Cart!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show()
            }
    }
}
