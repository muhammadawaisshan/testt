package com.example.myapplication.domain.model

// ServiceStatus.kt
data class ServiceStatus(
    val isPocketRemovalActive: Boolean = false,
    val isChargerRemovalActive: Boolean = false,
    val isMotionDetectionActive: Boolean = false
)

// ServiceType.kt
enum class ServiceType {
    POCKET_REMOVAL,
    CHARGER_REMOVAL,
    MOTION_DETECTION
}