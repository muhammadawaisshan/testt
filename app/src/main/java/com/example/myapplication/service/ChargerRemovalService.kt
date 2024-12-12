package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.presentation.MainActivity

class ChargerRemovalService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var chargerReceiver: BroadcastReceiver
    private lateinit var screenReceiver: ScreenReceiver

    private val CHANNEL_ID = "ChargerRemovalServiceChannel"
    private val NOTIFICATION_ID = 2
    private val ACTION_STOP_SERVICE = "STOP_SERVICE"

    private var unlockCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaPlayer = MediaPlayer.create(this, R.raw.clock)
        mediaPlayer.isLooping = true

        chargerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                    startAlarm()
                }
            }
        }

        screenReceiver = ScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())

        val chargerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chargerReceiver, chargerFilter, RECEIVER_EXPORTED)
            registerReceiver(screenReceiver, screenFilter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(chargerReceiver, chargerFilter)
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(screenReceiver, screenFilter)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        unregisterReceiver(chargerReceiver)
        unregisterReceiver(screenReceiver)
    }

    private fun startAlarm() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }
    }

    private fun createNotificationChannel() {
        val name = "Charger Removal Service"
        val descriptionText = "Monitors if the charger is removed"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ChargerRemovalService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Charger Removal Protection")
            .setContentText("Monitoring charger connection")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
  inner  class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {

                Log.d("ChargerRemovalService", "Screen Locked")
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {

                Log.d("ChargerRemovalService", "Screen Unlocked")


               unlockCount++

                if (unlockCount % 2==0) {
                    stopAlarm()
                }
            }
        }
    }
}