package com.example.fyp

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CartFragment : Fragment() {

    private lateinit var recyclerViewCart: RecyclerView
    private lateinit var emptyCartLayout: LinearLayout
    private lateinit var startShoppingButton: Button
    private lateinit var placeOrderButton: FloatingActionButton
    private lateinit var cartAdapter: CartAdapter

    private val cartViewModel: CartViewModel by activityViewModels()
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        recyclerViewCart = view.findViewById(R.id.recyclerViewCart)
        emptyCartLayout = view.findViewById(R.id.emptyCartLayout)
        startShoppingButton = view.findViewById(R.id.buttonStartShopping)
        placeOrderButton = view.findViewById(R.id.buttonPlaceOrder)

        // Set up RecyclerView and Adapter
        setupRecyclerView()

        // Observe Cart items in the ViewModel
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            updateUI(cartItems)
        }

        // Start shopping button navigation
        startShoppingButton.setOnClickListener {
            if (isAdded) {
                findNavController().navigate(R.id.action_cartFragment_to_homeFragment)
            }
        }

        // Place order button navigation
        placeOrderButton.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_orderSummaryFragment)
        }

        return view
    }

    private fun setupRecyclerView() {
        // Set up the layout manager and the adapter for RecyclerView
        recyclerViewCart.layoutManager = GridLayoutManager(requireContext(), 2)
        cartAdapter = CartAdapter(
            emptyList(),
            onRemoveClick = { cartViewModel.removeItems(listOf(it)) },  // Fixed function call
            onQuantityChange = { item, newQuantity -> cartViewModel.updateQuantity(item, newQuantity) },
            onSelectionChanged = { showToolbar(it) } // Notify when selection changes
        )
        recyclerViewCart.adapter = cartAdapter
    }

    private fun updateUI(cartItems: List<CartItem>) {
        // Update the adapter's list of items
        cartAdapter.updateItems(cartItems)

        // Show or hide the empty cart view
        emptyCartLayout.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewCart.visibility = if (cartItems.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showToolbar(show: Boolean) {
        // Show or hide the action mode (toolbar) for bulk delete
        if (show) {
            if (actionMode == null) {
                actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
            }
        } else {
            actionMode?.finish() // Close the action mode (toolbar) when no items are selected
        }
    }

    // ActionMode callback for handling toolbar actions (delete selected items)
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate the action mode menu (delete option)
            mode?.menuInflater?.inflate(R.menu.cart_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Prepare action mode (nothing to do in this case)
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.action_delete -> {
                    // Handle bulk delete action
                    val selectedItems = cartAdapter.getSelectedItems()
                    cartViewModel.removeItems(selectedItems)  // Remove selected items
                    cartAdapter.clearSelection() // Clear selection after deletion
                    return true
                }
                else -> return false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            cartAdapter.clearSelection() // Clear selection when action mode is destroyed
        }
    }
}
