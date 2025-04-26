package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HomeFragment : Fragment() {

    private val suggestionList = mutableListOf<String>()
    private lateinit var suggestionAdapter: ArrayAdapter<String>

    private lateinit var recyclerViewRecommended: RecyclerView
    private lateinit var recommendedAdapter: ProductAdapter
    private val recommendedList = mutableListOf<ProductModel>()

    private lateinit var recyclerViewRecentlyViewed: RecyclerView
    private lateinit var recentAdapter: ProductAdapter
    private val recentList = mutableListOf<ProductModel>()

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Greeting
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val greetingPrefix = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "there"
                    tvGreeting.text = "$greetingPrefix $name, ready to try something new?"
                }
                .addOnFailureListener {
                    tvGreeting.text = "$greetingPrefix there, ready to try something new?"
                }
        } else {
            tvGreeting.text = "$greetingPrefix there, ready to try something new?"
        }

        // Search Suggestions
        val etSearch = view.findViewById<AutoCompleteTextView>(R.id.etSearch)
        suggestionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestionList
        )
        etSearch.setAdapter(suggestionAdapter)

        FirebaseFirestore.getInstance().collection("products").get()
            .addOnSuccessListener { result ->
                suggestionList.clear()
                for (document in result) {
                    val productName = document.getString("name") ?: continue
                    suggestionList.add(productName)
                }
                suggestionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch suggestions: ${e.message}")
            }

        etSearch.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            val intent = Intent(requireContext(), ProductListActivity::class.java)
            intent.putExtra("searchQuery", selected)
            startActivity(intent)
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(requireContext(), ProductListActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)
            }
            true
        }

        // Tips Flipper
        val flipper = view.findViewById<ViewFlipper>(R.id.viewFlipperTips)
        flipper.inAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
        flipper.outAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right)
        flipper.flipInterval = 5000
        flipper.startFlipping()

        val tips = listOf(
            "🛍️ 1. Tap on any outfit and hit 'Try On' to start your virtual fitting.",
            "📷 2. Make sure your face and upper body are fully visible in the frame.",
            "🧱 3. Stand in front of a plain background for the best results.",
            "🔁 4. Double-tap the screen to switch between front and back cameras.",
            "📸 5. Tap the camera icon to snap a photo of your look.",
            "🎬 6. Hold the video icon to capture a stylish try-on video."
        )

        for (tip in tips) {
            val tipView = TextView(requireContext()).apply {
                text = "• $tip"
                textSize = 14f
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(R.color.black, null))
            }
            flipper.addView(tipView)
        }

        // Recommended Products
        recyclerViewRecommended = view.findViewById(R.id.recyclerViewRecommended)
        recyclerViewRecommended.layoutManager = GridLayoutManager(requireContext(), 2)
        recommendedAdapter = ProductAdapter(recommendedList) { product ->
            val intent = Intent(requireContext(), ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.productId)
                putExtra("categoryId", product.categoryId)
                putExtra("productName", product.name)
                putExtra("productPrice", product.price)
                putExtra("productImage", product.image)
                putExtra("productDescription", product.description)
            }
            startActivity(intent)
        }
        recyclerViewRecommended.adapter = recommendedAdapter

        loadRecommendedProducts()

        // Recently Viewed
        recyclerViewRecentlyViewed = view.findViewById(R.id.recyclerRecentlyViewed)
        recyclerViewRecentlyViewed.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recentAdapter = ProductAdapter(recentList) { product ->
            val intent = Intent(requireContext(), ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.productId)
                putExtra("categoryId", product.categoryId)
                putExtra("productName", product.name)
                putExtra("productPrice", product.price)
                putExtra("productImage", product.image)
                putExtra("productDescription", product.description)
            }
            startActivity(intent)
        }
        recyclerViewRecentlyViewed.adapter = recentAdapter

        val recent = RecentlyViewedManager.getProducts(requireContext())
        recentList.clear()
        recentList.addAll(recent)
        recentAdapter.notifyDataSetChanged()
    }

    private fun loadRecommendedProducts() {
        database = FirebaseDatabase.getInstance().getReference("products")
        val maxItems = 10

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<ProductModel>()
                for (categorySnapshot in snapshot.children) {
                    for (productSnapshot in categorySnapshot.children) {
                        val value = productSnapshot.value
                        if (value !is Map<*, *>) continue
                        try {
                            val product = ProductModel(
                                productId = productSnapshot.key ?: "",
                                categoryId = categorySnapshot.key ?: "",
                                name = value["name"].toString(),
                                price = value["price"]?.toString() ?: "0",
                                image = value["image"]?.toString() ?: "",
                                description = value["description"]?.toString() ?: "",
                                timestamp = value["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                            )
                            tempList.add(product)
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error parsing product: ${e.message}")
                        }
                    }
                }
                tempList.sortByDescending { it.timestamp }
                recommendedList.clear()
                recommendedList.addAll(tempList.take(maxItems))
                recommendedAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load recommended products", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
