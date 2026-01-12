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
import com.example.shoplyandroid.ShoppingItem
import com.example.shoplyandroid.ShoppingAdapter
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

            // ניקוי טקסט החיפוש
            if (etSearch.text.isNotEmpty()) {
                etSearch.setText("")
            }
        }

        updateListButtonCounter()
        applyFilter()
    }

    // פונקציה חדשה לטיפול במחיקה מהקטלוג (בונוס)
    private fun deleteProductFromSystem(position: Int, currentList: MutableList<ShoppingItem>) {
        val itemToDelete = currentList[position]

        // 1. הסרה מהרשימה שמוצגת כרגע
        currentList.removeAt(position)

        // 2. הסרה מהקטלוג הראשי (כדי שלא יחזור בחיפוש)
        catalogItems.removeAll { it.title == itemToDelete.title }

        // 3. הסרה מרשימת הקניות של המשתמש אם הוא נמצא שם
        userShoppingList.removeAll { it.title == itemToDelete.title }

        // 4. עדכון התצוגה
        adapter.notifyItemRemoved(position)
        updateListButtonCounter()

        Toast.makeText(this, "${itemToDelete.title} נמחק מהמערכת", Toast.LENGTH_SHORT).show()
    }

    private fun updateListButtonCounter() {
        val btnViewList = findViewById<Button>(R.id.btnViewList)
        btnViewList.text = "צפה ברשימת הקניות שלי (${userShoppingList.size} מוצרים)"
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
            userShoppingList.map { it.title },
            { item: ShoppingItem -> toggleProduct(item) }, // הוספת : ShoppingItem
            { url: String -> openVideo(url) },             // הוספת : String
            { position: Int -> deleteProductFromSystem(position, newList) } // הוספת : Int
        )
        recyclerView.adapter = adapter
    }

    private fun setupCatalog() {
        // כאן השארתי את הרשימה שלך כפי שהיא, רק הוספתי את האפשרות ל-imageUrl בעתיד
        catalogItems = mutableListOf(
            ShoppingItem("עגבנייה", "קילו עגבניות שרי", "פירות וירקות", R.drawable.tomato, null, "https://www.youtube.com/watch?v=PTIxy8anmYM"),
            ShoppingItem("מלפפון", "מארז מלפפונים", "פירות וירקות", R.drawable.cucumber, null, "https://www.youtube.com/watch?v=dvWiypj3W7s"),
            ShoppingItem("תפוח עץ", "פינק ליידי", "פירות וירקות", R.drawable.apple, null, "https://www.youtube.com/watch?v=eBzTCbGnlWo"),
            ShoppingItem("פלפל אדום", "גמבה טרי", "פירות וירקות", R.drawable.pepper, null, "https://www.youtube.com/shorts/jSCAoZ0bKHk"),
            ShoppingItem("בננה", "מארז בננות", "פירות וירקות", R.drawable.banana, null, "https://www.youtube.com/shorts/QB9nqTjo0-M"),
            ShoppingItem("בצל יבש", "רשת בצל", "פירות וירקות", R.drawable.onion, null, "https://www.youtube.com/shorts/XYwJYA4ggRc"),
            ShoppingItem("חלב 3%", "קרטון 1 ליטר", "מוצרי חלב", R.drawable.milk, null, "https://www.youtube.com/watch?v=FXTOqgai13w"),
            ShoppingItem("גבינה צהובה", "עמק 200 גרם", "מוצרי חלב", R.drawable.yelloecheese, null, "https://www.youtube.com/watch?v=GJ2Q4oHLbgo"),
            ShoppingItem("יופלה", "יופלה שטוזים", "מוצרי חלב", R.drawable.yopaledelicacy, null, "https://www.youtube.com/watch?v=Pecoy9kXOQ0"),
            ShoppingItem("גבינה לבנה", "250 גרם", "מוצרי חלב", R.drawable.curds, null, "https://www.youtube.com/watch?v=CA2-wEtZhKw"),
            ShoppingItem("קוטג'", "5% תנובה", "מוצרי חלב", R.drawable.cottage, null, "https://www.youtube.com/watch?v=BffrD1dAIek"),
            ShoppingItem("חמאה", "200 גרם", "מוצרי חלב", R.drawable.butter, null, "https://www.youtube.com/watch?v=sO1A_eW4Bdo"),
            ShoppingItem("אקונומיקה", "נוזל ניקוי", "ניקיון", R.drawable.bleach, null, "https://www.youtube.com/watch?v=w_WER-bRsMM"),
            ShoppingItem("נוזל כלים", "750 מ\"ל", "ניקיון", R.drawable.dishwashingliquid, null, "https://www.youtube.com/shorts/FFw_NPmyOk4"),
            ShoppingItem("נייר טואלט", "30 גלילים", "ניקיון", R.drawable.toiletpaper, null, "https://www.youtube.com/watch?v=gv6KBSIOaAc"),
            ShoppingItem("לחם מחמצת", "לחם מחמצת אנג'ל", "מאפה ודגנים", R.drawable.bread, null, "https://www.youtube.com/watch?v=MHh2I2o4bQQ"),
            ShoppingItem("לחמניות", "מארז 6 לחמניות", "מאפה ודגנים", R.drawable.bun, null, "https://www.youtube.com/watch?v=uOjAmj2j8fo"),
            ShoppingItem("קורנפלקס", "דגני בוקר", "מאפה ודגנים", R.drawable.korenflakes, null, "https://www.youtube.com/watch?v=4HfkAG56lr0"),
            ShoppingItem("טונה", "מארז 4 קופסאות", "שימורים ומזווה", R.drawable.tuna, null, "https://www.youtube.com/watch?v=O7QY9oH4S5c"),
            ShoppingItem("אורז פרסי", "1 קילו", "שימורים ומזווה", R.drawable.rice, null, "https://www.youtube.com/watch?v=GA9ljFbE004"),
            ShoppingItem("פסטה", "500 גרם", "שימורים ומזווה", R.drawable.pasta, null, "https://www.youtube.com/watch?v=1G0sivf2LU8")
        )
    }
}