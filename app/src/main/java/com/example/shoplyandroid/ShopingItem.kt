package com.example.shoplyandroid // שינוי חבילה כדי שתתאים ל-MainActivity

import java.io.Serializable

data class ShoppingItem(
    val title: String,
    val description: String,
    val category: String,
    val imageRes: Int,
    val imageUrl: String? = null,
    val videoUrl: String,
    var isSelected: Boolean = false
) : Serializable