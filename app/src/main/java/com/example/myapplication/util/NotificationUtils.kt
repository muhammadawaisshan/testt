package com.example.myapplication.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import javax.inject.Inject
import com.example.myapplication.R

class NotificationUtil @Inject constructor() {

    fun sendNotification(
        messageTitle: String,
        messageBody: String,
        notificationSound: Uri?,
        notificationId: Int,
        context: Context,


        ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "notification_channel"
        val notificationBuilder =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle).setContentText(messageBody).setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Custom Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)

            val audioAttributes =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

            // Set custom sound for the channel
            notificationSound.let {
                channel.setSound(it, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
        return notificationBuilder.build()
    }
}