package com.example.inventorymanagement.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Login : AppCompatActivity() {

    private lateinit var BASE_URL: String
    private var selectedBitmap: Bitmap? = null
    private lateinit var profileImage: ImageView

    // --- IMAGE PICKER ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profileImage.setImageURI(uri)
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session Check
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = sharedPref.getString("api_token", null)
        if (token != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
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

        profileImage = findViewById(R.id.iv_profile_logo) // Global variable

        // Inputs
        val loginEmailInput = findViewById<EditText>(R.id.login_email_input)
        val loginPassInput = findViewById<EditText>(R.id.login_password_input)
        val signupName = findViewById<EditText>(R.id.signup_fullname_input)
        val signupBusiness = findViewById<EditText>(R.id.signup_business_input)
        val signupPhone = findViewById<EditText>(R.id.signup_phone_input)
        val signupEmail = findViewById<EditText>(R.id.signup_email_input)
        val signupPass = findViewById<EditText>(R.id.signup_password_input)

        // --- TAB LOGIC ---
        tabSignupInactive.setOnClickListener {
            loginCard.visibility = View.GONE
            signupCard.visibility = View.VISIBLE

            // Enable Image Upload in Signup Mode
            profileImage.isClickable = true
            profileImage.setOnClickListener {
                Toast.makeText(this, "Select Profile Picture", Toast.LENGTH_SHORT).show()
                pickImageLauncher.launch("image/*")
            }
        }

        tabLoginInactive.setOnClickListener {
            signupCard.visibility = View.GONE
            loginCard.visibility = View.VISIBLE

            // Disable click in Login Mode
            profileImage.isClickable = false
            profileImage.setOnClickListener(null)
        }

        // --- BUTTON ACTIONS ---
        btnSignIn.setOnClickListener {
            val emailPhone = loginEmailInput.text.toString().trim()
            val password = loginPassInput.text.toString().trim()
            if (emailPhone.isNotEmpty() && password.isNotEmpty()) {
                performLogin(emailPhone, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnCreateAccount.setOnClickListener {
            val name = signupName.text.toString().trim()
            val bus = signupBusiness.text.toString().trim()
            val phone = signupPhone.text.toString().trim()
            val email = signupEmail.text.toString().trim()
            val pass = signupPass.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                performSignup(name, bus, phone, email, pass)
            } else {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- SIGNUP (MULTIPART) ---
    private fun performSignup(name: String, bus: String, phone: String, email: String, pass: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prepare params map
                val params = HashMap<String, String>()
                params["full_name"] = name
                params["business_name"] = bus
                params["phone"] = phone
                params["email"] = email
                params["password"] = pass

                // Use Multipart Request to send Image + Text
                val response = multipartRequest(BASE_URL + "signup.php", params, selectedBitmap, "image", "profile.jpg")

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

    // --- LOGIN (STANDARD POST) ---
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

    // --- NETWORKING HELPERS ---

    private fun sendPostRequest(url: URL, postData: String): String {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 5000
        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(postData)
        writer.flush()
        writer.close()
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun multipartRequest(urlTo: String, params: Map<String, String>, bitmap: Bitmap?, fileField: String, fileName: String): String {
        val connection = URL(urlTo).openConnection() as HttpURLConnection
        val boundary = "*****" + System.currentTimeMillis() + "*****"
        connection.doInput = true; connection.doOutput = true; connection.useCaches = false
        connection.requestMethod = "POST"
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        val outputStream = DataOutputStream(connection.outputStream)
        val lineEnd = "\r\n"; val twoHyphens = "--"

        // Write Strings
        for ((key, value) in params) {
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)
            outputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd")
            outputStream.writeBytes(lineEnd)
            outputStream.write(value.toByteArray(Charsets.UTF_8))
            outputStream.writeBytes(lineEnd)
        }

        // Write Image
        if (bitmap != null) {
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)
            outputStream.writeBytes("Content-Disposition: form-data; name=\"$fileField\";filename=\"$fileName\"$lineEnd")
            outputStream.writeBytes(lineEnd)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            outputStream.write(baos.toByteArray())
            outputStream.writeBytes(lineEnd)
        }

        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
        outputStream.flush(); outputStream.close()

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    // --- HANDLERS ---

    private fun handleLoginResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
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
                startActivity(Intent(this, MainActivity::class.java))
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

                // Reset image selection for next user
                selectedBitmap = null
                profileImage.setImageResource(R.drawable.ic_launcher_foreground) // Reset to default placeholder
            } else {
                Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Server Error: $response", Toast.LENGTH_LONG).show()
        }
    }
}