package com.example.shoplyandroid
import com.example.shoplyandroid.R
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ShoppingAdapter
    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()
    private lateinit var recyclerView: RecyclerView

    // לאונצ'ר לקבלת נתונים מה-AdminActivity
    private val startAdminActivity = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data

            // 1. בדיקה האם המשתמש לחץ על "מחק"
            val isDelete = data?.getBooleanExtra("IS_DELETE", false) ?: false

            if (isDelete) {
                val titleToDelete = data?.getStringExtra("DELETED_PRODUCT_TITLE")
                catalogItems.removeAll { it.title == titleToDelete }
                userShoppingList.removeAll { it.title == titleToDelete }
                Toast.makeText(this, "המוצר נמחק מהמערכת", Toast.LENGTH_SHORT).show()
            } else {
                // 2. לוגיקה של הוספה או עדכון
                val returnedItem = data?.getSerializableExtra("NEW_PRODUCT") as? ShoppingItem
                returnedItem?.let { item ->
                    val existingIndex = catalogItems.indexOfFirst { it.title == item.title }

                    if (existingIndex != -1) {
                        catalogItems[existingIndex] = item
                        Toast.makeText(this, "המוצר '${item.title}' עודכן בהצלחה!", Toast.LENGTH_SHORT).show()
                    } else {
                        catalogItems.add(0, item)
                        Toast.makeText(this, "מוצר חדש נוסף בהצלחה!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            saveItemsToDisk()
            applyFilter()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // כפתור הפלוס למעבר למסך ניהול
        val fabAddProduct = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddProduct)
        fabAddProduct.setOnClickListener {
            val intent = Intent(this, AdminActivity::class.java)
            startAdminActivity.launch(intent)
        }

        // טעינת נתונים
        loadItemsFromDisk()

        recyclerView = findViewById(R.id.recyclerView)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnViewList = findViewById<Button>(R.id.btnViewList)

        // הגדרת הספינר
        val categories = arrayOf("הכל", "פירות וירקות", "מוצרי חלב וביצים", "ניקיון", "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        recyclerView.layoutManager = LinearLayoutManager(this)
        updateAdapter(catalogItems)

        // מאזיני חיפוש וסינון
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnViewList.setOnClickListener {
            etSearch.setText("")
            spinnerCategory.setSelection(0)
            updateAdapter(userShoppingList)
            Toast.makeText(this, "מציג את רשימת התכנון שלך", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveItemsToDisk() {
        val sharedPreferences = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(catalogItems)
        editor.putString("saved_catalog", json)
        editor.apply()
    }

    private fun loadItemsFromDisk() {
        val sharedPreferences = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("saved_catalog", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ShoppingItem>>() {}.type
            catalogItems = Gson().fromJson(json, type)
        } else {
            setupInitialCatalog()
        }
    }

    private fun applyFilter() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        val query = etSearch.text.toString().trim().lowercase()
        val selectedCat = spinnerCategory.selectedItem?.toString() ?: "הכל"

        val filteredList = catalogItems.filter { item ->
            val matchesQuery = item.title.lowercase().contains(query)
            val matchesCat = selectedCat == "הכל" || item.category == selectedCat
            matchesQuery && matchesCat
        }.toMutableList()

        updateAdapter(filteredList)
    }

    private fun toggleProduct(item: ShoppingItem) {
        val index = userShoppingList.indexOfFirst { it.title == item.title }
        if (index != -1) {
            userShoppingList.removeAt(index)
            Toast.makeText(this, "${item.title} הוסר מהרשימה", Toast.LENGTH_SHORT).show()
        } else {
            userShoppingList.add(item)
            Toast.makeText(this, "${item.title} נוסף לרשימה", Toast.LENGTH_SHORT).show()
        }

        val btnViewList = findViewById<Button>(R.id.btnViewList)
        btnViewList.text = "צפה ברשימת הקניות שלי (${userShoppingList.size} מוצרים)"
        adapter.notifyDataSetChanged()
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
            { item -> toggleProduct(item) },
            { url -> openVideo(url) },
            { item -> openEditProduct(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun openEditProduct(item: ShoppingItem) {
        val intent = Intent(this, AdminActivity::class.java)
        intent.putExtra("EDIT_ITEM", item)
        startAdminActivity.launch(intent)
    }

    private fun setupInitialCatalog() {
        catalogItems = mutableListOf(
            ShoppingItem("עגבנייה", "קילו עגבניות שרי", "פירות וירקות", "https://upload.wikimedia.org/wikipedia/commons/8/89/Tomato_je.jpg", "https://www.youtube.com/watch?v=PTIxy8anmYM", 0),
            ShoppingItem("חלב 3%", "קרטון 1 ליטר", "מוצרי חלב וביצים", "https://p2.piqsels.com/preview/630/562/591/milk-bottle-glass-bottle-milk-bottle.jpg", "https://www.youtube.com/watch?v=FXTOqgai13w", 0),
            ShoppingItem("לחם פרוס", "חיטה מלאה", "מאפה ודגנים", "https://cdn.pixabay.com/photo/2014/07/22/09/59/bread-399286_1280.jpg", "", 0)
        )
        saveItemsToDisk()
    }
}