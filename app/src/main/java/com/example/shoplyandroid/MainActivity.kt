package com.example.shoplyandroid

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoplyandroid.R

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ShoppingAdapter
    private lateinit var allItems: List<ShoppingItem>
    private var filteredItems: MutableList<ShoppingItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupItems()

        // מציאת הרכיבים מה-XML וחיבורם למשתנים
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        // הגדרת ה-RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ShoppingAdapter(filteredItems)
        recyclerView.adapter = adapter

        // הגדרת הסינונים
        setupCategorySpinner(spinnerCategory)
        setupSearch(etSearch)
    }

    private fun setupItems() {
        allItems = listOf(
            ShoppingItem("עגבנייה", "קילו עגבניות שרי טריות", "פירות וירקות", "https://img.freepik.com/free-photo/fresh-tomato_144627-15422.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("מלפפון", "מארז מלפפונים מהערבה", "פירות וירקות", "https://img.freepik.com/free-photo/green-cucumber_144627-15423.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("חלב 3%", "קרטון חלב תנובה 1 ליטר", "מוצרי חלב", "https://img.freepik.com/free-photo/milk-bottle_144627-15424.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("גבינה צהובה", "עמק 28% שומן, 200 גרם", "מוצרי חלב", "https://img.freepik.com/free-photo/cheese_144627-15425.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("יוגורט", "יוגורט יווני טבעי", "מוצרי חלב", "https://img.freepik.com/free-photo/yogurt_144627-15426.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("אקונומיקה", "נוזל ניקוי בניחוח לימון", "ניקיון", "https://img.freepik.com/free-photo/cleaning-bottle_144627-15427.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("שקיות אשפה", "שקיות חזקות עם שרוך", "ניקיון", "https://img.freepik.com/free-photo/trash-bag_144627-15428.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("תפוח עץ", "תפוח פינק ליידי", "פירות וירקות", "https://img.freepik.com/free-photo/red-apple_144627-15429.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("גבינה לבנה", "5% שומן, גביע 250 גרם", "מוצרי חלב", "https://img.freepik.com/free-photo/white-cheese_144627-15430.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            ShoppingItem("נוזל כלים", "בניחוח תפוח, 750 מ\"ל", "ניקיון", "https://img.freepik.com/free-photo/dish-soap_144627-15431.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        filteredItems.clear()
        filteredItems.addAll(allItems)
    }

    private fun setupCategorySpinner(spinner: Spinner) {
        val categories = arrayOf("הכל", "פירות וירקות", "מוצרי חלב", "ניקיון")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapterSpinner

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearch(etSearch: EditText) {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterList() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterList() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        val searchText = etSearch.text.toString().lowercase()
        val selectedCategory = spinnerCategory.selectedItem.toString()

        val filtered = allItems.filter { item ->
            val matchesSearch = item.title.lowercase().contains(searchText)
            val matchesCategory = selectedCategory == "הכל" || item.category == selectedCategory
            matchesSearch && matchesCategory
        }

        adapter.updateList(filtered)
    }
}