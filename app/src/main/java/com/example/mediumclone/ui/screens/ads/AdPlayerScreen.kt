package com.example.mediumclone.ui.screens.ads

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun AdPlayerScreen(
    onAdComplete: () -> Unit
) {
    // Prevent back press
    Dialog(
        onDismissRequest = { /* Do nothing - Unskippable */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            var timeLeft by remember { mutableIntStateOf(10) } // 10 seconds mock ad
            var adFinished by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                while (timeLeft > 0) {
                    delay(1000)
                    timeLeft--
                }
                adFinished = true
            }

            // Mock Ad Content (Simulating a Video)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp, 200.dp)
                        .background(Color.DarkGray)
                        .border(1.dp, Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Super Cool App Video Ad", color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Download the Best App Ever!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Simulated Click */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Install Now")
                }
            }

            // Timer / Close Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                if (!adFinished) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reward in ${timeLeft}s",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    IconButton(
                        onClick = onAdComplete,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
            
            // Branding
            Text(
                text = "Ad by Google-ish",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
