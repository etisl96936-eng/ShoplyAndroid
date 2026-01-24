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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ShoppingAdapter
    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnViewList: Button
    private var isShowingOnlyCart = false // משתנה למעקב: האם אנחנו מציגים רק את הסל?

    private val startAdminActivity = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isDelete = data?.getBooleanExtra("IS_DELETE", false) ?: false

            if (isDelete) {
                val titleToDelete = data?.getStringExtra("DELETED_PRODUCT_TITLE")
                catalogItems.removeAll { it.title == titleToDelete }
                userShoppingList.removeAll { it.title == titleToDelete }
            } else {
                val returnedItem = data?.getSerializableExtra("NEW_PRODUCT") as? ShoppingItem
                returnedItem?.let { item ->
                    val existingIndex = catalogItems.indexOfFirst { it.title == item.title }
                    if (existingIndex != -1) catalogItems[existingIndex] = item
                    else catalogItems.add(0, item)
                }
            }
            saveItemsToDisk()
            applyFilter()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        btnViewList = findViewById(R.id.btnViewList)
        val fabAddProduct = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddProduct)

        loadItemsFromDisk() // טוען גם את הקטלוג וגם את הסל שנשמר

        val categories = arrayOf("הכל", "פירות וירקות", "מוצרי חלב וביצים", "ניקיון", "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)

        recyclerView.layoutManager = LinearLayoutManager(this)
        applyFilter()
        updateViewListButton()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { applyFilter() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // פתרון נקודה 2: לחיצה על הכפתור מחליפה בין תצוגת "הכל" לתצוגת "סל קניות"
        btnViewList.setOnClickListener {
            isShowingOnlyCart = !isShowingOnlyCart
            if (isShowingOnlyCart) {
                btnViewList.setBackgroundColor(android.graphics.Color.GRAY)
            } else {
                btnViewList.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            }
            applyFilter()
            updateViewListButton()
        }

        fabAddProduct.setOnClickListener {
            startAdminActivity.launch(Intent(this, AdminActivity::class.java))
        }
    }

    private fun applyFilter() {
        val query = findViewById<EditText>(R.id.etSearch).text.toString().trim().lowercase()
        val selectedCat = findViewById<Spinner>(R.id.spinnerCategory).selectedItem?.toString() ?: "הכל"

        // פתרון נקודה 2: סינון לפי האם המשתמש ביקש לראות רק את הסל שלו
        val baseList = if (isShowingOnlyCart) userShoppingList else catalogItems

        val filteredList = baseList.filter { item ->
            val matchesQuery = item.title.lowercase().contains(query)
            val matchesCat = selectedCat == "הכל" || item.category == selectedCat
            matchesQuery && matchesCat
        }.toMutableList()

        updateAdapter(filteredList)
    }

    private fun updateAdapter(newList: MutableList<ShoppingItem>) {
        adapter = ShoppingAdapter(
            newList,
            userShoppingList.map { it.title },
            { item -> toggleProduct(item) },
            { url -> openVideo(url) },
            { item -> openEditProduct(item) },
            { item -> deleteItemFromCatalog(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun toggleProduct(item: ShoppingItem) {
        val index = userShoppingList.indexOfFirst { it.title == item.title }
        if (index != -1) userShoppingList.removeAt(index)
        else userShoppingList.add(item)

        saveItemsToDisk() // פתרון נקודה 1: שומר מיד כשיש שינוי בסל
        updateViewListButton()
        applyFilter()
    }

    private fun updateViewListButton() {
        // פתרון נקודה 3: טקסט דינמי
        if (userShoppingList.isEmpty()) {
            btnViewList.text = "הסל ריק - הוסף מוצרים לרשימה"
        } else if (isShowingOnlyCart) {
            btnViewList.text = "חזור לקטלוג המלא"
        } else {
            btnViewList.text = "צפה ברשימת הקניות שלי (${userShoppingList.size} מוצרים)"
        }
    }

    private fun deleteItemFromCatalog(item: ShoppingItem) {
        catalogItems.remove(item)
        userShoppingList.remove(item)
        saveItemsToDisk()
        applyFilter()
        updateViewListButton()
    }

    private fun openEditProduct(item: ShoppingItem) {
        val intent = Intent(this, AdminActivity::class.java)
        intent.putExtra("EDIT_ITEM", item)
        startAdminActivity.launch(intent)
    }

    private fun openVideo(url: String) {
        if (url.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        else Toast.makeText(this, "אין וידאו זמין", Toast.LENGTH_SHORT).show()
    }

    // פתרון נקודה 1: שמירה של שני המערכים
    private fun saveItemsToDisk() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        val gson = Gson()
        prefs.putString("saved_catalog", gson.toJson(catalogItems))
        prefs.putString("saved_user_list", gson.toJson(userShoppingList))
        prefs.apply()
    }

    // פתרון נקודה 1: טעינה של שני המערכים
    private fun loadItemsFromDisk() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<ShoppingItem>>() {}.type

        val catalogJson = prefs.getString("saved_catalog", null)
        if (catalogJson != null) catalogItems = gson.fromJson(catalogJson, type)
        else setupInitialCatalog()

        val userListJson = prefs.getString("saved_user_list", null)
        if (userListJson != null) userShoppingList = gson.fromJson(userListJson, type)
    }

    private fun setupInitialCatalog() {
        catalogItems = mutableListOf(
            ShoppingItem("עגבנייה", "קילו עגבניות שרי", "פירות וירקות", "https://upload.wikimedia.org/wikipedia/commons/8/89/Tomato_je.jpg", "https://www.youtube.com/watch?v=PTIxy8anmYM", 0),
            ShoppingItem("חלב 3%", "קרטון 1 ליטר", "מוצרי חלב וביצים", "https://p2.piqsels.com/preview/630/562/591/milk-bottle-glass-bottle-milk-bottle.jpg", "https://www.youtube.com/watch?v=FXTOqgai13w", 0)
        )
        saveItemsToDisk()
    }
}