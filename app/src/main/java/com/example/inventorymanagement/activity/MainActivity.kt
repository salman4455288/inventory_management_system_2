package com.example.inventorymanagement.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.inventorymanagement.R
import com.example.inventorymanagement.fragment.*
import com.example.inventorymanagement.util.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navPos: LinearLayout
    private lateinit var navInventory: LinearLayout
    private lateinit var navCustomers: LinearLayout
    private lateinit var navReports: LinearLayout

    // --- 1. PERMISSION LAUNCHER ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, notifications will work
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Channels
        NotificationHelper.createNotificationChannels(this)

        // --- 2. ASK FOR PERMISSION (ANDROID 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize views
        navHome = findViewById(R.id.nav_home)
        navPos = findViewById(R.id.nav_pos)
        navInventory = findViewById(R.id.nav_inventory)
        navCustomers = findViewById(R.id.nav_customers)
        navReports = findViewById(R.id.nav_reports)

        // --- 3. LIFECYCLE CHECK (CRITICAL FOR CAMERA) ---
        // Only load HomeFragment if this is a fresh start.
        // If coming back from Camera (savedInstanceState != null), do NOTHING.
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            highlightTab(navHome)
        }

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

    // Change to 'public' so fragments can access it
    public fun loadFragment(fragment: Fragment) {
        // OPTIMIZATION: Check if the fragment is already currently displayed.
        // This prevents reloading the same fragment (and wasting memory) if the user taps the same tab twice.
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null && currentFragment::class.java == fragment::class.java) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Change to 'public' so fragments can access it
    public fun highlightTab(navId: Int) {
        val selectedLayout = when(navId) {
            R.id.nav_home -> navHome
            R.id.nav_pos -> navPos
            R.id.nav_inventory -> navInventory
            R.id.nav_customers -> navCustomers
            R.id.nav_reports -> navReports
            else -> navHome
        }
        highlightTab(selectedLayout)
    }

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