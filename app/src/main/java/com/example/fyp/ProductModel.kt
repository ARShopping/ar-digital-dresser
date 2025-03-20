package com.example.fyp

data class ProductModel(
    val productId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val price: String = "",
    val description: String = "",
    var modelUrl: String? = null,  // Ensure this exists
    val image: String = "" // ✅ Ensure this stores Cloudinary URL
)

