package br.ecosynergy_app.room.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import br.ecosynergy_app.R
import br.ecosynergy_app.home.HomeActivity
import br.ecosynergy_app.room.AppDatabase
import br.ecosynergy_app.teams.DashboardActivity
import br.ecosynergy_app.teams.TeamInfoActivity
import br.ecosynergy_app.user.NotificationActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var notificationsRepository: NotificationsRepository

    override fun onCreate() {
        super.onCreate()
        // Initialize Room Database and NotificationsRepository
        val db = AppDatabase.getDatabase(applicationContext)
        notificationsRepository = NotificationsRepository(db.notificationsDao())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Extract data from the notification message
        val title = remoteMessage.data["title"] ?: "No title"
        val body = remoteMessage.data["body"] ?: "No body"
        val type = remoteMessage.data["type"]
        val inviteId = remoteMessage.data["inviteId"]
        val teamId = remoteMessage.data["teamId"]

        Log.d("MyFirebaseService", "${remoteMessage.data}")

        // Determine the target activity based on the notification type
        val targetActivity = when (type) {
            "invite" -> NotificationActivity::class.java
            "fire" -> DashboardActivity::class.java
            "team" -> TeamInfoActivity::class.java
            else -> HomeActivity::class.java
        }

        // Display the notification
        sendNotification(title, body, targetActivity)

        // Store the notification in the Room database
        saveNotificationToDatabase(type, title, body, teamId, inviteId)
    }

    private fun sendNotification(title: String, messageBody: String, targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_transparent_globe)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification, "Ver detalhes", pendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Default Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun saveNotificationToDatabase(
        type: String?,
        title: String,
        body: String,
        teamId: String?,
        inviteId: String?
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = dateFormat.format(Date(System.currentTimeMillis()))

        val notification = Notifications(
            id = 0,
            type = type,
            title = title,
            subtitle = body,
            timestamp = formattedTimestamp,
            teamId = teamId,
            inviteId = inviteId
        )

        CoroutineScope(Dispatchers.IO).launch {
            notificationsRepository.addNotification(notification)
            Log.d("MyFirebaseService", "Notification Stored: $notification")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("MyFirebaseService", "Refreshed token: $token")
    }
}
