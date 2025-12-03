package com.example.inventorymanagement.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagement.R

class settingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_scroll)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- BIND BUTTONS ---
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges)

        // --- LOGOUT LOGIC ---
        btnLogout.setOnClickListener {
            // 1. Clear SharedPreferences (Delete Session)
            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear() // Removes everything: token, name, id
            editor.apply()

            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()

            // 2. Redirect to Login Activity
            val intent = Intent(this, Login::class.java)
            // Clear the activity stack so user cannot press "Back" to return to settings
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // --- SAVE CHANGES LOGIC (Placeholder) ---
        btnSaveChanges.setOnClickListener {
            // TODO: Implement API call to save settings
            Toast.makeText(this, "Settings Saved (Demo)", Toast.LENGTH_SHORT).show()
            finish() // Go back
        }
    }
}