package com.example.shoplyandroid

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StatisticsActivity : AppCompatActivity() {

    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val tvTotalCatalog = findViewById<TextView>(R.id.tvTotalCatalog)
        val tvTotalCart = findViewById<TextView>(R.id.tvTotalCart)
        val tvCategoryStats = findViewById<TextView>(R.id.tvCategoryStats)

        loadItemsFromDisk()

        tvTotalCatalog.text = "סה״כ מוצרים בקטלוג: ${catalogItems.size}"
        tvTotalCart.text = "סה״כ מוצרים ברשימת הקניות: ${userShoppingList.size}"

        val categoryCounts = catalogItems.groupingBy { it.category }.eachCount()

        val statsText = if (categoryCounts.isEmpty()) {
            "אין נתונים להצגה"
        } else {
            buildString {
                append("כמות מוצרים לפי קטגוריה:\n\n")
                categoryCounts.forEach { (category, count) ->
                    append("$category: $count\n")
                }
            }
        }

        tvCategoryStats.text = statsText
    }

    private fun loadItemsFromDisk() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<ShoppingItem>>() {}.type

        val catalogJson = prefs.getString("saved_catalog", null)
        if (catalogJson != null) {
            catalogItems = gson.fromJson(catalogJson, type)
        }

        val userListJson = prefs.getString("saved_user_list", null)
        if (userListJson != null) {
            userShoppingList = gson.fromJson(userListJson, type)
        }
    }
}