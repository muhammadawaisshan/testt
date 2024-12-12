package com.example.myapplication.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
 import kotlin.math.sqrt

class PocketRemovalService : Service(), SensorEventListener {

    private val isAboveAndroid9 =
        android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P
    private lateinit var sensorManager: SensorManager
    private val stepDetector: Sensor? = if (isAboveAndroid9) {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    } else sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var listener: StabilityListener? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var isStable: Boolean = true

    private lateinit var mediaPlayer: MediaPlayer
    private var job: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private val CHANNEL_ID = "PocketRemovalChannel"
    private val NOTIFICATION_ID = 1
    private val POCKET_THRESHOLD = 10f

    private var lastDistance = -1f
    private var pocketTime = System.currentTimeMillis()

    private val ACTION_STOP_SERVICE = "STOP_SERVICE"

    interface StabilityListener {
        fun onPhoneUnstable()
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mediaPlayer = MediaPlayer.create(this, R.raw.clock).apply {
            isLooping = true
        }
        if (stepDetector == null) {
            Toast.makeText(this, "No sensor found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        registerStabilityListener()
        startListening()

        return START_STICKY
    }

    private fun registerStabilityListener() {
        listener = object : StabilityListener {
            override fun onPhoneUnstable() {
                triggerAlarm()
            }
        }
    }

    private fun startListening() {
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    private fun triggerAlarm() {
        job?.cancel() // Cancel any ongoing alarms
        job = serviceScope.launch {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (!isAboveAndroid9) {
                val currentX: Float = event.values.getOrElse(0) { 0f }
                val currentY: Float = event.values.getOrElse(1) { 0f }
                val currentZ: Float = event.values.getOrElse(2) { 0f }

                val deltaX: Float = lastX - currentX
                val deltaY: Float = lastY - currentY
                val deltaZ: Float = lastZ - currentZ

                lastX = currentX
                lastY = currentY
                lastZ = currentZ

                val acceleration: Float =
                    sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
                isStable = acceleration >= THRESHOLD
            }

            if (isAboveAndroid9) {
                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    listener?.onPhoneUnstable()
                }
            } else {
                if (isStable) {
                    listener?.onPhoneUnstable()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
    }

    private fun createNotificationChannel() {
        val name = "Pocket Detection Service"
        val descriptionText = "Monitors device pocket removal"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PocketRemovalService::class.java).apply {
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
            .setContentTitle("Pocket Detection Active")
            .setContentText("Monitoring device pocket removal")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Service", stopPendingIntent).build()
    }

    companion object {
        private const val THRESHOLD = 1.5f
    }
}

