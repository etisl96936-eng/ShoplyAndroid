package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * מסך פרופיל המשתמש.
 * מציג את פרטי המשתמש המחובר ומאפשר עדכון שם תצוגה.
 * השינוי נשמר גם ב-SharedPreferences וגם ב-Firestore לסנכרון בין מכשירים.
 */
class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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

        val savedDisplayName = prefs.getString("DISPLAY_NAME", "") ?: ""
        val nameToShow = if (savedDisplayName.isNotBlank()) savedDisplayName else username

        tvProfileGreeting.text = "שלום, $nameToShow"
        tvUsername.text = "שם משתמש: $username"
        tvRole.text = "תפקיד: ${if (isAdmin) "admin" else "user"}"
        etDisplayName.setText(savedDisplayName)

        btnSaveProfile.setOnClickListener {
            saveDisplayName(etDisplayName, tvProfileGreeting, username, prefs)
        }

        btnLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
    }

    /**
     * שומר את שם התצוגה החדש ב-SharedPreferences וב-Firestore.
     * אם המשתמש לא מחובר — שומר רק לוקאלית.
     *
     * @param etDisplayName שדה הקלט של שם התצוגה
     * @param tvProfileGreeting טקסט הברכה לעדכון
     * @param username שם המשתמש הבסיסי (אימייל) כגיבוי
     * @param prefs SharedPreferences לשמירה מקומית
     */
    private fun saveDisplayName(
        etDisplayName: EditText,
        tvProfileGreeting: TextView,
        username: String,
        prefs: android.content.SharedPreferences
    ) {
        val displayName = etDisplayName.text.toString().trim()
        val updatedName = if (displayName.isNotBlank()) displayName else username

        prefs.edit()
            .putString("DISPLAY_NAME", displayName)
            .apply()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            tvProfileGreeting.text = "שלום, $updatedName"
            Toast.makeText(this, "הפרופיל נשמר בהצלחה", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid)
            .update("displayName", displayName)
            .addOnSuccessListener {
                tvProfileGreeting.text = "שלום, $updatedName"
                Toast.makeText(this, "הפרופיל נשמר בהצלחה", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                tvProfileGreeting.text = "שלום, $updatedName"
                Toast.makeText(this, "השם נשמר מקומית, אך לא עודכן בענן", Toast.LENGTH_SHORT).show()
            }
    }
}