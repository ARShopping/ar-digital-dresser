package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class AddProductActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etProductPrice: EditText
    private lateinit var etProductDescription: EditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnSaveProduct: Button
    private lateinit var productImageView: ImageView

    private var imageUri: Uri? = null
    private lateinit var database: DatabaseReference
    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        etProductName = findViewById(R.id.etProductName)
        etProductPrice = findViewById(R.id.etProductPrice)
        etProductDescription = findViewById(R.id.etProductDescription)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSaveProduct = findViewById(R.id.btnSaveProduct)
        productImageView = findViewById(R.id.productImageView)

        categoryId = intent.getStringExtra("categoryId")
        if (categoryId.isNullOrEmpty()) {
            Toast.makeText(this, "Category ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("products").child(categoryId!!)

        btnSelectImage.setOnClickListener { openImagePicker() }
        btnSaveProduct.setOnClickListener { uploadProduct() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            productImageView.setImageURI(imageUri)
        }
    }

    private fun uploadProduct() {
        val productName = etProductName.text.toString().trim()
        val productPrice = etProductPrice.text.toString().trim()
        val productDescription = etProductDescription.text.toString().trim()

        if (productName.isEmpty() || productPrice.isEmpty() || productDescription.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPrice(productPrice)) {
            Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        val productId = database.push().key ?: return

        lifecycleScope.launch {
            val imageFile = createTempFileFromUri(imageUri!!)
            if (imageFile == null) {
                Toast.makeText(this@AddProductActivity, "Failed to read image file", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)
            val multipartBody = MultipartBody.Part.createFormData("image_file", imageFile.name, requestFile)

            RetrofitInstance.service.removeBackground(multipartBody).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        val removedBgFile = File(cacheDir, "removed-bg-${System.currentTimeMillis()}.png")
                        response.body()?.byteStream()?.use { input ->
                            FileOutputStream(removedBgFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        lifecycleScope.launch {
                            val cloudinaryUrl = CloudinaryManager.uploadImage(this@AddProductActivity, Uri.fromFile(removedBgFile))
                            if (cloudinaryUrl != null) {
                                val product = ProductModel(
                                    productId = productId,
                                    categoryId = categoryId!!,
                                    name = productName,
                                    price = productPrice,
                                    description = productDescription,
                                    image = cloudinaryUrl
                                )
                                saveProductToDatabase(product)
                            } else {
                                Toast.makeText(this@AddProductActivity, "Cloud upload failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@AddProductActivity, "Background removal failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@AddProductActivity, "API call failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveProductToDatabase(product: ProductModel) {
        database.child(product.productId).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Product Added Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidPrice(price: String): Boolean {
        return price.toDoubleOrNull() != null && price.toDouble() > 0
    }

    private suspend fun createTempFileFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val fileName = getFileName(uri) ?: "temp_image"
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
