package com.example.myapplication.domain.usecases

import android.content.Context
import android.content.Intent
import com.example.myapplication.domain.model.ServiceType
import com.example.myapplication.service.ChargerRemovalService
import com.example.myapplication.service.MotionDetectionService
import com.example.myapplication.service.PocketRemovalService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StartStopServiceUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(serviceType: ServiceType): Boolean {
        val serviceClass = when (serviceType) {
            ServiceType.POCKET_REMOVAL -> PocketRemovalService::class.java
            ServiceType.CHARGER_REMOVAL -> ChargerRemovalService::class.java
            ServiceType.MOTION_DETECTION -> MotionDetectionService::class.java
        }

        return if (isServiceRunning(serviceClass)) {
            stopService(serviceClass)
            false
        } else {
            startService(serviceClass)
            true
        }
    }

    fun checkServiceStatus(serviceType: ServiceType): Boolean {
        val serviceClass = when (serviceType) {
            ServiceType.POCKET_REMOVAL -> PocketRemovalService::class.java
            ServiceType.CHARGER_REMOVAL -> ChargerRemovalService::class.java
            ServiceType.MOTION_DETECTION -> MotionDetectionService::class.java
        }
        return isServiceRunning(serviceClass)
    }

    private fun startService(serviceClass: Class<*>) {
        Intent(context, serviceClass).also { intent ->
            context.startForegroundService(intent)
        }
    }

    private fun stopService(serviceClass: Class<*>) {
        Intent(context, serviceClass).also { intent ->
            context.stopService(intent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
