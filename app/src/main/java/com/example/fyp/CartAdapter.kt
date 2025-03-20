package com.example.fyp

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private var items: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit,
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onSelectionChanged: (Boolean) -> Unit // Notify toolbar visibility
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val selectedItems = SparseBooleanArray()

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.textViewProductName)
        val quantity: TextView = view.findViewById(R.id.textViewQuantity)
        val buttonIncrease: Button = view.findViewById(R.id.buttonIncrease)
        val buttonDecrease: Button = view.findViewById(R.id.buttonDecrease)
        val buttonRemove: Button = view.findViewById(R.id.buttonRemove)
        val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]

        holder.productName.text = item.productName
        holder.quantity.text = "Quantity: ${item.quantity}"
        holder.selectionOverlay.visibility = if (selectedItems[position, false]) View.VISIBLE else View.GONE

        holder.buttonIncrease.setOnClickListener {
            onQuantityChange(item, item.quantity + 1)
        }

        holder.buttonDecrease.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChange(item, item.quantity - 1)
            }
        }

        holder.buttonRemove.setOnClickListener {
            onRemoveClick(item)
        }

        // Toggle selection on long press
        holder.itemView.setOnLongClickListener {
            toggleSelection(position)
            true
        }

        holder.itemView.setOnClickListener {
            if (selectedItems.size() > 0) { // Allow selection if already selecting
                toggleSelection(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): CartItem {
        return items[position]
    }

    // Toggle item selection
    private fun toggleSelection(position: Int) {
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
        onSelectionChanged(selectedItems.size() > 0) // Notify activity to show/hide toolbar
    }

    // Get selected items for bulk delete
    fun getSelectedItems(): List<CartItem> {
        return items.filterIndexed { index, _ -> selectedItems[index, false] }
    }

    // Clear selection after delete
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged(false) // Hide toolbar
    }
}
