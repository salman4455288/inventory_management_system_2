package com.example.inventorymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.inventorymanagement.R
import org.json.JSONObject

class Signup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        // Get references to all input fields
        val fullNameInput = findViewById<EditText>(R.id.fullname_input)
        val businessInput = findViewById<EditText>(R.id.business_input)
        val phoneInput = findViewById<EditText>(R.id.phone_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val createAccountButton = findViewById<Button>(R.id.create_account_button)

        createAccountButton.setOnClickListener {
            // Get input values
            val fullName = fullNameInput.text.toString().trim()
            val businessName = businessInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate inputs
            if (fullName.isEmpty()) {
                fullNameInput.error = "Full name is required"
                fullNameInput.requestFocus()
                return@setOnClickListener
            }

            if (businessName.isEmpty()) {
                businessInput.error = "Business name is required"
                businessInput.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                phoneInput.error = "Phone number is required"
                phoneInput.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            // All validations passed, proceed with registration
            registerUser(fullName, businessName, phone, email, password)
        }
    }

    private fun registerUser(
        fullName: String,
        businessName: String,
        phone: String,
        email: String,
        password: String
    ) {
        // Replace with your actual server URL
        val url = "http://192.168.18.130/inventorymanagment/register.php"

        // Show loading message
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    // Parse JSON response
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.getInt("status")
                    val message = jsonResponse.getString("message")

                    if (status == 1) {
                        // Registration successful
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                        // Optional: Save user data to SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("email", email)
                        editor.putString("fullName", fullName)
                        editor.putString("businessName", businessName)
                        if (jsonResponse.has("userId")) {
                            editor.putInt("userId", jsonResponse.getInt("userId"))
                        }
                        editor.apply()

                        // Navigate to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Registration failed
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                // Network error
                val errorMessage = when {
                    error.networkResponse != null -> {
                        "Server error: ${error.networkResponse.statusCode}"
                    }
                    error.message != null -> {
                        "Error: ${error.message}"
                    }
                    else -> {
                        "Network error. Please check your connection."
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "fullName" to fullName,
                    "businessName" to businessName,
                    "phoneNumber" to phone,
                    "email" to email,
                    "password" to password
                )
            }
        }

        // Add request to queue
        Volley.newRequestQueue(this).add(request)
    }
}