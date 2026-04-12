package com.example.shoplyandroid

/**
 * מודל נתונים המייצג הודעה בצ'אט הקבוצתי.
 * ברירות המחדל נדרשות כדי ש-Firestore יוכל לבצע deserialization אוטומטי.
 *
 * @param senderId ה-UID של המשתמש ששלח את ההודעה
 * @param senderName שם התצוגה של השולח
 * @param message תוכן ההודעה
 * @param timestamp חותמת זמן של שליחת ההודעה במילישניות
 */
data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)