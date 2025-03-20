package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProductListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoProducts: TextView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var productList: MutableList<ProductModel>
    private lateinit var database: DatabaseReference
    private lateinit var btnAddProduct: ImageButton

    private var categoryId: String? = null
    private var categoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product_list)

        categoryId = intent.getStringExtra("categoryId")
        categoryName = intent.getStringExtra("categoryName")

        if (categoryId.isNullOrEmpty()) {
            Toast.makeText(this, "Category ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = "Products in $categoryName"

        database = FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        tvNoProducts = findViewById(R.id.tvNoProducts)  // Find the "No products" TextView
        btnAddProduct = findViewById(R.id.btnAddProduct)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        productList = mutableListOf()

        productAdapter = ProductAdapter(productList) { product ->
            val intent = Intent(this, AdminProductDetailsActivity::class.java)
            intent.putExtra("productId", product.productId)
            intent.putExtra("categoryId", categoryId)
            intent.putExtra("productName", product.name)
            intent.putExtra("productPrice", product.price)
            intent.putExtra("productImage", product.image)
            intent.putExtra("productDescription", product.description)
            startActivity(intent)
        }

        recyclerView.adapter = productAdapter

        loadProducts()

        btnAddProduct.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("categoryId", categoryId)
            intent.putExtra("categoryName", categoryName)
            startActivity(intent)
        }
    }

    private fun loadProducts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(ProductModel::class.java)
                    product?.let { productList.add(it) }
                }

                if (productList.isEmpty()) {
                    tvNoProducts.visibility = View.VISIBLE  // Show "No products available"
                    recyclerView.visibility = View.GONE  // Hide RecyclerView
                } else {
                    tvNoProducts.visibility = View.GONE  // Hide message
                    recyclerView.visibility = View.VISIBLE  // Show RecyclerView
                }

                productAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminProductListActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
