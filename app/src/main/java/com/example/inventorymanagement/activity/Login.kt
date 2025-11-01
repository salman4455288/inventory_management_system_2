package com.example.inventorymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagement.activity.MainActivity
import com.example.inventorymanagement.R

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        var signupbtn=findViewById<Button>(R.id.signup_tab)
        var signinbtn=findViewById<Button>(R.id.signin_button)
        signinbtn.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        signupbtn.setOnClickListener {
            val intent = android.content.Intent(this, Signup::class.java)
            startActivity(intent)
        }

    }
}