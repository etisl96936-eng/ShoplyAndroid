package com.example.shoplyandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ShoppingAdapter
    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupCatalog()

        recyclerView = findViewById(R.id.recyclerView)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnViewList = findViewById<Button>(R.id.btnViewList)

        // 1. הגדרת הספינר עם כל הקטגוריות
        val categories = arrayOf("הכל", "פירות וירקות", "מוצרי חלב", "ניקיון", "מאפה ודגנים", "שימורים ומזווה")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        // 2. הגדרת ה-RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateAdapter(userShoppingList)

        // 3. האזנה לחיפוש טקסטואלי
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 4. האזנה לבחירת קטגוריה
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 5. כפתור צפייה ברשימה המתוכננת (איפוס מסננים)
        btnViewList.setOnClickListener {
            etSearch.setText("")
            spinnerCategory.setSelection(0)
            applyFilter()
            Toast.makeText(this, "מציג את רשימת התכנון שלך", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFilter() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        val query = etSearch.text.toString().trim().lowercase()
        val selectedCat = spinnerCategory.selectedItem?.toString() ?: "הכל"

        if (query.isEmpty() && selectedCat == "הכל") {
            // מציג רק את מה שהמשתמש בחר לתכנון
            updateAdapter(userShoppingList)
        } else {
            // מציג הצעות מהקטלוג לפי סינון
            val suggestions = catalogItems.filter { item ->
                val matchesQuery = item.title.lowercase().contains(query)
                val matchesCat = selectedCat == "הכל" || item.category == selectedCat
                matchesQuery && matchesCat
            }.toMutableList()
            updateAdapter(suggestions)
        }
    }

    private fun toggleProduct(item: ShoppingItem) {
        val existingItem = userShoppingList.find { it.title == item.title }
        val etSearch = findViewById<EditText>(R.id.etSearch)

        if (existingItem != null) {
            // הסרה מהרשימה
            userShoppingList.remove(existingItem)
            Toast.makeText(this, "${item.title} הוסר מהרשימה", Toast.LENGTH_SHORT).show()
        } else {
            // הוספה לרשימה
            userShoppingList.add(item)
            Toast.makeText(this, "${item.title} נוסף לרשימה", Toast.LENGTH_SHORT).show()

            // ניקוי טקסט החיפוש (אבל נשארים באותה קטגוריה)
            if (etSearch.text.isNotEmpty()) {
                etSearch.setText("")
            }
        }

        // עדכון כמות המוצרים על הכפתור העליון
        val btnViewList = findViewById<Button>(R.id.btnViewList)
        btnViewList.text = "צפה ברשימת הקניות שלי (${userShoppingList.size} מוצרים)"

        applyFilter() // ריענון התצוגה (מעדכן צבעים/סימונים)
    }

    private fun openVideo(url: String) {
        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } else {
            Toast.makeText(this, "אין וידאו זמין למוצר זה", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAdapter(newList: MutableList<ShoppingItem>) {
        adapter = ShoppingAdapter(
            newList,
            userShoppingList.map { it.title }, // רשימת שמות לבדיקת V
            { item -> toggleProduct(item) },  // פונקציית הוספה/הסרה
            { url -> openVideo(url) }          // פונקציית וידאו
        )
        recyclerView.adapter = adapter
    }

    private fun setupCatalog() {
        catalogItems = mutableListOf(
            // פירות וירקות
            ShoppingItem("עגבנייה", "קילו עגבניות שרי", "פירות וירקות", "", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("מלפפון", "מארז מלפפונים", "פירות וירקות", "", ""),
            ShoppingItem("תפוח עץ", "פינק ליידי", "פירות וירקות", "", ""),
            ShoppingItem("פלפל אדום", "גמבה טרי", "פירות וירקות", "", ""),
            ShoppingItem("בננה", "מארז בננות", "פירות וירקות", "", ""),
            ShoppingItem("בצל יבש", "רשת בצל", "פירות וירקות", "", ""),

            // מוצרי חלב
            ShoppingItem("חלב 3%", "קרטון 1 ליטר", "מוצרי חלב", "https://www.rami-levy.co.il/_ipx/w_366,f_webp/https://img.rami-levy.co.il/product/7290114313865/small.jpg", ""),
            ShoppingItem("גבינה צהובה", "עמק 200 גרם", "מוצרי חלב", "", ""),
            ShoppingItem("יוגורט", "יוגורט יווני", "מוצרי חלב", "", ""),
            ShoppingItem("גבינה לבנה", "250 גרם", "מוצרי חלב", "", ""),
            ShoppingItem("קוטג'", "5% תנובה", "מוצרי חלב", "", ""),
            ShoppingItem("חמאה", "200 גרם", "מוצרי חלב", "", ""),

            // ניקיון
            ShoppingItem("אקונומיקה", "נוזל ניקוי", "ניקיון", "", ""),
            ShoppingItem("נוזל כלים", "750 מ\"ל", "ניקיון", "", ""),
            ShoppingItem("נייר טואלט", "30 גלילים", "ניקיון", "", ""),

            // מאפה ודגנים
            ShoppingItem("לחם פרוס", "לחם אחיד פרוס", "מאפה ודגנים", "", ""),
            ShoppingItem("פיתות", "מארז 10 פיתות", "מאפה ודגנים", "", ""),
            ShoppingItem("קורנפלקס", "דגני בוקר", "מאפה ודגנים", "", ""),

            // שימורים ומזווה
            ShoppingItem("טונה", "מארז 4 קופסאות", "שימורים ומזווה", "", ""),
            ShoppingItem("אורז פרסי", "1 קילו", "שימורים ומזווה", "", ""),
            ShoppingItem("פסטה פנה", "500 גרם", "שימורים ומזווה", "", "")
        )
    }
}