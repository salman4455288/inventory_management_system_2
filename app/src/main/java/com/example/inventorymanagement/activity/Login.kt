package com.example.inventorymanagement.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagement.R
import com.example.inventorymanagement.util.BaseURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Login : AppCompatActivity() {

    private lateinit var BASE_URL: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. SESSION CHECK (Add this block at the very top) ---
        // Check if user is already logged in before loading the view
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = sharedPref.getString("api_token", null)

        if (token != null) {
            // User has a token, skip login screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close Login activity so they can't go back to it
            return // Stop the rest of onCreate
        }
        // ---------------------------------------------------------

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Base URL
        BASE_URL = BaseURL.getUrl(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        val loginCard = findViewById<LinearLayout>(R.id.card_login_container)
        val signupCard = findViewById<LinearLayout>(R.id.card_signup_container)

        val tabSignupInactive = findViewById<Button>(R.id.tab_signup_inactive)
        val tabLoginInactive = findViewById<Button>(R.id.tab_login_inactive)

        val btnSignIn = findViewById<Button>(R.id.btn_action_signin)
        val btnCreateAccount = findViewById<Button>(R.id.btn_action_create_account)
        val profileImage = findViewById<ImageView>(R.id.iv_profile_logo)

        // Login Inputs
        val loginEmailInput = findViewById<EditText>(R.id.login_email_input)
        val loginPassInput = findViewById<EditText>(R.id.login_password_input)

        // Signup Inputs
        val signupName = findViewById<EditText>(R.id.signup_fullname_input)
        val signupBusiness = findViewById<EditText>(R.id.signup_business_input)
        val signupPhone = findViewById<EditText>(R.id.signup_phone_input)
        val signupEmail = findViewById<EditText>(R.id.signup_email_input)
        val signupPass = findViewById<EditText>(R.id.signup_password_input)

        tabSignupInactive.setOnClickListener {
            loginCard.visibility = View.GONE
            signupCard.visibility = View.VISIBLE
            profileImage.isClickable = true
            profileImage.setOnClickListener {
                Toast.makeText(this, "Tap to upload profile picture", Toast.LENGTH_SHORT).show()
            }
        }

        tabLoginInactive.setOnClickListener {
            signupCard.visibility = View.GONE
            loginCard.visibility = View.VISIBLE
            profileImage.isClickable = false
            profileImage.setOnClickListener(null)
        }

        // --- LOGIN BUTTON ACTION ---
        btnSignIn.setOnClickListener {
            val emailPhone = loginEmailInput.text.toString().trim()
            val password = loginPassInput.text.toString().trim()

            if (emailPhone.isNotEmpty() && password.isNotEmpty()) {
                performLogin(emailPhone, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // --- SIGNUP BUTTON ACTION ---
        btnCreateAccount.setOnClickListener {
            val name = signupName.text.toString().trim()
            val business = signupBusiness.text.toString().trim()
            val phone = signupPhone.text.toString().trim()
            val email = signupEmail.text.toString().trim()
            val pass = signupPass.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                performSignup(name, business, phone, email, pass)
            } else {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSignup(name: String, bus: String, phone: String, email: String, pass: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "signup.php")
                val postData = "full_name=" + URLEncoder.encode(name, "UTF-8") +
                        "&business_name=" + URLEncoder.encode(bus, "UTF-8") +
                        "&phone=" + URLEncoder.encode(phone, "UTF-8") +
                        "&email=" + URLEncoder.encode(email, "UTF-8") +
                        "&password=" + URLEncoder.encode(pass, "UTF-8")

                val response = sendPostRequest(url, postData)

                withContext(Dispatchers.Main) {
                    handleSignupResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performLogin(emailPhone: String, pass: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "login.php")
                val postData = "email_phone=" + URLEncoder.encode(emailPhone, "UTF-8") +
                        "&password=" + URLEncoder.encode(pass, "UTF-8")

                val response = sendPostRequest(url, postData)

                withContext(Dispatchers.Main) {
                    handleLoginResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendPostRequest(url: URL, postData: String): String {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.connectTimeout = 5000

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(postData)
        writer.flush()
        writer.close()

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        return response.toString()
    }

    private fun handleLoginResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                // Save User Session
                val user = json.getJSONObject("user")
                val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                with (sharedPref.edit()) {
                    putInt("user_id", user.getInt("id"))
                    putString("full_name", user.getString("full_name"))
                    putString("api_token", user.getString("api_token"))
                    if(user.has("profile_image") && !user.isNull("profile_image")) {
                        putString("profile_image", user.getString("profile_image"))
                    }
                    apply()
                }

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Server Error: $response", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSignupResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                Toast.makeText(this, "Account Created! Please Login.", Toast.LENGTH_SHORT).show()
                findViewById<LinearLayout>(R.id.card_signup_container).visibility = View.GONE
                findViewById<LinearLayout>(R.id.card_login_container).visibility = View.VISIBLE
            } else {
                Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Server Error: $response", Toast.LENGTH_LONG).show()
        }
    }
}