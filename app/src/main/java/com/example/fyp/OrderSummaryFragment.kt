package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class OrderSummaryFragment : Fragment() {

    private lateinit var textOrderTotal: TextView
    private lateinit var buttonConfirmOrder: Button

    private val cartViewModel: CartViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_order_summary, container, false)

        textOrderTotal = view.findViewById(R.id.textOrderTotal)
        buttonConfirmOrder = view.findViewById(R.id.buttonConfirmOrder)

        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            val totalPrice = cartItems.sumOf { it.price * it.quantity }
            textOrderTotal.text = "Total: $${String.format("%.2f", totalPrice)}"
        }

        buttonConfirmOrder.setOnClickListener {
            navigateToDeliveryActivity()
        }

        return view
    }

    private fun navigateToDeliveryActivity() {
        cartViewModel.cartItems.value?.let { cartItems ->
            if (cartItems.isNotEmpty()) {
                val firstProduct = cartItems[0] // Assuming passing details of the first product

                val intent = Intent(requireContext(), DeliveryActivity::class.java).apply {
                    putExtra("productName", firstProduct.productName)
                    putExtra("productPrice", firstProduct.price.toString())
                    putExtra("selectedSize", "Default Size") // Update if you track sizes
                }
                startActivity(intent)
            }
        }
    }
}
