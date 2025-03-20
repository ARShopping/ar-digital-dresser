package com.example.fyp

data class CartItem(
    var productId: String = "",
    var productName: String = "",
    var quantity: Int = 1,
    var price: Double = 0.0 // Added price field for calculations
)
