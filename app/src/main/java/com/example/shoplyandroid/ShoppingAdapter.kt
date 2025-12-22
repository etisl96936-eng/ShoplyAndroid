package com.example.shoplyandroid

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ShoppingAdapter(private var items: List<ShoppingItem>) :
    RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    // ViewHolder: מחזיק את הרכיבים הוויזואליים של כל שורה
    class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val description: TextView = view.findViewById(R.id.itemDescription)
        val image: ImageView = view.findViewById(R.id.itemImage)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position]

        // הגדרת הטקסטים
        holder.title.text = item.title
        holder.description.text = item.description

        // טעינת תמונה מהרשת בעזרת Glide [cite: 61]
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image) // תמונה זמנית עד שהטעינה תסתיים
            .into(holder.image)

        // הפעלת וידאו בלחיצה על Play באמצעות Implicit Intent [cite: 56]
        holder.btnPlay.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.videoUrl))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    // פונקציה לעדכון הרשימה לאחר סינון או חיפוש [cite: 54, 55]
    fun updateList(newList: List<ShoppingItem>) {
        items = newList
        notifyDataSetChanged()
    }
}