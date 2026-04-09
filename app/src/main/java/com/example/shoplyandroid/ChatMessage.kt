package com.example.shoplyandroid

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)