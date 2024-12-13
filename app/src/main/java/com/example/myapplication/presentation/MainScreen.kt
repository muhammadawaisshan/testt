package com.example.myapplication.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.domain.model.ServiceType
import com.example.myapplication.presentation.components.ServiceButton
import java.util.jar.Manifest

@Composable
fun MainScreen(modifier: Modifier,viewModel: MainViewModel = hiltViewModel()) {
    val serviceStatuses by viewModel.serviceStatuses.collectAsStateWithLifecycle()
    val countdownTime by viewModel.countdownTime.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {

        ServiceButton(
            text = "Charger Removal",
            isActive = serviceStatuses.isChargerRemovalActive,
            onClick = { viewModel.toggleService(ServiceType.CHARGER_REMOVAL) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServiceButton(
            text = "Motion Detection",
            isActive = serviceStatuses.isMotionDetectionActive,
            onClick = { viewModel.toggleService(ServiceType.MOTION_DETECTION) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ServiceButton(
            text = "Pocket Removal",
            isActive = serviceStatuses.isPocketRemovalActive,countdownTime = countdownTime,
            onClick = { viewModel.toggleService(ServiceType.POCKET_REMOVAL) }
        )
    }
}
