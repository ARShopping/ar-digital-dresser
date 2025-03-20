package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navigationView: NavigationView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup DrawerLayout & NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_menu)

        // Setup Navigation Components
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        // Define top-level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_message, R.id.nav_cart, R.id.nav_account),
            drawerLayout
        )

        // Setup NavigationUI
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigationView.setupWithNavController(navController)
        navigationView.setupWithNavController(navController)

        // Hide Bottom Navigation in specific fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationView.visibility = when (destination.id) {
                else -> BottomNavigationView.VISIBLE
            }
        }

        // Load categories dynamically into Navigation Drawer
        database = FirebaseDatabase.getInstance().getReference("categories")
        loadCategoriesIntoNavigationDrawer()

        // Handle Back Press Properly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false // Allow normal back press behavior
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadCategoriesIntoNavigationDrawer() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val menu: Menu = navigationView.menu
                menu.clear() // Clear existing items before updating

                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(CategoryModel::class.java)
                    if (category != null) {
                        val menuItem = menu.add(Menu.NONE, category.categoryId.hashCode(), Menu.NONE, category.name)

                        menuItem.setOnMenuItemClickListener {
                            // ✅ Pass categoryId and categoryName to ProductListActivity
                            val intent = Intent(this@MainActivity, ProductListActivity::class.java)
                            intent.putExtra("categoryId", category.categoryId)  // ✅ Pass categoryId
                            intent.putExtra("categoryName", category.name)  // ✅ Pass categoryName
                            startActivity(intent)
                            drawerLayout.closeDrawer(GravityCompat.START)
                            true
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
