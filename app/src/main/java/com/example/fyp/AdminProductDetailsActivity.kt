package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            productImageView.setImageURI(it)

            val tempImageFile = createTempFileFromUri(it)
            if (tempImageFile != null) {
                val name = editTextProductName.text.toString().trim()
                val price = editTextProductPrice.text.toString().trim()
                val description = editTextProductDescription.text.toString().trim()

                if (name.isEmpty() || price.isEmpty() || description.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields before changing image", Toast.LENGTH_SHORT).show()
                    return@let
                }

                removeBackgroundAndUploadImage(tempImageFile, name, price, description)
            } else {
                Toast.makeText(this, "Failed to read selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        btnChangeImage.setOnClickListener { openFileChooser() }
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

    private fun openFileChooser() {
        imagePickerLauncher.launch("image/*")
    }

    private fun updateProduct() {
        val updatedName = editTextProductName.text.toString().trim()
        val updatedPrice = editTextProductPrice.text.toString().trim()
        val updatedDescription = editTextProductDescription.text.toString().trim()

        if (updatedName.isEmpty() || updatedPrice.isEmpty() || updatedDescription.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        saveProductDetails(updatedName, updatedPrice, updatedDescription, modelUrl)
    }

    private fun removeBackgroundAndUploadImage(imageFile: File, name: String, price: String, description: String) {
        val mediaType = "image/*".toMediaTypeOrNull()
        val requestBody = imageFile.asRequestBody(mediaType)
        val body = MultipartBody.Part.createFormData("image_file", imageFile.name, requestBody)

        val removeBgRequest = RetrofitInstance.service.removeBackground(body)

        removeBgRequest.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val imageStream = response.body()?.byteStream()
                    if (imageStream != null) {
                        val bitmap = BitmapFactory.decodeStream(imageStream)
                        uploadToCloudinary(bitmap, name, price, description)
                    } else {
                        Toast.makeText(this@AdminProductDetailsActivity, "Image stream is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AdminProductDetailsActivity, "Failed: $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@AdminProductDetailsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadToCloudinary(bitmap: Bitmap, name: String, price: String, description: String) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val tempFile = File(cacheDir, "upload.png")
        FileOutputStream(tempFile).use {
            it.write(byteArray)
        }

        MediaManager.get().upload(tempFile.path)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val uploadedUrl = resultData?.get("url") as? String
                    saveProductDetails(name, price, description, uploadedUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@AdminProductDetailsActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                    Log.e("Cloudinary", "Upload error: ${error?.description}")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }


    private fun saveProductDetails(name: String, price: String, description: String, imageUrl: String?) {
        val updatedProduct = ProductModel(productId!!, categoryId!!, name, price, description, imageUrl ?: "")
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
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton("Yes") { _, _ -> deleteProduct() }
            .setNegativeButton("No", null)
            .create()
            .show()
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

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "${System.currentTimeMillis()}.jpg"
            val tmp = File(cacheDir, name)
            input.use { inp ->
                FileOutputStream(tmp).use { out ->
                    inp.copyTo(out)
                }
            }
            tmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
