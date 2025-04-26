package com.example.fyp

data class CartItem(
    val productId: String? = null,
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String? = null
) {
    // Add a no-argument constructor for Firebase
    constructor() : this(null, "", 0.0, 1, null)
}