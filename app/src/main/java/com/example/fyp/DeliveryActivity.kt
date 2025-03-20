package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DeliveryActivity : AppCompatActivity() {

    private lateinit var spinnerCity: Spinner
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPostalCode: EditText
    private lateinit var etPhone: EditText
    private lateinit var rgPaymentMethod: RadioGroup
    private lateinit var tvConfirmationNote: TextView
    private lateinit var tvSelectedProduct: TextView
    private lateinit var btnSubmit: MaterialButton

    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery)

        // Initialize Views
        spinnerCity = findViewById(R.id.spinnerCity)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etAddress = findViewById(R.id.etAddress)
        etPostalCode = findViewById(R.id.etPostalCode)
        etPhone = findViewById(R.id.etPhone)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        tvConfirmationNote = findViewById(R.id.tvConfirmationNote)
        tvSelectedProduct = findViewById(R.id.tvSelectedProduct)
        btnSubmit = findViewById(R.id.btnSubmit)

        // Initialize Firebase Database
        databaseRef = FirebaseDatabase.getInstance().getReference("orders")

        // Populate City Spinner
        val cities = arrayOf("Select City", "Lahore", "Karachi", "Islamabad", "Faisalabad", "Multan", "Rawalpindi", "Peshawar", "Quetta")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cities)
        spinnerCity.adapter = adapter

        // Retrieve Intent Data
        val productName = intent.getStringExtra("productName") ?: "Unknown Product"
        val productPrice = intent.getStringExtra("productPrice") ?: "N/A"
        val selectedSize = intent.getStringExtra("selectedSize") ?: "Default Size"

        // Display Product Info
        tvSelectedProduct.text = "Product: $productName\nSize: $selectedSize\nPrice: $productPrice"

        Log.d("DeliveryActivity", "Product: $productName, Price: $productPrice, Size: $selectedSize")

        // Submit Button Click Listener
        btnSubmit.setOnClickListener {
            if (validateInput()) {
                saveOrderToFirebase(productName, productPrice, selectedSize)
            }
        }
    }

    private fun validateInput(): Boolean {
        if (spinnerCity.selectedItemPosition == 0) {
            showToast("Please select a city")
            return false
        }
        if (etFirstName.text.toString().isEmpty() || etLastName.text.toString().isEmpty()) {
            showToast("Enter your full name")
            return false
        }
        if (etAddress.text.toString().isEmpty()) {
            showToast("Enter your address")
            return false
        }
        if (etPostalCode.text.toString().length != 6) {
            showToast("Enter a valid 6-digit postal code")
            return false
        }
        if (etPhone.text.toString().length != 11) {
            showToast("Enter a valid 11-digit phone number")
            return false
        }
        if (rgPaymentMethod.checkedRadioButtonId == -1) {
            showToast("Select a payment method")
            return false
        }
        return true
    }

    private fun saveOrderToFirebase(productName: String, productPrice: String, selectedSize: String) {
        val orderId = databaseRef.push().key
        if (orderId == null) {
            showToast("Error generating order ID")
            return
        }

        val paymentMethod = if (rgPaymentMethod.checkedRadioButtonId == R.id.rbCreditCard) "Credit Card" else "Cash on Delivery"

        val orderDetails = mapOf(
            "orderId" to orderId,
            "product" to productName,
            "size" to selectedSize,
            "price" to productPrice,
            "firstName" to etFirstName.text.toString(),
            "lastName" to etLastName.text.toString(),
            "address" to etAddress.text.toString(),
            "postalCode" to etPostalCode.text.toString(),
            "phone" to etPhone.text.toString(),
            "city" to spinnerCity.selectedItem.toString(),
            "paymentMethod" to paymentMethod
        )

        databaseRef.child(orderId).setValue(orderDetails)
            .addOnSuccessListener {
                showToast("✅ Order Confirmed!")
                tvConfirmationNote.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                showToast("❌ Order Failed! Try again.")
                Log.e("DeliveryActivity", "Error saving order: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
