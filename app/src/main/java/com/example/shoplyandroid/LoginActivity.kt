package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username == "admin" && password == "123456") {
                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", true)
                prefs.putString("USERNAME", username)
                prefs.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()

            } else if (username == "eti" && password == "123456") {
                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", false)
                prefs.putString("USERNAME", username)
                prefs.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()

            } else {
                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", false)
                prefs.putString("USERNAME", "")
                prefs.apply()

                Toast.makeText(this, "שם משתמש או סיסמה שגויים", Toast.LENGTH_SHORT).show()
            }
        }
    }
}