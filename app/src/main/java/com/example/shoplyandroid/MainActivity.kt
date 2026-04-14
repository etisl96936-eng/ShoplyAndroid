package com.example.shoplyandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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

/**
 * מחלקה זו מייצגת את המסך הראשי של האפליקציה.
 *
 * תחומי האחריות של המחלקה:
 * 1. הצגת קטלוג המוצרים למשתמש.
 * 2. סינון מוצרים לפי טקסט חיפוש וקטגוריה.
 * 3. ניהול רשימת הקניות האישית של המשתמש.
 * 4. טעינת מוצרים מ-Firestore עם Pagination.
 * 5. שמירה מקומית של נתונים לצורך שיפור חוויית השימוש.
 * 6. ניהול הרשאות תצוגה ופעולות לפי תפקיד משתמש (Admin / User).
 * 7. ניווט למסכים נוספים: פרופיל, סטטיסטיקות, צ'אט, הוספה ועריכת מוצרים.
 *
 * זהו המסך המרכזי באפליקציה, ולכן הוא מרכז בתוכו את רוב הלוגיקה העסקית
 * הקשורה לתצוגת המוצרים ולפעולות המשתמש.
 */
class MainActivity : AppCompatActivity() {

    /** מתאם ה-RecyclerView האחראי להצגת רשימת המוצרים במסך. */
    private lateinit var adapter: ShoppingAdapter

    /** רשימת כל המוצרים בקטלוג כפי שנטענו מהמסד או מהשמירה המקומית. */
    private var catalogItems: MutableList<ShoppingItem> = mutableListOf()

    /** רשימת הקניות האישית של המשתמש המחובר. */
    private var userShoppingList: MutableList<ShoppingItem> = mutableListOf()

    /** רכיב ה-RecyclerView המציג את הנתונים על גבי המסך. */
    private lateinit var recyclerView: RecyclerView

    /** כפתור מעבר בין הקטלוג המלא לבין תצוגת רשימת הקניות האישית. */
    private lateinit var btnViewList: Button

    /** תווית ברכה למשתמש המחובר. */
    private lateinit var tvWelcome: TextView

    /**
     * דגל פנימי המציין האם כרגע המסך מציג רק את רשימת הקניות של המשתמש,
     * או את הקטלוג המלא.
     */
    private var isShowingOnlyCart = false

    /**
     * דגל עזר פנימי למניעת הפעלה לא רצויה של אירוע בחירת קטגוריה ב-Spinner.
     * נועד לשמור על חוויית משתמש תקינה בזמן מעבר בין תצוגות.
     */
    private var suppressNextSpinnerSelection = false

    /** המסמך האחרון שנטען מ-Firestore לצורך Pagination. */
    private var lastVisibleProduct: DocumentSnapshot? = null

    /** מציין האם הגענו לעמוד האחרון של הנתונים ב-Firestore. */
    private var isLastPage = false

    /** מציין האם כרגע מתבצעת טעינת מוצרים, למניעת בקשות כפולות. */
    private var isLoadingProducts = false

    /** גודל עמוד טעינה מ-Firestore – מספר המוצרים שייטענו בכל פעם. */
    private val firestorePageSize = 5

    /** גישה למסד הנתונים Firestore. */
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /** גישה לשירות האימות של Firebase. */
    private val auth = FirebaseAuth.getInstance()

    /**
     * מנגנון לקבלת תוצאה חזרה ממסך הניהול (AdminActivity).
     *
     * תפקידו:
     * - לקלוט מוצר חדש שנוסף או מוצר קיים שעודכן.
     * - לזהות האם בוצעה מחיקה של מוצר.
     * - לעדכן את הנתונים ב-Firestore בהתאם לפעולה שבוצעה.
     *
     * הבחירה ב-ActivityResultContracts.StartActivityForResult מאפשרת
     * ניהול תקני ומודרני של תקשורת בין מסכים באנדרואיד.
     */
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

    /**
     * המתודה המרכזית שמופעלת בעת יצירת המסך.
     *
     * כאן מתבצעים:
     * - אתחול רכיבי ממשק
     * - קריאת הרשאות המשתמש
     * - הגדרת מאזיני אירועים
     * - טעינת נתונים מקומית
     * - בקשת הרשאות מערכת במידת הצורך
     * - טעינת הדף הראשון של המוצרים מ-Firestore
     */
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

        // עדכון טקסט הברכה בהתאם למשתמש המחובר.
        updateWelcomeText()

        // ניהול הרשאות תצוגה: רק Admin יכול לראות כפתור הוספת מוצר ורמז ניהולי.
        if (isAdmin) {
            fabAddProduct.visibility = View.VISIBLE
            tvAdminHint.visibility = View.VISIBLE
        } else {
            fabAddProduct.visibility = View.GONE
            tvAdminHint.visibility = View.GONE
        }

        // טעינת נתונים מקומית לשיפור מהירות הפתיחה וחוויית המשתמש.
        loadItemsFromDisk()

        val categories = arrayOf(
            "הכל", "פירות וירקות", "מוצרי חלב וביצים",
            "ניקיון", "מאפה ודגנים", "שימורים ומזווה", "בשר ודגים"
        )

        /**
         * באנדרואיד 13 ומעלה, יש צורך לבקש הרשאת התראות בזמן ריצה.
         * זה מאפשר לאפליקציה בעתיד להציג התראות רלוונטיות למשתמש.
         */
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

        // הפעלת סינון ראשוני ועדכון טקסט הכפתור.
        applyFilter()
        updateViewListButton()

        /**
         * מאזין לטקסט חיפוש.
         * בכל שינוי בטקסט, הסינון מופעל מחדש כדי לספק חיפוש דינמי בזמן אמת.
         */
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilter()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        /**
         * מאזין לבחירת קטגוריה ב-Spinner.
         * כאשר המשתמש בוחר קטגוריה:
         * - אם המסך היה במצב "רשימת קניות בלבד", נחזור אוטומטית לקטלוג המלא.
         * - לאחר מכן נבצע סינון מחדש.
         */
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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

        /**
         * טיפול מיוחד במגע על ה-Spinner.
         * נועד לשמור על עקביות ממשק כאשר המשתמש נמצא בתצוגת סל בלבד.
         */
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

        /**
         * מעבר בין מצב קטלוג מלא לבין מצב תצוגת סל אישי.
         * בנוסף, הכפתור משנה צבע כדי לייצג את מצב התצוגה הנוכחי.
         */
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

        // מעבר למסך הסטטיסטיקות.
        btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // מעבר למסך הצ'אט.
        val btnChat = findViewById<Button>(R.id.btnChat)
        btnChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        // מעבר למסך הפרופיל.
        btnProfileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // פתיחת מסך אדמין להוספה או עריכה של מוצר.
        fabAddProduct.setOnClickListener {
            startAdminActivity.launch(Intent(this, AdminActivity::class.java))
        }

        // טעינת עמוד נוסף של מוצרים.
        btnLoadMore.setOnClickListener {
            loadMoreProductsFromFirestore()
        }

        /**
         * ביצוע Logout:
         * - ניתוק המשתמש מ-FirebaseAuth
         * - ניקוי נתונים מקומיים
         * - מעבר למסך ההתחברות
         */
        btnLogout.setOnClickListener {
            auth.signOut()
            val prefsEdit = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
            prefsEdit.clear()
            prefsEdit.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // טעינת קבוצת המוצרים הראשונה מהשרת.
        loadProductsFirstPage()
    }

    /**
     * מתודה זו מופעלת כאשר המסך חוזר לחזית.
     * משמשת לרענון טקסט הברכה, למשל לאחר שינוי שם תצוגה בפרופיל.
     */
    override fun onResume() {
        super.onResume()
        updateWelcomeText()
    }

    /**
     * מעדכנת את הודעת הברכה למשתמש.
     * אם קיים displayName יוצג הוא, אחרת יוצג האימייל.
     */
    private fun updateWelcomeText() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val displayName = prefs.getString("DISPLAY_NAME", "") ?: ""
        val nameToShow = if (displayName.isNotBlank()) displayName else username
        tvWelcome.text = if (nameToShow.isNotBlank()) "שלום, $nameToShow" else "שלום"
    }

    /**
     * מבצעת סינון של הנתונים המוצגים לפי:
     * - מצב תצוגה נוכחי (קטלוג / סל)
     * - טקסט חיפוש
     * - קטגוריה נבחרת
     *
     * בנוסף:
     * - מטפלת במצב ריק
     * - מעדכנת את תצוגת כפתור "טען עוד"
     */
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
            tvEmptyState.text = if (isShowingOnlyCart) {
                "רשימת הקניות שלך ריקה כרגע"
            } else {
                "לא נמצאו מוצרים"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            updateAdapter(filteredList)
        }

        updateLoadMoreVisibility()
    }

    /**
     * יוצרת ומחברת Adapter חדש ל-RecyclerView.
     *
     * המתאם מקבל:
     * - את הרשימה המסוננת להצגה
     * - מידע על מוצרים שכבר נמצאים בסל
     * - מידע האם המשתמש הוא Admin
     * - callbacks לפעולות משתמש: הוספה/הסרה, עריכה, מחיקה
     */
    private fun updateAdapter(newList: MutableList<ShoppingItem>) {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("IS_ADMIN", false)

        adapter = ShoppingAdapter(
            newList,
            userShoppingList.map { it.title },
            isAdmin,
            { item -> toggleProduct(item) },
            { item -> openEditProduct(item) },
            { item -> deleteItemFromCatalog(item) }
        )
        recyclerView.adapter = adapter
    }

    /**
     * מוסיפה או מסירה מוצר מרשימת הקניות האישית של המשתמש.
     *
     * לאחר הפעולה:
     * - נשמרת הרשימה מקומית ובמסד
     * - הכפתור העליון מתעדכן
     * - התצוגה מתרעננת
     */
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

    /**
     * מעדכנת את טקסט הכפתור העליון בהתאם למצב הסל:
     * - אם הסל ריק
     * - אם מוצגת כרגע רשימת הקניות
     * - אם מוצג הקטלוג המלא
     */
    private fun updateViewListButton() {
        if (userShoppingList.isEmpty()) {
            btnViewList.text = "הסל ריק - הוסף מוצרים לרשימה"
        } else if (isShowingOnlyCart) {
            btnViewList.text = "חזור לקטלוג המלא"
        } else {
            btnViewList.text = "צפה ברשימת הקניות שלי (${userShoppingList.size} מוצרים)"
        }
    }

    /** מחיקה לוגית של פריט דרך שכבת המסד. */
    private fun deleteItemFromCatalog(item: ShoppingItem) {
        deleteProductFromFirestore(item.title)
    }

    /**
     * מוחק מוצר מ-Firestore לפי כותרת המוצר.
     *
     * לאחר מחיקה מוצלחת:
     * - המוצר מוסר גם מהקטלוג המקומי
     * - המוצר מוסר גם מרשימת הקניות אם היה קיים שם
     * - הנתונים נשמרים מחדש
     * - נטען מחדש הדף הראשון לצורך רענון
     */
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
                Toast.makeText(this, "שגיאה במחיקת מוצר: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    /**
     * פתיחת מסך עריכת מוצר והעברת הפריט הנבחר למסך הניהול.
     */
    private fun openEditProduct(item: ShoppingItem) {
        val intent = Intent(this, AdminActivity::class.java)
        intent.putExtra("EDIT_ITEM", item)
        startAdminActivity.launch(intent)
    }

    /**
     * שומר מוצר ב-Firestore.
     *
     * פעולה זו מותרת רק למשתמש אדמין.
     * אם הפעולה מצליחה:
     * - המוצר נשמר במסד
     * - הקטלוג נטען מחדש
     */
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

    /**
     * שומר את הקטלוג מקומית ב-SharedPreferences בפורמט JSON.
     *
     * מטרה:
     * - שיפור זמן טעינה
     * - שמירה על זמינות נתונים בסיסית גם לפני סנכרון מחדש מהשרת
     */
    private fun saveCatalogLocally() {
        val gson = Gson()
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE).edit()
        prefs.putString("saved_catalog", gson.toJson(catalogItems))
        prefs.apply()
    }

    /**
     * שומר את רשימת הקניות המקומית וגם מסנכרן אותה ל-Firestore תחת המשתמש הנוכחי.
     *
     * כך נשמרת התמדה של נתוני המשתמש גם בין מכשירים או התחברויות שונות.
     */
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

    /**
     * טוען את רשימת הקניות של המשתמש מתוך Firestore.
     *
     * המימוש שומר במסד את שמות המוצרים בלבד,
     * ולאחר מכן מתאים אותם למוצרים המלאים שכבר נטענו לקטלוג.
     */
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

    /**
     * טוען נתונים שנשמרו מקומית מההרצה הקודמת:
     * - קטלוג מוצרים
     * - רשימת קניות
     *
     * שימוש במנגנון זה תורם לזמינות נתונים מהירה עם פתיחת האפליקציה.
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

        applyFilter()
        updateViewListButton()
    }

    /**
     * טוען את קבוצת המוצרים הראשונה מ-Firestore.
     *
     * המימוש כולל Pagination:
     * - מיון לפי title
     * - טעינה מוגבלת לפי firestorePageSize
     * - שמירת המסמך האחרון לצורך המשך טעינה
     *
     * זהו פתרון חשוב מבחינת ביצועים, משום שאין צורך לטעון את כל הנתונים בבת אחת.
     */
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
                Toast.makeText(this, "שגיאה בטעינת מוצרים: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    .show()
                applyFilter()
                updateViewListButton()
            }
    }

    /**
     * טוען עמוד נוסף של מוצרים מ-Firestore.
     *
     * השיטה מתבססת על:
     * - startAfter(lastVisibleProduct)
     * - טעינה הדרגתית
     * - עצירה אוטומטית כאשר אין עוד נתונים
     *
     * זהו יישום מלא של מנגנון Pagination בצד הלקוח.
     */
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
                Toast.makeText(
                    this,
                    "שגיאה בטעינת מוצרים נוספים: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * קובעת האם כפתור "טען עוד" יוצג למשתמש.
     *
     * הכפתור יוצג רק כאשר:
     * - לא מוצגת רשימת הקניות האישית
     * - לא הגענו לעמוד האחרון
     * - קיימים מוצרים בקטלוג
     */
    private fun updateLoadMoreVisibility() {
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)
        btnLoadMore.visibility =
            if (!isShowingOnlyCart && !isLastPage && catalogItems.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}