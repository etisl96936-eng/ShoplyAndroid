package com.example.shoplyandroid
import java.io.Serializable

data class ShoppingItem(
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val videoUrl: String,
    val imageRes: Int           // נשמור בשביל תאימות אחורה
) : Serializable