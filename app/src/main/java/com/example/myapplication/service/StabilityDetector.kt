package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast

import javax.inject.Inject
import kotlin.math.sqrt

class StabilityDetector @Inject constructor(private val context: Context) : SensorEventListener {

    val isAboveAndroid9 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector: Sensor? = if (isAboveAndroid9) {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    } else sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var listener: StabilityListener? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var isStable: Boolean = true

    interface StabilityListener {
        fun onPhoneUnstable()
    }

    fun setStabilityListener(listener: StabilityListener) {
        this.listener = listener
    }

    fun startListening() {
        if (stepDetector == null) {
            Toast.makeText(context, "no sensor was found...", Toast.LENGTH_SHORT).show()
        }
        sensorManager.registerListener(
            this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL,
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private var readingsCount = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isAboveAndroid9) {
            val currentX: Float = event?.values?.get(0) ?: 0f
            val currentY: Float = event?.values?.get(1) ?: 0f
            val currentZ: Float = event?.values?.get(2) ?: 0f

            val deltaX: Float = lastX - currentX
            val deltaY: Float = lastY - currentY
            val deltaZ: Float = lastZ - currentZ

            lastX = currentX
            lastY = currentY
            lastZ = currentZ

            val acceleration: Float =
                sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
            isStable = acceleration >= THRESHOLD
            if (readingsCount < 2) {
                readingsCount++
                return
            }
        }
        if (isAboveAndroid9) {
            if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
                listener?.onPhoneUnstable()
            }
        } else {
            if (!isStable) {
//            listener?.onPhoneStable()
            } else {
                listener?.onPhoneUnstable()
            }
        }
    }

    companion object {
        private const val THRESHOLD = 1.5f
        val maximumReportingDelay = 1 * 1000000 // 2 seconds in microseconds
    }
}