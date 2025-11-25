package com.example.inventorymanagement.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.inventorymanagement.R
import com.example.inventorymanagement.fragment.*

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navPos: LinearLayout
    private lateinit var navInventory: LinearLayout
    private lateinit var navCustomers: LinearLayout
    private lateinit var navReports: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        navHome = findViewById(R.id.nav_home)
        navPos = findViewById(R.id.nav_pos)
        navInventory = findViewById(R.id.nav_inventory)
        navCustomers = findViewById(R.id.nav_customers)
        navReports = findViewById(R.id.nav_reports)

        // Default load home
        loadFragment(HomeFragment())
        highlightTab(navHome)

        // Handle navigation clicks
        navHome.setOnClickListener {
            loadFragment(HomeFragment())
            highlightTab(navHome)
        }
        navPos.setOnClickListener {
            loadFragment(PosFragment())
            highlightTab(navPos)
        }
        navInventory.setOnClickListener {
            loadFragment(InventoryFragment())
            highlightTab(navInventory)
        }
        navCustomers.setOnClickListener {
            loadFragment(CustomersFragment())
            highlightTab(navCustomers)
        }
        navReports.setOnClickListener {
            loadFragment(ReportsFragment())
            highlightTab(navReports)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Highlights the selected navigation item and resets others
     */
    private fun highlightTab(selected: LinearLayout) {
        val navItems = listOf(navHome, navPos, navInventory, navCustomers, navReports)
        val activeColor = ContextCompat.getColor(this, R.color.teal_200)
        val inactiveColor = ContextCompat.getColor(this, R.color.black)

        navItems.forEach { item ->
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView

            if (item == selected) {
                icon.setColorFilter(activeColor)
                label.setTextColor(activeColor)
            } else {
                icon.setColorFilter(inactiveColor)
                label.setTextColor(inactiveColor)
            }
        }
    }
}
