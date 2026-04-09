package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvProfileGreeting = findViewById<TextView>(R.id.tvProfileGreeting)
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val etDisplayName = findViewById<EditText>(R.id.etDisplayName)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLocation = findViewById<Button>(R.id.btnLocation)

        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "לא ידוע") ?: "לא ידוע"
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        val displayNameKey = "DISPLAY_NAME_$username"
        val savedDisplayName = prefs.getString(displayNameKey, "") ?: ""

        val nameToShow = if (savedDisplayName.isNotBlank()) savedDisplayName else username

        tvProfileGreeting.text = "שלום, $nameToShow"
        tvUsername.text = "שם משתמש: $username"
        tvRole.text = "תפקיד: ${if (isAdmin) "admin" else "user"}"
        etDisplayName.setText(savedDisplayName)

        btnSaveProfile.setOnClickListener {
            val displayName = etDisplayName.text.toString().trim()

            prefs.edit()
                .putString(displayNameKey, displayName)
                .apply()

            val updatedName = if (displayName.isNotBlank()) displayName else username
            tvProfileGreeting.text = "שלום, $updatedName"

            Toast.makeText(this, "הפרופיל נשמר בהצלחה", Toast.LENGTH_SHORT).show()
        }

        btnLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
    }
}