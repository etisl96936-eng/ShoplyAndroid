package com.example.shoplyandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    private var suppressNextSpinnerSelection = false

    private var lastVisibleProduct: DocumentSnapshot? = null
    private var isLastPage = false
    private var isLoadingProducts = false
    private val firestorePageSize = 5

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val startAdminActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isDelete = data?.getBooleanExtra("IS_DELETE", false) ?: false

            if (isDelete) {
                val titleToDelete = data?.getStringExtra("DELETED_PRODUCT_TITLE")
                titleToDelete?.let { deleteProductFromFirestore(it) }
            } else {
                val returnedItem = data?.getSerializableExtra("NEW_PRODUCT") as? ShoppingItem
                returnedItem?.let { item ->
                    saveProductToFirestore(item)
                }
            }
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
        val fabAddProduct = findViewById<FloatingActionButton>(R.id.fabAddProduct)
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
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
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
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressNextSpinnerSelection) {
                    suppressNextSpinnerSelection = false
                    return
                }
                if (isShowingOnlyCart) {
                    isShowingOnlyCart = false
                    btnViewList.setBackgroundColor(Color.parseColor("#4CAF50"))
                    updateViewListButton()
                }
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerCategory.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN &&
                isShowingOnlyCart &&
                spinnerCategory.selectedItem?.toString() == "הכל"
            ) {
                suppressNextSpinnerSelection = true
                if (spinnerCategory.adapter.count > 1) {
                    spinnerCategory.setSelection(1, false)
                }
            }
            false
        }

        btnViewList.setOnClickListener {
            isShowingOnlyCart = !isShowingOnlyCart
            if (isShowingOnlyCart) {
                btnViewList.setBackgroundColor(Color.GRAY)
                etSearch.text?.clear()
            } else {
                btnViewList.setBackgroundColor(Color.parseColor("#4CAF50"))
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
            loadMoreProductsFromFirestore()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val prefsEdit = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
            prefsEdit.clear()
            prefsEdit.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadProductsFirstPage()
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

        val query = etSearch.text.toString().trim().lowercase()

        val filteredList = if (isShowingOnlyCart) {
            userShoppingList.filter { item ->
                item.title.lowercase().contains(query)
            }.toMutableList()
        } else {
            val selectedCat = spinnerCategory.selectedItem?.toString() ?: "הכל"
            catalogItems.filter { item ->
                val matchesQuery = item.title.lowercase().contains(query)
                val matchesCat = selectedCat == "הכל" || item.category == selectedCat
                matchesQuery && matchesCat
            }.toMutableList()
        }

        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = if (isShowingOnlyCart) "רשימת הקניות שלך ריקה כרגע" else "לא נמצאו מוצרים"
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            updateAdapter(filteredList)
        }

        updateLoadMoreVisibility()
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
        deleteProductFromFirestore(item.title)
    }

    private fun deleteProductFromFirestore(title: String) {
        db.collection("products")
            .document(title)
            .delete()
            .addOnSuccessListener {
                catalogItems.removeAll { it.title == title }
                userShoppingList.removeAll { it.title == title }
                saveCatalogLocally()
                saveUserShoppingList()
                loadProductsFirstPage()
                Toast.makeText(this, "המוצר נמחק מהקטלוג", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "שגיאה במחיקת מוצר: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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

    private fun saveProductToFirestore(item: ShoppingItem) {
        val isAdmin = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
            .getBoolean("IS_ADMIN", false)

        if (!isAdmin) {
            Toast.makeText(this, "אין הרשאת אדמין לשמירת מוצר", Toast.LENGTH_LONG).show()
            return
        }

        val productData = hashMapOf(
            "title" to item.title,
            "description" to item.description,
            "category" to item.category,
            "imageUrl" to item.imageUrl,
            "videoUrl" to item.videoUrl
        )

        db.collection("products")
            .document(item.title)
            .set(productData)
            .addOnSuccessListener {
                Toast.makeText(this, "המוצר נשמר ב-Firestore", Toast.LENGTH_LONG).show()
                loadProductsFirstPage()
                Toast.makeText(this, "המוצר נוסף לקטלוג", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "שגיאה בשמירת מוצר: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveCatalogLocally() {
        val gson = Gson()
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        prefs.putString("saved_catalog", gson.toJson(catalogItems))
        prefs.apply()
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
                prefsEdit.putString("saved_user_list", Gson().toJson(userShoppingList))
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
    }

    private fun loadProductsFirstPage() {
        if (isLoadingProducts) return

        isLoadingProducts = true
        lastVisibleProduct = null
        isLastPage = false

        db.collection("products")
            .orderBy("title")
            .limit(firestorePageSize.toLong())
            .get()
            .addOnSuccessListener { documents ->
                isLoadingProducts = false
                catalogItems.clear()

                if (documents.isEmpty) {
                    isLastPage = true
                    saveCatalogLocally()
                    applyFilter()
                    updateViewListButton()
                    return@addOnSuccessListener
                }

                val loadedItems = documents.map { doc ->
                    ShoppingItem(
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        imageRes = 0
                    )
                }

                catalogItems.addAll(loadedItems)
                lastVisibleProduct = documents.documents.lastOrNull()
                isLastPage = documents.size() < firestorePageSize

                saveCatalogLocally()
                loadUserShoppingList()
                applyFilter()
                updateViewListButton()
            }
            .addOnFailureListener { e ->
                isLoadingProducts = false
                Toast.makeText(this, "שגיאה בטעינת מוצרים: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                applyFilter()
                updateViewListButton()
            }
    }

    private fun loadMoreProductsFromFirestore() {
        if (isLoadingProducts || isLastPage || lastVisibleProduct == null) return

        isLoadingProducts = true

        db.collection("products")
            .orderBy("title")
            .startAfter(lastVisibleProduct!!)
            .limit(firestorePageSize.toLong())
            .get()
            .addOnSuccessListener { documents ->
                isLoadingProducts = false

                if (documents.isEmpty) {
                    isLastPage = true
                    updateLoadMoreVisibility()
                    Toast.makeText(this, "אין עוד מוצרים לטעון", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val loadedItems = documents.map { doc ->
                    ShoppingItem(
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        imageRes = 0
                    )
                }

                catalogItems.addAll(loadedItems)
                lastVisibleProduct = documents.documents.lastOrNull()
                isLastPage = documents.size() < firestorePageSize

                saveCatalogLocally()
                loadUserShoppingList()
                applyFilter()
            }
            .addOnFailureListener { e ->
                isLoadingProducts = false
                Toast.makeText(this, "שגיאה בטעינת מוצרים נוספים: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateLoadMoreVisibility() {
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)
        btnLoadMore.visibility =
            if (!isShowingOnlyCart && !isLastPage && catalogItems.isNotEmpty()) View.VISIBLE
            else View.GONE
    }
}