package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ProductListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var database: DatabaseReference
    private val productList = mutableListOf<ProductModel>()

    private var categoryId: String? = null
    private var categoryName: String? = null
    private var searchQuery: String? = null

    private lateinit var tvNoResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        categoryId = intent.getStringExtra("categoryId")
        categoryName = intent.getStringExtra("categoryName")
        searchQuery = intent.getStringExtra("searchQuery")

        supportActionBar?.title = categoryName ?: "Search Results"

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        tvNoResults = findViewById(R.id.tvNoResults)

        productAdapter = ProductAdapter(productList) { product ->
            val intent = Intent(this, ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.productId)
                putExtra("categoryId", categoryId)
                putExtra("productName", product.name)
                putExtra("productPrice", product.price)
                putExtra("productImage", product.image)
                putExtra("productDescription", product.description)
            }
            startActivity(intent)
        }
        recyclerView.adapter = productAdapter

        database = if (!categoryId.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)
        } else {
            FirebaseDatabase.getInstance().getReference("products")
        }

        loadProducts()
    }

    private fun loadProducts() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                if (snapshot.exists()) {
                    if (categoryId != null) {
                        for (child in snapshot.children) {
                            val product = child.getValue(ProductModel::class.java)
                            if (product != null && shouldInclude(product)) {
                                productList.add(product)
                            }
                        }
                    } else {
                        for (categorySnapshot in snapshot.children) {
                            for (productSnapshot in categorySnapshot.children) {
                                val product = productSnapshot.getValue(ProductModel::class.java)
                                if (product != null && shouldInclude(product)) {
                                    productList.add(product)
                                }
                            }
                        }
                    }
                }

                // ✅ Sort by most recent timestamp
                productList.sortByDescending { it.timestamp }

                productAdapter.notifyDataSetChanged()
                tvNoResults.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProductListActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun shouldInclude(product: ProductModel): Boolean {
        return searchQuery.isNullOrEmpty() || product.name.contains(searchQuery!!, ignoreCase = true)
    }
}
