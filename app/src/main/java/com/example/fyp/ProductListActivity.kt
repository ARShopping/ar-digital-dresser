package com.example.fyp

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        // Get category ID and name from intent
        categoryId = intent.getStringExtra("categoryId")
        categoryName = intent.getStringExtra("categoryName")

        if (categoryId.isNullOrEmpty()) {
            Toast.makeText(this, "Category ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = categoryName

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        productAdapter = ProductAdapter(productList) { product ->
            val intent = Intent(this, ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.productId)
                putExtra("categoryId", categoryId)  // ✅ Now categoryId is passed correctly
                putExtra("productName", product.name)
                putExtra("productPrice", product.price)
                putExtra("productImage", product.image)
                putExtra("productDescription", product.description)
            }
            startActivity(intent)
        }
        recyclerView.adapter = productAdapter

        database = FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)
        loadProducts()
    }

    private fun loadProducts() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val product = snapshot.getValue(ProductModel::class.java)
                product?.let {
                    productList.add(it)
                    productAdapter.notifyItemInserted(productList.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedProduct = snapshot.getValue(ProductModel::class.java)
                updatedProduct?.let {
                    val index = productList.indexOfFirst { it.productId == updatedProduct.productId }
                    if (index != -1) {
                        productList[index] = updatedProduct
                        productAdapter.notifyItemChanged(index)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedProduct = snapshot.getValue(ProductModel::class.java)
                removedProduct?.let {
                    val index = productList.indexOfFirst { it.productId == removedProduct.productId }
                    if (index != -1) {
                        productList.removeAt(index)
                        productAdapter.notifyItemRemoved(index)
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProductListActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
