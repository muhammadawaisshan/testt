package com.example.myapplication.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.example.myapplication.R
import com.example.myapplication.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TheftService : Service(), StabilityDetector.StabilityListener {
    @Inject
    lateinit var notificationUtil: NotificationUtil

    @Inject
    lateinit var detector: StabilityDetector

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.clock)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            mediaPlayer?.release()
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = notificationUtil.sendNotification(
            messageTitle = "Anti Theft",
            messageBody = "Service Running...",
            notificationSound = null,
            notificationId = 101,
            context = this
        )
        startForeground(101, notification)
        detector.setStabilityListener(this)
        detector.startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.stopListening()
        mediaPlayer?.release()
    }

    override fun onPhoneUnstable() {
        detector.stopListening()

        mediaPlayer?.apply {
            if (!isPlaying) {
                isLooping = true
                start()
            }
        }
    }
}