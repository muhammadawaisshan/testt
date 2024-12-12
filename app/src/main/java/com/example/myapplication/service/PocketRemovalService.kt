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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.presentation.MainActivity

class PocketRemovalService : Service(), SensorEventListener {
    var unlockCount = 0
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var lastProximityReading: Float = -1f
    private lateinit var mediaPlayer: MediaPlayer
    private var isPhoneInPocket = false
    private var isAlarmTriggered = false

    private val ACTION_STOP_SERVICE = "STOP_SERVICE"
    private val CHANNEL_ID = "PocketRemovalChannel"
    private val NOTIFICATION_ID = 1
    private val POCKET_THRESHOLD = 5f
    private val POCKET_EXIT_THRESHOLD = 3f

    private lateinit var screenReceiver: ScreenReceiver

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mediaPlayer = MediaPlayer.create(this, R.raw.clock)
        mediaPlayer.isLooping = true

        screenReceiver = ScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        proximitySensor?.let { sensor ->
            Handler(Looper.getMainLooper()).postDelayed({
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                accelerometerSensor?.let {
                    sensorManager.registerListener(
                        this, it, SensorManager.SENSOR_DELAY_UI
                    )
                }
            }, 10000)
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                screenReceiver, screenFilter
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        mediaPlayer.release()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val distance = it.values[0]
                    if (distance < POCKET_THRESHOLD) {
                        if (!isPhoneInPocket) {
                            isPhoneInPocket = true
                            triggerAlarm()
                        }
                    } else if (distance > POCKET_EXIT_THRESHOLD) {
                        if (isPhoneInPocket) {
                            isPhoneInPocket = false
                            stopAlarm()
                        }
                    }
                    lastProximityReading = distance
                }

                Sensor.TYPE_ACCELEROMETER -> {
                    // Accelerometer-based motion detection can be added here
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerAlarm() {
        if (!isAlarmTriggered) {
            isAlarmTriggered = true
            mediaPlayer.start()
            mediaPlayer.isLooping = true
        }
    }

    private fun stopAlarm() {
        if (isAlarmTriggered) {
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
            isAlarmTriggered = false
        }
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
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_launcher_background, "Stop Service", stopPendingIntent).build()
    }

    inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                Log.d("PocketRemovalService", "Screen Unlocked")



                 unlockCount++

                if ( unlockCount % 2 == 0) {

                     stopAlarm()
                }
            }
        }
    }


}