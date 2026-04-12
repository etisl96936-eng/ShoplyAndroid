package com.example.shoplyandroid

import java.io.Serializable

/**
 * מודל נתונים המייצג מוצר בקטלוג הקניות.
 * מממש Serializable לצורך העברה בין Activities דרך Intent.
 *
 * @param title שם המוצר — משמש גם כמזהה ייחודי בקטלוג
 * @param description תיאור קצר של המוצר
 * @param category קטגוריית המוצר (למשל: "פירות וירקות")
 * @param imageUrl כתובת URL לתמונת המוצר
 * @param videoUrl כתובת URL לסרטון YouTube הקשור למוצר
 * @param imageRes מזהה drawable מקומי — נשמר לתאימות אחורה
 */
data class ShoppingItem(
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val videoUrl: String,
    val imageRes: Int
) : Serializable