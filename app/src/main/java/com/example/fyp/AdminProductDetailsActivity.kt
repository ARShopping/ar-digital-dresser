package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.*

class AdminProductDetailsActivity : AppCompatActivity() {

    private lateinit var productImageView: ImageView
    private lateinit var btnChangeImage: Button
    private lateinit var editTextProductName: EditText
    private lateinit var editTextProductPrice: EditText
    private lateinit var editTextProductDescription: EditText
    private lateinit var btnUpdateProduct: Button
    private lateinit var btnDeleteProduct: Button

    private lateinit var database: DatabaseReference
    private var productId: String? = null
    private var categoryId: String? = null
    private var modelUrl: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product_details)

        productImageView = findViewById(R.id.productImageView)
        btnChangeImage = findViewById(R.id.btnChangeImage)
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextProductPrice = findViewById(R.id.editTextProductPrice)
        editTextProductDescription = findViewById(R.id.editTextProductDescription)
        btnUpdateProduct = findViewById(R.id.btnUpdateProduct)
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct)

        productId = intent.getStringExtra("productId")
        categoryId = intent.getStringExtra("categoryId")

        if (categoryId.isNullOrEmpty() || productId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)

        loadProductDetails()

        btnChangeImage.setOnClickListener { openFileChooser(PICK_IMAGE_REQUEST) }
        btnUpdateProduct.setOnClickListener { updateProduct() }
        btnDeleteProduct.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun loadProductDetails() {
        database.child(productId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val product = snapshot.getValue(ProductModel::class.java)
                    product?.let {
                        editTextProductName.setText(it.name)
                        editTextProductPrice.setText(it.price)
                        editTextProductDescription.setText(it.description)
                        modelUrl = it.image
                        Glide.with(this@AdminProductDetailsActivity)
                            .load(it.image)
                            .into(productImageView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminProductDetailsActivity, "Failed to load product", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openFileChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            productImageView.setImageURI(selectedImageUri)
        }
    }

    private fun updateProduct() {
        val updatedName = editTextProductName.text.toString().trim()
        val updatedPrice = editTextProductPrice.text.toString().trim()
        val updatedDescription = editTextProductDescription.text.toString().trim()

        if (updatedName.isEmpty() || updatedPrice.isEmpty() || updatedDescription.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Optionally upload image
        if (selectedImageUri != null) {
            uploadImageAndSaveDetails(updatedName, updatedPrice, updatedDescription)
        } else {
            saveProductDetails(updatedName, updatedPrice, updatedDescription, modelUrl)
        }
    }

    private fun uploadImageAndSaveDetails(name: String, price: String, description: String) {
        MediaManager.get().upload(selectedImageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    // Optionally show progress
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    // Show upload progress
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val uploadedUrl = resultData?.get("url") as? String
                    saveProductDetails(name, price, description, uploadedUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@AdminProductDetailsActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun saveProductDetails(name: String, price: String, description: String, imageUrl: String?) {
        val updatedProduct = ProductModel(productId!!, categoryId!!, name, price, description, imageUrl)
        database.child(productId!!).setValue(updatedProduct).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton("Yes") { _, _ ->
                deleteProduct()
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.show()
    }

    private fun deleteProduct() {
        database.child(productId!!).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
