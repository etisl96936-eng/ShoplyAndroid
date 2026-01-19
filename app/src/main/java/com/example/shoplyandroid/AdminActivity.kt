package com.example.shoplyandroid

import android.content.Intent
import android.os.Bundle
import android.view.View // ייבוא חשוב כדי שנוכל לשנות את ה-Visibility
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etImageUrl = findViewById<EditText>(R.id.etImageUrl)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<Button>(R.id.btnDelete) // חברי את כפתור המחיקה מה-XML

        // 1. הגדרת המתאם לספינר
        val categories = arrayOf("הכל", "פירות וירקות", "מוצרי חלב וביצים", "ניקיון", "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        // בדיקה אם קיבלנו מוצר לעריכה
        val itemToEdit = intent.getSerializableExtra("EDIT_ITEM") as? ShoppingItem

        if (itemToEdit != null) {
            etTitle.setText(itemToEdit.title)
            etDescription.setText(itemToEdit.description)
            etImageUrl.setText(itemToEdit.imageUrl)

            val position = spinnerAdapter.getPosition(itemToEdit.category)
            if (position >= 0) {
                spinnerCategory.setSelection(position)
            }

            btnSave.text = "עדכן מוצר"
            etTitle.isEnabled = false

            // --- כאן מוסיפים את הלוגיקה של המחיקה ---
            btnDelete.visibility = View.VISIBLE // מציג את הכפתור רק בעריכה
            btnDelete.setOnClickListener {
                val resultIntent = Intent()
                resultIntent.putExtra("DELETED_PRODUCT_TITLE", itemToEdit.title)
                resultIntent.putExtra("IS_DELETE", true)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            // ---------------------------------------
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val imageUrl = etImageUrl.text.toString().trim()

            if (title.isEmpty()) {
                etTitle.error = "חובה להזין שם"
                return@setOnClickListener
            }

            val newItem = ShoppingItem(
                title = title,
                description = etDescription.text.toString(),
                category = spinnerCategory.selectedItem.toString(),
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
}