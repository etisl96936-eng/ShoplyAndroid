package com.example.shoplyandroid

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * מסך סטטיסטיקות.
 * מציג סיכום של הקטלוג ורשימת הקניות, כולל גרף עמודות
 * המראה את כמות המוצרים לפי קטגוריה.
 * הנתונים נטענים מ-SharedPreferences ומעובדים בצד הלקוח (Group By).
 */
class StatisticsActivity : AppCompatActivity() {

    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()

    private val allCategories = listOf(
        "פירות וירקות", "מוצרי חלב וביצים", "ניקיון",
        "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val tvTotalCatalog = findViewById<TextView>(R.id.tvTotalCatalog)
        val tvTotalCart = findViewById<TextView>(R.id.tvTotalCart)
        val tvCategoryStats = findViewById<TextView>(R.id.tvCategoryStats)
        val barChart = findViewById<BarChart>(R.id.barChart)

        loadItemsFromDisk()

        tvTotalCatalog.text = "סה״כ מוצרים בקטלוג: ${catalogItems.size}"
        tvTotalCart.text = "סה״כ מוצרים ברשימת הקניות: ${userShoppingList.size}"

        // אגרגציה בצד הלקוח — ספירת מוצרים לפי קטגוריה
        val actualCounts = catalogItems.groupingBy { it.category }.eachCount()

        // שמירה על סדר הקטגוריות הקבוע גם אם קטגוריה מסוימת ריקה
        val categoryCounts = linkedMapOf<String, Int>()
        for (category in allCategories) {
            categoryCounts[category] = actualCounts[category] ?: 0
        }

        val statsText = buildString {
            append("כמות מוצרים לפי קטגוריה:\n\n")
            categoryCounts.forEach { (category, count) ->
                append("$category: $count\n")
            }
        }

        tvCategoryStats.text = statsText
        setupBarChart(barChart, categoryCounts)
    }

    /**
     * מגדיר ומציג גרף עמודות עם כמות המוצרים לפי קטגוריה.
     * משתמש בספריית MPAndroidChart.
     *
     * @param barChart רכיב הגרף להצגה
     * @param categoryCounts מפה של קטגוריה לכמות המוצרים בה
     */
    private fun setupBarChart(barChart: BarChart, categoryCounts: Map<String, Int>) {
        val categories = categoryCounts.keys.toList()
        val entries = categoryCounts.values.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "מוצרים לפי קטגוריה")
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setExtraBottomOffset(20f)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(categories)
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 10f
        xAxis.setDrawGridLines(false)
        xAxis.labelCount = categories.size

        barChart.invalidate()
    }

    /**
     * טוענת את נתוני הקטלוג ורשימת הקניות מ-SharedPreferences.
     */
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