package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * מסך ההתחברות של האפליקציה.
 * תומך בהתחברות עם אימייל וסיסמה, וכן בהתחברות עם חשבון Google.
 * אם המשתמש כבר מחובר, מנתב ישירות למסך הראשי.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button
    private lateinit var btnGoogleSignIn: SignInButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // אם המשתמש כבר מחובר — טען נתונים ועבור ישירות למסך הראשי
        if (auth.currentUser != null) {
            loadUserDataAndGoToMain()
            return
        }

        btnLogin.setOnClickListener {
            loginWithEmailPassword()
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * מבצע התחברות עם אימייל וסיסמה.
     * מבצע ולידציה על השדות לפני השליחה ל-Firebase Auth.
     */
    private fun loginWithEmailPassword() {
        val email = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etUsername.error = "חובה להזין אימייל"
            etUsername.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.error = "אימייל לא תקין"
            etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "חובה להזין סיסמה"
            etPassword.requestFocus()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                loadUserDataAndGoToMain()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "התחברות נכשלה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * מפעיל את תהליך ההתחברות עם חשבון Google באמצעות Credential Manager.
     * רץ ב-Coroutine על ה-Main dispatcher.
     */
    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleGoogleCredential(result.credential)
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "התחברות עם Google נכשלה: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * מטפל ב-Credential שהתקבל מ-Google ומעביר את ה-ID Token ל-Firebase.
     *
     * @param credential האישור שהתקבל מ-Credential Manager
     */
    private fun handleGoogleCredential(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this, "שגיאה בקריאת פרטי Google", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "סוג ההתחברות שהתקבל אינו נתמך", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * מבצע אימות מול Firebase Auth באמצעות Google ID Token.
     *
     * @param idToken ה-Token שהתקבל מ-Google
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    Toast.makeText(this, "לא התקבל משתמש מ-Google", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                createOrUpdateGoogleUser(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: ""
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "התחברות Firebase עם Google נכשלה: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * יוצר מסמך משתמש חדש ב-Firestore אם לא קיים, או מעדכן פרטים קיימים.
     * משתמש חדש מקבל תפקיד "user" כברירת מחדל.
     *
     * @param uid מזהה המשתמש ב-Firebase
     * @param email האימייל של המשתמש
     * @param displayName שם התצוגה של המשתמש
     */
    private fun createOrUpdateGoogleUser(uid: String, email: String, displayName: String) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // משתמש קיים — עדכון פרטים בלבד
                    userRef.update(mapOf("email" to email, "displayName" to displayName))
                        .addOnCompleteListener { loadUserDataAndGoToMain() }
                } else {
                    // משתמש חדש — יצירת מסמך עם תפקיד ברירת מחדל
                    val userData = hashMapOf(
                        "email" to email,
                        "displayName" to displayName,
                        "role" to "user"
                    )
                    userRef.set(userData)
                        .addOnSuccessListener { loadUserDataAndGoToMain() }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "שמירת משתמש נכשלה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "קריאת נתוני משתמש נכשלה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * טוען את נתוני המשתמש המחובר מ-Firestore (תפקיד ושם תצוגה)
     * ושומר אותם ב-SharedPreferences לפני המעבר למסך הראשי.
     */
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
                // במקרה של כשל — ברירת מחדל: משתמש רגיל
                val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                prefs.putBoolean("IS_ADMIN", false)
                prefs.putString("USERNAME", email)
                prefs.putString("DISPLAY_NAME", "")
                prefs.apply()

                goToMain()
            }
    }

    /**
     * מנווט למסך הראשי וסוגר את מסך ההתחברות.
     */
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}