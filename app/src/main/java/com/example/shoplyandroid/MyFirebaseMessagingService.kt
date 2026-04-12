package com.example.shoplyandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * שירות FCM (Firebase Cloud Messaging) לקבלת Push Notifications.
 * רץ ברקע ומאזין להודעות נכנסות מ-Firebase גם כשהאפליקציה סגורה.
 * בעת קבלת הודעה — מציג אותה כ-Notification במכשיר.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * נקרא כאשר מתקבלת הודעת Push מ-Firebase.
     * מחלץ את הכותרת והתוכן ומציג אותם כהתראה.
     *
     * @param remoteMessage ההודעה שהתקבלה מ-Firebase
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "Shoply"
        val body = remoteMessage.notification?.body ?: ""

        showNotification(title, body)
    }

    /**
     * נקרא כאשר נוצר או מתחדש FCM Token למכשיר.
     * ניתן לשמור את ה-Token ב-Firestore לצורך שליחת התראות ממוקדות למשתמש ספציפי.
     *
     * @param token ה-Token החדש של המכשיר
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * בונה ומציג התראה (Notification) במכשיר.
     * יוצר Notification Channel עבור Android 8 ומעלה.
     * לחיצה על ההתראה פותחת את MainActivity.
     *
     * @param title כותרת ההתראה
     * @param body תוכן ההתראה
     */
    private fun showNotification(title: String, body: String) {
        val channelId = "shoply_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // יצירת Notification Channel נדרשת עבור Android 8.0 (Oreo) ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shoply Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}