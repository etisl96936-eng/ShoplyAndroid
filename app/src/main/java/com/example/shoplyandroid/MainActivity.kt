package com.example.shoplyandroid

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ShoppingAdapter
    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnViewList: Button
    private lateinit var tvWelcome: TextView
    private var isShowingOnlyCart = false

    private var visibleItemCount = 5
    private val pageSize = 5

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
                Toast.makeText(this@MainActivity, "המוצר נמחק מהקטלוג", Toast.LENGTH_SHORT).show()
            } else {
                val returnedItem = data?.getSerializableExtra("NEW_PRODUCT") as? ShoppingItem
                returnedItem?.let { item ->
                    val existingIndex = catalogItems.indexOfFirst { it.title == item.title }
                    if (existingIndex != -1) {
                        catalogItems[existingIndex] = item
                        Toast.makeText(this@MainActivity, "המוצר עודכן בקטלוג", Toast.LENGTH_SHORT).show()
                    } else {
                        catalogItems.add(0, item)
                        Toast.makeText(this@MainActivity, "המוצר נוסף לקטלוג", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            saveCatalog()
            saveUserShoppingList()
            applyFilter()
            updateViewListButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        btnViewList = findViewById(R.id.btnViewList)
        tvWelcome = findViewById(R.id.tvWelcome)

        val btnProfileIcon = findViewById<ImageButton>(R.id.btnProfileIcon)
        val btnStatistics = findViewById<Button>(R.id.btnStatistics)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val fabAddProduct =
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddProduct)
        val tvAdminHint = findViewById<TextView>(R.id.tvAdminHint)
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        updateWelcomeText()

        if (isAdmin) {
            fabAddProduct.visibility = View.VISIBLE
            tvAdminHint.visibility = View.VISIBLE
        } else {
            fabAddProduct.visibility = View.GONE
            tvAdminHint.visibility = View.GONE
        }

        loadItemsFromDisk()

        val categories = arrayOf(
            "הכל", "פירות וירקות", "מוצרי חלב וביצים",
            "ניקיון", "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }

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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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

        btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        val btnChat = findViewById<Button>(R.id.btnChat)
        btnChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnProfileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
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

        btnLogout.setOnClickListener {
            auth.signOut()
            val prefsEdit = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
            prefsEdit.clear()
            prefsEdit.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateWelcomeText()
    }

    private fun updateWelcomeText() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val displayName = prefs.getString("DISPLAY_NAME", "") ?: ""
        val nameToShow = if (displayName.isNotBlank()) displayName else username
        tvWelcome.text = if (nameToShow.isNotBlank()) "שלום, $nameToShow" else "שלום"
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
            tvEmptyState.text = if (isShowingOnlyCart) "רשימת הקניות שלך ריקה כרגע" else "לא נמצאו מוצרים"
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            val visibleList = filteredList.take(visibleItemCount).toMutableList()
            updateAdapter(visibleList)
            btnLoadMore.visibility = if (filteredList.size > visibleItemCount) View.VISIBLE else View.GONE
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

        saveUserShoppingList()
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
        catalogItems.removeAll { it.title == item.title }
        userShoppingList.removeAll { it.title == item.title }

        saveCatalog()
        saveUserShoppingList()
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
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            Toast.makeText(this, "אין וידאו זמין", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCatalog() {
        val gson = Gson()
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        prefs.putString("saved_catalog", gson.toJson(catalogItems))
        prefs.apply()

        val uid = auth.currentUser?.uid ?: return
        val isAdmin = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).getBoolean("IS_ADMIN", false)

        if (!isAdmin) return

        val catalogData = catalogItems.map { item ->
            hashMapOf(
                "title" to item.title,
                "description" to item.description,
                "category" to item.category,
                "imageUrl" to item.imageUrl,
                "videoUrl" to item.videoUrl
            )
        }

        db.collection("catalog")
            .document("items")
            .set(hashMapOf("products" to catalogData))
    }

    private fun saveUserShoppingList() {
        val gson = Gson()
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        prefs.putString("saved_user_list", gson.toJson(userShoppingList))
        prefs.apply()

        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("shoppingList")
            .document("myList")
            .set(hashMapOf("items" to userShoppingList.map { it.title }))
    }

    private fun loadUserShoppingList() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("shoppingList")
            .document("myList")
            .get()
            .addOnSuccessListener { doc ->
                val titles = doc.get("items") as? List<String> ?: emptyList()
                userShoppingList = catalogItems.filter { it.title in titles }.toMutableList()

                val prefsEdit = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                val gson = Gson()
                prefsEdit.putString("saved_user_list", gson.toJson(userShoppingList))
                prefsEdit.apply()

                updateViewListButton()
                applyFilter()
            }
            .addOnFailureListener {
                updateViewListButton()
                applyFilter()
            }
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

        applyFilter()
        updateViewListButton()

        db.collection("catalog").document("items")
            .get()
            .addOnSuccessListener { doc ->
                val products = doc.get("products") as? List<Map<String, Any>> ?: emptyList()
                if (products.isNotEmpty()) {
                    catalogItems = products.map { map ->
                        ShoppingItem(
                            title = map["title"] as? String ?: "",
                            description = map["description"] as? String ?: "",
                            category = map["category"] as? String ?: "",
                            imageUrl = map["imageUrl"] as? String ?: "",
                            videoUrl = map["videoUrl"] as? String ?: "",
                            imageRes = 0
                        )
                    }.toMutableList()

                    val prefsEdit = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
                    prefsEdit.putString("saved_catalog", gson.toJson(catalogItems))
                    prefsEdit.apply()
                }

                loadUserShoppingList()
            }
            .addOnFailureListener {
                applyFilter()
                updateViewListButton()
            }
    }
}