package com.example.inventorymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.inventorymanagement.R
import org.json.JSONObject
import java.nio.charset.Charset

class Login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val emailPhoneInput = findViewById<EditText>(R.id.email_phone_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val signInButton = findViewById<Button>(R.id.signin_button)
        val signupTab = findViewById<Button>(R.id.signup_tab)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)

        signInButton.setOnClickListener {
            val emailOrPhone = emailPhoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (emailOrPhone.isEmpty()) {
                emailPhoneInput.error = "Email or phone is required"
                emailPhoneInput.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            loginUser(emailOrPhone, password)
        }

        signupTab.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser(emailOrPhone: String, password: String) {
        val url = "http://192.168.18.130/inventorymanagment/login.php"
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.getInt("status")
                    val message = jsonResponse.getString("message")

                    if (status == 1) {
                        val userData = jsonResponse.getJSONObject("data")

                        val userId = userData.getInt("id")
                        val fullName = userData.getString("fullName")
                        val businessName = userData.getString("businessName")
                        val email = userData.getString("email")
                        val phoneNumber = userData.getString("phoneNumber")
                        val profilePicture =
                            if (userData.has("profilePicture") && !userData.isNull("profilePicture"))
                                userData.getString("profilePicture")
                            else null

                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putInt("userId", userId)
                        editor.putString("fullName", fullName)
                        editor.putString("businessName", businessName)
                        editor.putString("email", email)
                        editor.putString("phoneNumber", phoneNumber)
                        editor.putString("profilePicture", profilePicture)
                        editor.putBoolean("isLoggedIn", true)
                        editor.apply()

                        Toast.makeText(this, "Welcome back, $fullName!", Toast.LENGTH_LONG).show()

                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
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
                    "emailOrPhone" to emailOrPhone,
                    "password" to password
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
