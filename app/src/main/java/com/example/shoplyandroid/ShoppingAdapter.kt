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

/**
 * Adapter להצגת מוצרי הקטלוג ב-RecyclerView.
 * תומך בהצגת תמונה, כותרת, תיאור, וידאו, והוספה לרשימת קניות.
 * למשתמש Admin מוצגים גם כפתורי עריכה ומחיקה.
 *
 * @param items רשימת המוצרים להצגה
 * @param userTitles רשימת שמות המוצרים שכבר נמצאים ברשימת הקניות של המשתמש
 * @param isAdmin האם המשתמש הנוכחי הוא אדמין
 * @param onClick callback בלחיצה על כפתור הוספה/הסרה מרשימת הקניות
 * @param onVideoClick callback בלחיצה על כפתור הווידאו
 * @param onLongClick callback בלחיצה ארוכה על פריט (לעריכה)
 * @param onDeleteClick callback בלחיצה על כפתור המחיקה
 */
class ShoppingAdapter(
    private var items: List<ShoppingItem>,
    private val userTitles: List<String>,
    private val isAdmin: Boolean,
    private val onClick: (ShoppingItem) -> Unit,
    private val onVideoClick: (String) -> Unit,
    private val onLongClick: (ShoppingItem) -> Unit,
    private val onDeleteClick: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    /**
     * ViewHolder המחזיק את רכיבי ה-UI של פריט מוצר בודד.
     */
    class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle: TextView = view.findViewById(R.id.itemTitle)
        val itemDescription: TextView = view.findViewById(R.id.itemDescription)
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        val btnAddToList: Button = view.findViewById(R.id.btnAddToList)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    /**
     * ממלא את נתוני המוצר ב-ViewHolder.
     * טוען תמונה באמצעות Glide, מגדיר מצב כפתור הוספה לפי רשימת המשתמש,
     * ומציג/מסתיר פקדי אדמין לפי תפקיד המשתמש.
     */
    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position]

        holder.itemTitle.text = item.title
        holder.itemDescription.text = item.description

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(item.imageRes.takeIf { it != 0 } ?: android.R.drawable.ic_menu_report_image)
            .into(holder.itemImage)

        if (isAdmin) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.itemView.setOnLongClickListener(null)
            holder.btnDelete.setOnClickListener(null)
        }

        // שינוי מראה הכפתור לפי האם המוצר כבר ברשימת הקניות
        if (userTitles.contains(item.title)) {
            holder.btnAddToList.text = "✓"
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.LTGRAY)
        } else {
            holder.btnAddToList.text = "+"
            holder.btnAddToList.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        }

        holder.btnAddToList.setOnClickListener {
            onClick(item)
        }

        holder.btnPlay.setOnClickListener {
            onVideoClick(item.videoUrl)
        }
    }

    override fun getItemCount(): Int = items.size
}