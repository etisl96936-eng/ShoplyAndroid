package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * מסך ניהול מוצרים — זמין לאדמין בלבד.
 * מאפשר הוספת מוצר חדש לקטלוג, עריכת מוצר קיים, או מחיקתו.
 * מחזיר תוצאה ל-MainActivity באמצעות setResult.
 */
class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        // בדיקת הרשאות — רק אדמין יכול לגשת למסך זה
        if (!isAdmin) {
            Toast.makeText(this, "אין לך הרשאת אדמין", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_admin)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etImageUrl = findViewById<EditText>(R.id.etImageUrl)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        val categories = arrayOf(
            "פירות וירקות", "מוצרי חלב וביצים", "ניקיון",
            "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים"
        )

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        // אם הועבר מוצר לעריכה — מאכלסים את השדות בפרטיו
        val itemToEdit = intent.getSerializableExtra("EDIT_ITEM") as? ShoppingItem

        if (itemToEdit != null) {
            etTitle.setText(itemToEdit.title)
            etDescription.setText(itemToEdit.description)
            etImageUrl.setText(itemToEdit.imageUrl)

            val position = spinnerAdapter.getPosition(itemToEdit.category)
            if (position >= 0) spinnerCategory.setSelection(position)

            btnSave.text = "עדכן מוצר"
            etTitle.isEnabled = false

            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                val resultIntent = Intent()
                resultIntent.putExtra("DELETED_PRODUCT_TITLE", itemToEdit.title)
                resultIntent.putExtra("IS_DELETE", true)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } else {
            btnDelete.visibility = View.GONE
            btnSave.text = "הוסף מוצר"
        }

        btnSave.setOnClickListener {
            saveProduct(etTitle, etDescription, etImageUrl, spinnerCategory, itemToEdit)
        }
    }

    /**
     * מאמת את הקלט ושולח את פרטי המוצר החדש/המעודכן חזרה ל-MainActivity.
     *
     * @param etTitle שדה שם המוצר
     * @param etDescription שדה תיאור המוצר
     * @param etImageUrl שדה קישור לתמונה
     * @param spinnerCategory ספינר לבחירת קטגוריה
     * @param itemToEdit המוצר המקורי במקרה של עריכה, או null במקרה של הוספה
     */
    private fun saveProduct(
        etTitle: EditText,
        etDescription: EditText,
        etImageUrl: EditText,
        spinnerCategory: Spinner,
        itemToEdit: ShoppingItem?
    ) {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val imageUrl = etImageUrl.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()

        if (title.isEmpty()) {
            etTitle.error = "חובה להזין שם מוצר"
            etTitle.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "חובה להזין תיאור"
            etDescription.requestFocus()
            return
        }

        if (imageUrl.isEmpty()) {
            etImageUrl.error = "חובה להזין קישור לתמונה"
            etImageUrl.requestFocus()
            return
        }

        val newItem = ShoppingItem(
            title = title,
            description = description,
            category = category,
            imageUrl = imageUrl,
            videoUrl = itemToEdit?.videoUrl ?: "",
            imageRes = 0
        )

        val resultIntent = Intent()
        resultIntent.putExtra("NEW_PRODUCT", newItem)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}