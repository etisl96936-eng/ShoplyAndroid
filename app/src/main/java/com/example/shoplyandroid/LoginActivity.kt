package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        if (auth.currentUser != null) {
            loadUserDataAndGoToMain()
            return
        }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etUsername.error = "חובה להזין אימייל"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "חובה להזין סיסמה"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    loadUserDataAndGoToMain()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "שגיאה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadUserDataAndGoToMain() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val email = currentUser?.email ?: ""

        if (uid == null) {
            goToMain()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "user"
                val displayName = doc.getString("displayName") ?: ""

                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", role == "admin")
                prefs.putString("USERNAME", email)
                prefs.putString("DISPLAY_NAME", displayName)
                prefs.apply()

                goToMain()
            }
            .addOnFailureListener {
                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", false)
                prefs.putString("USERNAME", email)
                prefs.putString("DISPLAY_NAME", "")
                prefs.apply()

                goToMain()
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}