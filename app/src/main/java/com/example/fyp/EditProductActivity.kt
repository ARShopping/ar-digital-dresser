package com.example.fyp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class EditProductActivity : AppCompatActivity() {

    private lateinit var editProductName: EditText
    private lateinit var editProductPrice: EditText
    private lateinit var productImage: ImageView
    private lateinit var btnUpdateProduct: Button
    private lateinit var btnDeleteProduct: Button
    private lateinit var productId: String
    private lateinit var categoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        editProductName = findViewById(R.id.editProductName)
        editProductPrice = findViewById(R.id.editProductPrice)
        productImage = findViewById(R.id.productImage)
        btnUpdateProduct = findViewById(R.id.btnUpdateProduct)
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct)

        productId = intent.getStringExtra("productId") ?: ""
        categoryId = intent.getStringExtra("categoryId") ?: ""

        btnUpdateProduct.setOnClickListener {
            updateProduct()
        }

        btnDeleteProduct.setOnClickListener {
            deleteProduct()
        }
    }

    private fun updateProduct() {
        val name = editProductName.text.toString()
        val price = editProductPrice.text.toString().toDoubleOrNull()

        if (name.isEmpty() || price == null) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("products").child(categoryId).child(productId)
        dbRef.child("name").setValue(name)
        dbRef.child("price").setValue(price)

        Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show()
    }

    private fun deleteProduct() {
        val dbRef = FirebaseDatabase.getInstance().getReference("products").child(categoryId).child(productId)
        dbRef.removeValue()
        Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show()
        finish()
    }
}
