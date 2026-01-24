package com.example.shoplyandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ShoppingAdapter(
    private var items: List<ShoppingItem>,
    private val userTitles: List<String>,
    private val onClick: (ShoppingItem) -> Unit,
    private val onVideoClick: (String) -> Unit,
    private val onLongClick: (ShoppingItem) -> Unit,
    private val onDeleteClick: (ShoppingItem) -> Unit // לוגיקת המחיקה שלך
) : RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle: TextView = view.findViewById(R.id.itemTitle)
        val itemDescription: TextView = view.findViewById(R.id.itemDescription)
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        val btnAddToList: Button = view.findViewById(R.id.btnAddToList)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position]
        holder.itemTitle.text = item.title
        holder.itemDescription.text = item.description

        // שימוש ב-Glide לטעינה חכמה
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(item.imageRes.takeIf { it != 0 } ?: android.R.drawable.ic_menu_report_image)
            .into(holder.itemImage)

        // לחיצה ארוכה לעריכה
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }

        // עדכון כפתור הוספה
        if (userTitles.contains(item.title)) {
            holder.btnAddToList.text = "✓"
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.LTGRAY)
        } else {
            holder.btnAddToList.text = "+"
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        }

        holder.btnAddToList.setOnClickListener { onClick(item) }
        holder.btnPlay.setOnClickListener { onVideoClick(item.videoUrl) }

        // כפתור מחיקה מהיר (הבונוס)
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size
}