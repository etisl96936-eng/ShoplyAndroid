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
    private var isShowingOnlyCart = false

    private var visibleItemCount = 5
    private val pageSize = 5

    private val startAdminActivity = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isDelete = data?.getBooleanExtra("IS_DELETE", false) ?: false

            if (isDelete) {
                val titleToDelete = data?.getStringExtra("DELETED_PRODUCT_TITLE")
                catalogItems.removeAll { it.title == titleToDelete }
                userShoppingList.removeAll { it.title == titleToDelete }

                Toast.makeText(
                    this@MainActivity,
                    "המוצר נמחק מהקטלוג",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val returnedItem = data?.getSerializableExtra("NEW_PRODUCT") as? ShoppingItem
                returnedItem?.let { item ->
                    val existingIndex = catalogItems.indexOfFirst { it.title == item.title }

                    if (existingIndex != -1) {
                        catalogItems[existingIndex] = item
                        Toast.makeText(
                            this@MainActivity,
                            "המוצר עודכן בקטלוג",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        catalogItems.add(0, item)
                        Toast.makeText(
                            this@MainActivity,
                            "המוצר נוסף לקטלוג",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            saveItemsToDisk()
            applyFilter()
            updateViewListButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        btnViewList = findViewById(R.id.btnViewList)

        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val fabAddProduct =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddProduct)
        val tvAdminHint = findViewById<TextView>(R.id.tvAdminHint)
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)

        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        if (isAdmin) {
            fabAddProduct.visibility = View.VISIBLE
            tvAdminHint.visibility = View.VISIBLE
        } else {
            fabAddProduct.visibility = View.GONE
            tvAdminHint.visibility = View.GONE
        }

        loadItemsFromDisk()

        val categories = arrayOf(
            "הכל",
            "פירות וירקות",
            "מוצרי חלב וביצים",
            "ניקיון",
            "מאפה ודגנים",
            "שימורים ומזווה",
            "בשר ודגים"
        )

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        recyclerView.layoutManager = LinearLayoutManager(this)
        applyFilter()
        updateViewListButton()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                visibleItemCount = pageSize
                applyFilter()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                visibleItemCount = pageSize
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnViewList.setOnClickListener {
            isShowingOnlyCart = !isShowingOnlyCart
            visibleItemCount = pageSize

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

        btnLoadMore.setOnClickListener {
            val previousCount = visibleItemCount
            visibleItemCount += pageSize
            applyFilter()
            recyclerView.post {
                recyclerView.scrollToPosition(previousCount - 1)
            }
        }
    }

    private fun applyFilter() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)

        val query = etSearch.text.toString().trim().lowercase()
        val selectedCat = spinnerCategory.selectedItem?.toString() ?: "הכל"

        val baseList = if (isShowingOnlyCart) userShoppingList else catalogItems

        val filteredList = baseList.filter { item ->
            val matchesQuery = item.title.lowercase().contains(query)
            val matchesCat = selectedCat == "הכל" || item.category == selectedCat
            matchesQuery && matchesCat
        }.toMutableList()

        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            btnLoadMore.visibility = View.GONE

            tvEmptyState.text = if (isShowingOnlyCart) {
                "רשימת הקניות שלך ריקה כרגע"
            } else {
                "לא נמצאו מוצרים"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE

            val visibleList = filteredList.take(visibleItemCount).toMutableList()
            updateAdapter(visibleList)

            btnLoadMore.visibility =
                if (filteredList.size > visibleItemCount) View.VISIBLE else View.GONE
        }
    }

    private fun updateAdapter(newList: MutableList<ShoppingItem>) {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        adapter = ShoppingAdapter(
            newList,
            userShoppingList.map { it.title },
            isAdmin,
            { item -> toggleProduct(item) },
            { url -> openVideo(url) },
            { item -> openEditProduct(item) },
            { item -> deleteItemFromCatalog(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun toggleProduct(item: ShoppingItem) {
        val index = userShoppingList.indexOfFirst { it.title == item.title }

        if (index != -1) {
            userShoppingList.removeAt(index)
            Toast.makeText(this, "המוצר הוסר מרשימת הקניות", Toast.LENGTH_SHORT).show()
        } else {
            userShoppingList.add(item)
            Toast.makeText(this, "המוצר נוסף לרשימת הקניות", Toast.LENGTH_SHORT).show()
        }

        saveItemsToDisk()
        updateViewListButton()
        applyFilter()
    }

    private fun updateViewListButton() {
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

        Toast.makeText(this, "המוצר נמחק מהקטלוג", Toast.LENGTH_SHORT).show()
    }

    private fun openEditProduct(item: ShoppingItem) {
        val intent = Intent(this, AdminActivity::class.java)
        intent.putExtra("EDIT_ITEM", item)
        startAdminActivity.launch(intent)
    }

    private fun openVideo(url: String) {
        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } else {
            Toast.makeText(this, "אין וידאו זמין", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveItemsToDisk() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        val gson = Gson()
        prefs.putString("saved_catalog", gson.toJson(catalogItems))
        prefs.putString("saved_user_list", gson.toJson(userShoppingList))
        prefs.apply()
    }

    private fun loadItemsFromDisk() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<ShoppingItem>>() {}.type

        val catalogJson = prefs.getString("saved_catalog", null)
        if (catalogJson != null) {
            catalogItems = gson.fromJson(catalogJson, type)
        } else {
            setupInitialCatalog()
        }

        val userListJson = prefs.getString("saved_user_list", null)
        if (userListJson != null) {
            userShoppingList = gson.fromJson(userListJson, type)
        }
    }

    private fun setupInitialCatalog() {
        val vDairy = "https://www.youtube.com/watch?v=eSkBFNsQUis"
        val vProduce = "https://www.youtube.com/watch?v=iRgFLeRcZE8"
        val vCleaning = "https://www.youtube.com/shorts/sGdKLTpOoFo"
        val vBakery = "https://www.youtube.com/watch?v=M1WmB0i4Pxc"
        val vPantry = "https://www.youtube.com/watch?v=OlZRFpQQI58"
        val vMeat = "https://www.youtube.com/shorts/JOF1_eEWGT8"

        catalogItems = mutableListOf(
            ShoppingItem("עגבנייה", "קילו עגבניות שרי", "פירות וירקות", "https://m.pricez.co.il/ProductPictures/200x/Pricez65717.jpg", vProduce, 0),
            ShoppingItem("מלפפון", "קילו מלפפון מובחר", "פירות וירקות", "https://m.pricez.co.il/ProductPictures/200x/Pricez65716.jpg", vProduce, 0),
            ShoppingItem("בננה", "מארז בננות", "פירות וירקות", "https://m.pricez.co.il/ProductPictures/200x/Pricez65907.jpg", vProduce, 0),
            ShoppingItem("חלב 3%", "קרטון 1 ליטר תנובה", "מוצרי חלב וביצים", "https://m.pricez.co.il/ProductPictures/200x/7290000042442.jpg", vDairy, 0),
            ShoppingItem("גבינה צהובה", "עמק 200 גרם", "מוצרי חלב וביצים", "https://m.pricez.co.il/ProductPictures/200x/7290000052311.jpg", vDairy, 0),
            ShoppingItem("קוטג' 5%", "גביע 250 גרם", "מוצרי חלב וביצים", "https://m.pricez.co.il/ProductPictures/200x/7290004127329.jpg", vDairy, 0),
            ShoppingItem("לחם פרוס", "אחיד פרוס אנג'ל", "מאפה ודגנים", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT6ozOVUG7lZnZSGc3n6ATPNkMQY1zLsyIcFA&s", vBakery, 0),
            ShoppingItem("פסטה פוסילי", "500 גרם ברילה", "מאפה ודגנים", "https://m.pricez.co.il/ProductPictures/200x/8076802085981.jpg", vBakery, 0),
            ShoppingItem("קורנפלקס", "750 גרם תלמה", "מאפה ודגנים", "https://m.pricez.co.il/ProductPictures/200x/7290112494351.jpg", vBakery, 0),
            ShoppingItem("קוקה קולה", "בקבוק 1.5 ליטר", "שימורים ומזווה", "https://m.pricez.co.il/ProductPictures/200x/7290110115203.jpg", vPantry, 0),
            ShoppingItem("שמן זית", "750 מ\"ל יד מרדכי", "שימורים ומזווה", "https://m.pricez.co.il/ProductPictures/200x/7290010429554.jpg", vPantry, 0),
            ShoppingItem("אורז פרסי", "1 קילו סוגת", "שימורים ומזווה", "https://m.pricez.co.il/ProductPictures/200x/7290000211442.jpg", vPantry, 0),
            ShoppingItem("טונה בשמן", "מארז 4 יחידות", "בשר ודגים", "https://m.pricez.co.il/ProductPictures/200x/7290019196273.jpg", vMeat, 0),
            ShoppingItem("נוזל כלים", "700 מ\"ל פיירי", "ניקיון", "https://m.pricez.co.il/ProductPictures/200x/8700216163811.jpg", vCleaning, 0),
            ShoppingItem("נייר טואלט", "30 גלילים לילי", "ניקיון", "https://m.pricez.co.il/ProductPictures/200x/7290103702540.jpg", vCleaning, 0)
        )

        saveItemsToDisk()
    }
}