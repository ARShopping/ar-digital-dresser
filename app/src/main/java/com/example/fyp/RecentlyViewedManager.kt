package com.example.fyp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecentlyViewedManager {
    private const val PREF_NAME = "recently_viewed"
    private const val KEY_RECENTS = "recent_products"
    private const val MAX_SIZE = 10

    private val gson = Gson()

    fun saveProduct(context: Context, product: ProductModel) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getProducts(context).toMutableList()

        // Remove if already present to avoid duplication
        currentList.removeAll { it.productId == product.productId }

        // Add to top
        currentList.add(0, product)

        // Limit the size
        if (currentList.size > MAX_SIZE) currentList.removeAt(currentList.lastIndex)

        val json = gson.toJson(currentList)
        prefs.edit().putString(KEY_RECENTS, json).apply()
    }

    fun getProducts(context: Context): List<ProductModel> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENTS, null)
        return if (json != null) {
            val type = object : TypeToken<List<ProductModel>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
