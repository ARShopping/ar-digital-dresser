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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        recyclerViewCart = view.findViewById(R.id.recyclerViewCart)
        emptyCartLayout = view.findViewById(R.id.emptyCartLayout)
        startShoppingButton = view.findViewById(R.id.buttonStartShopping)
        placeOrderButton = view.findViewById(R.id.buttonPlaceOrder)

        setupRecyclerView()

        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            updateUI(cartItems)
            placeOrderButton.isEnabled = cartItems.isNotEmpty()
        }

        startShoppingButton.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_homeFragment)
        }

        placeOrderButton.setOnClickListener {
            if (cartViewModel.cartItems.value?.isNotEmpty() == true) {
                findNavController().navigate(R.id.action_cartFragment_to_orderSummaryFragment)
            }
        }

        return view
    }

    private fun setupRecyclerView() {
        recyclerViewCart.layoutManager = GridLayoutManager(requireContext(), 2)
        cartAdapter = CartAdapter(
            emptyList(),
            onRemoveClick = { cartViewModel.removeItems(listOf(it)) },
            onQuantityChange = { item, newQuantity ->
                cartViewModel.updateQuantity(item, newQuantity)
            },
            onSelectionChanged = { showToolbar(it) }
        )
        recyclerViewCart.adapter = cartAdapter
    }

    private fun updateUI(cartItems: List<CartItem>) {
        cartAdapter.updateItems(cartItems)
        val isEmpty = cartItems.isEmpty()
        emptyCartLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
        placeOrderButton.isEnabled = !isEmpty
    }

    private fun showToolbar(show: Boolean) {
        if (show) {
            if (actionMode == null) {
                actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
                actionMode?.title = "${cartAdapter.getSelectedItems().size} selected"
            }
        } else {
            actionMode?.finish()
            actionMode = null
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.cart_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.title = "${cartAdapter.getSelectedItems().size} selected"
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_delete -> {
                    val selectedItems = cartAdapter.getSelectedItems()
                    cartViewModel.removeItems(selectedItems)
                    cartAdapter.clearSelection()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            cartAdapter.clearSelection()
        }
    }
}