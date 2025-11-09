package com.example.inventorymanagement.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.inventorymanagement.R
import com.example.inventorymanagement.fragment.CustomersFragment
import com.example.inventorymanagement.fragment.HomeFragment
import com.example.inventorymanagement.fragment.InventoryFragment
import com.example.inventorymanagement.fragment.PosFragment
import com.example.inventorymanagement.fragment.ReportsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.transition.MaterialSharedAxis

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Load default fragment
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_pos -> PosFragment()
                R.id.nav_inventory -> InventoryFragment()
                R.id.nav_customers -> CustomersFragment()
                R.id.nav_reports -> ReportsFragment()
                else -> HomeFragment()
            }

            // Apply Material Shared Axis (horizontal) transitions
            val forward = MaterialSharedAxis(MaterialSharedAxis.X, true)
            val backward = MaterialSharedAxis(MaterialSharedAxis.X, false)
            selectedFragment.enterTransition = forward
            selectedFragment.exitTransition = backward

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()


            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}