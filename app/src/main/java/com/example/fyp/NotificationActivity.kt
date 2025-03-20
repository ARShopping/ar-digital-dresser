package com.example.fyp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // Find the dismiss buttons
        val btnDismissFlashSale = findViewById<Button>(R.id.btnDismissFlashSale)
        val btnDismissFreeDelivery = findViewById<Button>(R.id.btnDismissFreeDelivery)
        val btnDismissSpecialOffer = findViewById<Button>(R.id.btnDismissSpecialOffer)
        val btnDismissLimitedTimeDiscount = findViewById<Button>(R.id.btnDismissLimitedTimeDiscount)
        val btnDismissCashbackOffer = findViewById<Button>(R.id.btnDismissCashbackOffer)

        // Find the CardViews
        val flashSaleCard = findViewById<CardView>(R.id.flashSaleLayout)
        val freeDeliveryCard = findViewById<CardView>(R.id.freeDeliveryLayout)
        val specialOfferCard = findViewById<CardView>(R.id.specialOfferLayout)
        val limitedTimeDiscountCard = findViewById<CardView>(R.id.limitedTimeDiscountLayout)
        val cashbackOfferCard = findViewById<CardView>(R.id.cashbackOfferLayout)

        // Set onClickListeners to dismiss notifications
        btnDismissFlashSale.setOnClickListener {
            flashSaleCard.visibility = View.GONE
        }

        btnDismissFreeDelivery.setOnClickListener {
            freeDeliveryCard.visibility = View.GONE
        }

        btnDismissSpecialOffer.setOnClickListener {
            specialOfferCard.visibility = View.GONE
        }

        btnDismissLimitedTimeDiscount.setOnClickListener {
            limitedTimeDiscountCard.visibility = View.GONE
        }

        btnDismissCashbackOffer.setOnClickListener {
            cashbackOfferCard.visibility = View.GONE
        }
    }
}
