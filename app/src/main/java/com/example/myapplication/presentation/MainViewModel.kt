package com.example.myapplication.presentation

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.ServiceStatus
import com.example.myapplication.domain.model.ServiceType
import com.example.myapplication.domain.usecases.StartStopServiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val startStopServiceUseCase: StartStopServiceUseCase
) : ViewModel() {

    private val _serviceStatuses = MutableStateFlow(ServiceStatus())
    val serviceStatuses: StateFlow<ServiceStatus> = _serviceStatuses.asStateFlow()

    private val _countdownTime = MutableStateFlow(0)
    val countdownTime: StateFlow<Int> = _countdownTime.asStateFlow()

    private var countdownTimer: CountDownTimer? = null

    init {
        initializeServiceStatuses()
    }

    fun toggleService(serviceType: ServiceType) {
        viewModelScope.launch {
            when (serviceType) {
                ServiceType.POCKET_REMOVAL -> handlePocketRemovalToggle()
                else -> {
                    val newStatus = startStopServiceUseCase(serviceType)
                    updateServiceStatus(serviceType, newStatus)
                }
            }
        }
    }

    private fun handlePocketRemovalToggle() {
        val currentStatus = _serviceStatuses.value.isPocketRemovalActive
        if (currentStatus) {
            viewModelScope.launch {
                val newStatus = startStopServiceUseCase(ServiceType.POCKET_REMOVAL)
                updateServiceStatus(ServiceType.POCKET_REMOVAL, newStatus)
                _countdownTime.value = 0
                countdownTimer?.cancel()
            }
        } else {
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        viewModelScope.launch {
            val newStatus = startStopServiceUseCase(ServiceType.POCKET_REMOVAL)
            updateServiceStatus(ServiceType.POCKET_REMOVAL, newStatus)
        }
        _countdownTime.value = 10
        countdownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _countdownTime.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _countdownTime.value = 0

            }
        }.start()
    }

    private fun updateServiceStatus(serviceType: ServiceType, isActive: Boolean) {
        _serviceStatuses.update { currentStatus ->
            when (serviceType) {
                ServiceType.POCKET_REMOVAL -> currentStatus.copy(isPocketRemovalActive = isActive)
                ServiceType.CHARGER_REMOVAL -> currentStatus.copy(isChargerRemovalActive = isActive)
                ServiceType.MOTION_DETECTION -> currentStatus.copy(isMotionDetectionActive = isActive)
            }
        }
    }

    private fun initializeServiceStatuses() {
        viewModelScope.launch {
            val pocketRemovalStatus =
                startStopServiceUseCase.checkServiceStatus(ServiceType.POCKET_REMOVAL)
            val chargerRemovalStatus =
                startStopServiceUseCase.checkServiceStatus(ServiceType.CHARGER_REMOVAL)
            val motionDetectionStatus =
                startStopServiceUseCase.checkServiceStatus(ServiceType.MOTION_DETECTION)

            _serviceStatuses.value = ServiceStatus(
                isPocketRemovalActive = pocketRemovalStatus,
                isChargerRemovalActive = chargerRemovalStatus,
                isMotionDetectionActive = motionDetectionStatus
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownTimer?.cancel()
    }
}

