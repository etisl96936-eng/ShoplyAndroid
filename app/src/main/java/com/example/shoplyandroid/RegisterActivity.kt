package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * מסך הרשמה למשתמשים חדשים.
 * מאפשר יצירת חשבון עם שם תצוגה, אימייל וסיסמה.
 * לאחר הרשמה מוצלחת, נוצר מסמך משתמש ב-Firestore עם תפקיד "user".
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<EditText>(R.id.etRegisterName)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "חובה להזין שם"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "חובה להזין אימייל"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "הסיסמה חייבת להכיל לפחות 6 תווים"
                return@setOnClickListener
            }

            registerUser(name, email, password)
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * יוצר חשבון חדש ב-Firebase Auth ושומר את פרטי המשתמש ב-Firestore.
     * לאחר הצלחה, שומר את פרטי המשתמש ב-SharedPreferences ומנווט למסך הראשי.
     *
     * @param name שם התצוגה של המשתמש
     * @param email האימייל של המשתמש
     * @param password הסיסמה שנבחרה
     */
    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                db.collection("users").document(uid).set(
                    hashMapOf(
                        "email" to email,
                        "displayName" to name,
                        "role" to "user"
                    )
                ).addOnSuccessListener {
                    val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                    prefs.putBoolean("IS_ADMIN", false)
                    prefs.putString("USERNAME", email)
                    prefs.putString("DISPLAY_NAME", name)
                    prefs.apply()

                    Toast.makeText(this, "ההרשמה הצליחה! ברוך הבא 🎉", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "שגיאה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}