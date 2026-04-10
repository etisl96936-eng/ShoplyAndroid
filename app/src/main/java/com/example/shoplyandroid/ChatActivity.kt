package com.example.shoplyandroid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        chatAdapter = ChatAdapter(messages, auth.currentUser?.uid ?: "")
        rvMessages.layoutManager = LinearLayoutManager(this).also {
            it.stackFromEnd = true
        }
        rvMessages.adapter = chatAdapter

        listenToMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val uid = auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val displayName = prefs.getString("DISPLAY_NAME", "") ?: ""
        val username = prefs.getString("USERNAME", "") ?: ""
        val senderName = if (displayName.isNotBlank()) displayName else username

        val message = hashMapOf(
            "senderId" to uid,
            "senderName" to senderName,
            "message" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chat").add(message)
            .addOnSuccessListener {
                etMessage.setText("")
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בשליחת ההודעה", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToMessages() {
        db.collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val msg = ChatMessage(
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                    messages.add(msg)
                }
                chatAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
    }
}

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        val tvMessageText: TextView = view.findViewById(R.id.tvMessageText)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvSenderName.text = if (msg.senderId == currentUserId) "אני" else msg.senderName
        holder.tvMessageText.text = msg.message

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.tvTimestamp.text = sdf.format(Date(msg.timestamp))
    }

    override fun getItemCount() = messages.size
}