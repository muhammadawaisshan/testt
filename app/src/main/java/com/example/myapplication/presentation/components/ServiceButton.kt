package com.example.myapplication.presentation.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
@Composable
fun ServiceButton(
    text: String,
    isActive: Boolean,
    countdownTime: Int = 0,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onClick()
            } else {
                Toast.makeText(
                    context, "Grant Notification Permission to continue", Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    val buttonColor = when {
        text == "Pocket Removal" && countdownTime > 0 -> Color.Yellow.copy(alpha = 0.5f) // Countdown phase
        isActive -> Color.Green
        else -> Color.Red
    }

    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onClick()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        )
    ) {
        val displayText = when {
            text == "Pocket Removal" && countdownTime > 0 -> "$text: Put phone in pocket in $countdownTime seconds"
            text == "Pocket Removal" && !isActive -> "$text: Inactive"
            else -> "$text: ${if (isActive) "Active" else "Inactive"}"
        }

        Text(text = displayText)
    }
}