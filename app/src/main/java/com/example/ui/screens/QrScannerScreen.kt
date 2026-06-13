package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    // Scanned result simulated laser animation
    var scanLinePosition by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (isScanning) {
            delay(10)
            scanLinePosition = (scanLinePosition + 0.015f) % 1.0f
        }
    }

    // Trigger scanning results mock
    LaunchedEffect(Unit) {
        delay(2600)
        isScanning = false
        val mockResults = listOf(
            "https://ai.studio/build",
            "https://google.com",
            "WIFI:S:MyNetwork;T:WPA;P:SuperSecret123;;",
            "HTTPS://NKSCARE.GMAIL.COM/REVENUE",
            "TEL:1234567890"
        )
        val result = mockResults.random()
        scannedResult = result
        onQrScanned(result)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M3 QR Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back_btn")) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Point Camera at QR Code",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Align the code within the frame to scan. Our advanced neural decoder extracts parameters instantly.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                // Camera Scan View Frame
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val heightPx = constraints.maxHeight.toFloat()
                    
                    // Simulated matrix grids
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        // Scan animated laser line
                        if (isScanning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = (240.dp * scanLinePosition))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanned",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            // Results summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isScanning) "SCANNING CODE TYPE..." else "DECODED SUCCESSFULLY!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isScanning) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = scannedResult ?: "Point at a QR / Barcode tag...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_complete_btn")
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isScanning) "Cancel" else "Dismiss Scanner")
            }
        }
    }
}
