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
    private val onSelectionChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val selectedItems = SparseBooleanArray()
    private var isSelectionMode = false

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

        // Show/hide buttons based on selection mode
        val buttonsVisibility = if (isSelectionMode) View.GONE else View.VISIBLE
        holder.buttonIncrease.visibility = buttonsVisibility
        holder.buttonDecrease.visibility = buttonsVisibility
        holder.buttonRemove.visibility = buttonsVisibility

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

        holder.itemView.setOnLongClickListener {
            toggleSelection(position)
            true
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        isSelectionMode = selectedItems.size() > 0
        notifyItemChanged(position)
        onSelectionChanged(isSelectionMode)
    }

    fun getSelectedItems(): List<CartItem> {
        return items.filterIndexed { index, _ -> selectedItems[index, false] }
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(false)
    }
}