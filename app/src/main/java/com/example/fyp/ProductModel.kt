package com.example.fyp

data class ProductModel(
    val productId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val price: String = "",
    val image: String = "",
    val description: String = "",
    val timestamp: Long = 0L  // Required for sorting
)


