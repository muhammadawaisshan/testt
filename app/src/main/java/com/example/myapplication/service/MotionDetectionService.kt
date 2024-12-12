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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.presentation.MainActivity

class MotionDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var mediaPlayer: MediaPlayer

    private val CHANNEL_ID = "MotionDetectionServiceChannel"
    private val NOTIFICATION_ID = 3
    private val ACTION_STOP_SERVICE = "STOP_SERVICE"

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val MOVEMENT_THRESHOLD = 10f

    private lateinit var screenReceiver: ScreenReceiver
    private var unlockCount = 0  // Counter to track screen unlocks

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        mediaPlayer = MediaPlayer.create(this, R.raw.clock)
        mediaPlayer.isLooping = true

        // Initialize ScreenReceiver
        screenReceiver = ScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Register the accelerometer sensor listener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Register the screen lock/unlock receiver
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)  // Device locked
            addAction(Intent.ACTION_SCREEN_ON)   // Device unlocked
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             registerReceiver(screenReceiver, screenFilter, RECEIVER_EXPORTED)
        } else {
             @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(screenReceiver, screenFilter)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        sensorManager.unregisterListener(this)

        // Unregister the screen receiver when service is destroyed
        unregisterReceiver(screenReceiver)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = Math.abs(lastX - x)
            val deltaY = Math.abs(lastY - y)
            val deltaZ = Math.abs(lastZ - z)

            if (deltaX > MOVEMENT_THRESHOLD || deltaY > MOVEMENT_THRESHOLD || deltaZ > MOVEMENT_THRESHOLD) {
                startAlarm()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
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
        val name = "Motion Detection Service"
        val descriptionText = "Monitors device motion"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, MotionDetectionService::class.java).apply {
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
            .setContentTitle("Motion Detection Active")
            .setContentText("Monitoring device motion")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
                 .build()
    }

    // BroadcastReceiver for handling screen lock/unlock events
    inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                // Device locked
                Log.d("MotionDetectionService", "Screen Locked")
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                // Device unlocked
                Log.d("MotionDetectionService", "Screen Unlocked")

                // Increment the unlock counter
                unlockCount++

                // Stop the alarm after the second unlock (without stopping the service)
                if (unlockCount %2==0) {
                    // Stop the alarm only, do not stop the service
                    stopAlarm()
                }
            }
        }
    }
}
