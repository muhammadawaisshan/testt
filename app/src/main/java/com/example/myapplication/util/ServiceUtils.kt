package com.example.myapplication.util


import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService

object ServiceUtils {
    fun isMyServiceRunning(serviceClass: Class<*>,context: Context): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}