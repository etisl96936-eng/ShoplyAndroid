package com.example.shoplyandroid // שינוי חבילה

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
    private val selectedItemsNames: List<String>,
    private val onAddClick: (ShoppingItem) -> Unit,
    private val onVideoClick: (String) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ShoppingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.itemImage)
        val tvTitle: TextView = view.findViewById(R.id.itemTitle)
        val tvDescription: TextView = view.findViewById(R.id.itemDescription)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnAdd: Button = view.findViewById(R.id.btnAddToList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description

        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(item.imageRes)
        }

        holder.btnAdd.setOnClickListener { onAddClick(item) } // הוספת הלוגיקה לכפתור ה- "+"
        holder.btnPlay.setOnClickListener { onVideoClick(item.videoUrl) }
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount() = items.size
}