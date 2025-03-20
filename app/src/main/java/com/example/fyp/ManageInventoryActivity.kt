package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ManageInventoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryList: MutableList<CategoryModel>
    private lateinit var database: DatabaseReference
    private lateinit var addCategoryFab: ImageButton

    private var selectedCategory: CategoryModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_inventory)

        database = FirebaseDatabase.getInstance().getReference("categories")
        categoryList = mutableListOf()

        recyclerView = findViewById(R.id.recyclerViewCategories)
        addCategoryFab = findViewById(R.id.btnAddCategory)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with long-press functionality for admin
        categoryAdapter = CategoryAdapter(
            categoryList,
            { category -> openCategory(category) },  // Click Listener
            { category, view ->
                selectedCategory = category
                registerForContextMenu(view)
                openContextMenu(view)
            }
        )

        recyclerView.adapter = categoryAdapter

        loadCategories()
        addCategoryFab.setOnClickListener { showAddCategoryDialog() }
    }

    private fun loadCategories() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(CategoryModel::class.java)
                    category?.let { categoryList.add(it) }
                }
                categoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageInventoryActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ FIXED: Function to Add New Category
    private fun showAddCategoryDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Add New Category")

        val inputName = EditText(this)
        inputName.hint = "Enter category name"
        dialog.setView(inputName)

        dialog.setPositiveButton("Add") { _, _ ->
            val categoryName = inputName.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                val categoryId = database.push().key ?: return@setPositiveButton
                val newCategory = CategoryModel(categoryId, categoryName)

                database.child(categoryId).setValue(newCategory)
                    .addOnSuccessListener { Toast.makeText(this, "Category added!", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show() }
            } else {
                Toast.makeText(this, "Category name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun openCategory(category: CategoryModel) {
        val intent = Intent(this, AdminProductListActivity::class.java)
        intent.putExtra("categoryId", category.categoryId)
        intent.putExtra("categoryName", category.name)
        startActivity(intent)
    }

    // ✅ Context Menu for Admin (Long Press)
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Category Options")
        menu.add(0, 1, 0, "Edit Name")
        menu.add(0, 2, 0, "Delete Category")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showEditCategoryDialog()
                true
            }
            2 -> {
                showDeleteCategoryDialog()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // ✅ FIXED: Edit Category Name
    private fun showEditCategoryDialog() {
        val category = selectedCategory ?: return
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Edit Category")

        val inputName = EditText(this)
        inputName.setText(category.name)
        dialog.setView(inputName)

        dialog.setPositiveButton("Save") { _, _ ->
            val newName = inputName.text.toString().trim()
            if (newName.isNotEmpty()) {
                database.child(category.categoryId).child("name").setValue(newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update category", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    // ✅ FIXED: Delete Category Confirmation
    private fun showDeleteCategoryDialog() {
        val category = selectedCategory ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Yes") { _, _ ->
                database.child(category.categoryId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete category", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
