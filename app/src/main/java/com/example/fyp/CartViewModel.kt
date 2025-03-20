package com.example.fyp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartViewModel : ViewModel() {
    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val cartRef = userId?.let { database.child("users").child(it).child("cart") }

    init {
        if (userId != null) {
            fetchCartItems()
        }
    }

    private fun fetchCartItems() {
        cartRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cartList = mutableListOf<CartItem>()
                for (itemSnapshot in snapshot.children) {
                    val cartItem = itemSnapshot.getValue(CartItem::class.java)
                    cartItem?.let { cartList.add(it) }
                }
                _cartItems.value = cartList
            }

            override fun onCancelled(error: DatabaseError) {
                // Log database error if needed
            }
        })
    }

    fun removeItems(cartItems: List<CartItem>) {
        // Remove selected items from the cart in Firebase
        cartItems.forEach { item ->
            if (!item.productId.isNullOrBlank()) {
                cartRef?.child(item.productId)?.removeValue()
            }
        }
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        if (!item.productId.isNullOrBlank()) {
            val updatedValues = mapOf(
                "quantity" to newQuantity,
                "price" to item.price // Ensure price is updated properly
            )
            cartRef?.child(item.productId)?.updateChildren(updatedValues)
        }
    }
}
