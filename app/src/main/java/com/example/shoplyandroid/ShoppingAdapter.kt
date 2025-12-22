package com.example.shoplyandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShoppingAdapter(
    private var items: MutableList<ShoppingItem>,
    private val userListTitles: List<String>,
    private val onAddClick: (ShoppingItem) -> Unit,
    private val onVideoClick: (String) -> Unit
) : RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle: TextView = view.findViewById(R.id.itemTitle)
        val itemDescription: TextView = view.findViewById(R.id.itemDescription)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        val btnAddToList: Button = view.findViewById(R.id.btnAddToList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position]
        holder.itemTitle.text = item.title
        holder.itemDescription.text = item.description

        // בדיקה האם המוצר כבר ברשימה
        if (userListTitles.contains(item.title)) {
            // מוצר נמצא ברשימה - הצגת V
            holder.btnAddToList.text = "✓" // או שימוש באייקון אם תרצי בהמשך
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.LTGRAY)
        } else {
            // מוצר לא נמצא - הצגת +
            holder.btnAddToList.text = "+"
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        }

        holder.btnAddToList.setOnClickListener { onAddClick(item) }
        holder.btnPlay.setOnClickListener { onVideoClick(item.videoUrl) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: MutableList<ShoppingItem>) {
        this.items = newList
        notifyDataSetChanged()
    }
}