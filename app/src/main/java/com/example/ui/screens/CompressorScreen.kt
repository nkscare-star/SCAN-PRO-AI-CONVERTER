package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DocumentPage
import com.example.data.models.RecentConversion
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorScreen(
    page: DocumentPage,
    repository: DocumentRepository,
    onBack: () -> Unit,
    onCompressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var quality by remember { mutableStateOf(60f) }
    var originalSize by remember { mutableStateOf(0L) }
    var compressedSize by remember { mutableStateOf(0L) }
    var isCompressing by remember { mutableStateOf(false) }

    LaunchedEffect(page) {
        val oFile = File(page.originalImagePath)
        if (oFile.exists()) {
            originalSize = oFile.length()
        }
        val cFile = File(page.imagePath)
        if (cFile.exists()) {
            compressedSize = cFile.length()
        }
    }

    val df = DecimalFormat("#.##")
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB")
        var size = bytes.toDouble()
        var unitIdx = 0
        while (size > 1024 && unitIdx < units.lastIndex) {
            size /= 1024
            unitIdx++
        }
        return "${df.format(size)} ${units[unitIdx]}"
    }

    val compressRatio = if (originalSize > 0) {
        ((originalSize - compressedSize).toDouble() / originalSize * 100).coerceAtLeast(0.0)
    } else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Size Compressor") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("compressor_back")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Optimize Document Size",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Slide to adjust JPEG compression factor. High compression saves substantial space while maintaining text readability.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Image preview inside frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(page.imagePath) {
                        BitmapFactory.decodeFile(page.imagePath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Preview Scanned Document",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.PhotoSizeSelectLarge, "Preview Unavailable", modifier = Modifier.padding(32.dp))
                    }
                }

                // Comparison Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Original", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(formatBytes(originalSize), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Optimized", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(formatBytes(compressedSize), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                if (compressRatio > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.OfflineBolt, "Ratio", tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                "AI Compressor reduced storage size by ${df.format(compressRatio)}%!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Slider control
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compression Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${quality.toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }

                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 10f..95f,
                        onValueChangeFinished = {
                            if (!isCompressing) {
                                isCompressing = true
                                scope.launch {
                                    compressedSize = repository.compressPageJpeg(page, quality.toInt())
                                    isCompressing = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("compression_slider")
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        repository.insertRecentConversion(
                            RecentConversion(
                                id = java.util.UUID.randomUUID().toString(),
                                name = "Comp_" + page.id.take(6),
                                timestamp = System.currentTimeMillis(),
                                type = "compress",
                                format = "JPG"
                            )
                        )
                        onCompressed()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_compression_btn")
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Done, "Apply")
                Text("Confirm Optimization", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
