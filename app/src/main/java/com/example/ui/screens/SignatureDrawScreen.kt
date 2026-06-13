package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignatureDrawScreen(
    onSignatureSaved: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Draw Your Signature",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sign on the tablet/phone screen below. This will be converted to a transparent overlay for your scanned papers.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Drawing viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath.add(offset)
                            },
                            onDrag = { _, dragAmount ->
                                if (currentPath.isNotEmpty()) {
                                    val lastPoint = currentPath.last()
                                    currentPath.add(lastPoint + dragAmount)
                                }
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath.toList())
                                    currentPath.clear()
                                }
                            }
                        )
                    }
                    .testTag("signature_canvas")
            ) {
                // Drawing rendering
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    // Render historical strokes
                    paths.forEach { stroke ->
                        if (stroke.size > 1) {
                            for (i in 0 until stroke.size - 1) {
                                drawLine(
                                    color = Color.Black,
                                    start = stroke[i],
                                    end = stroke[i + 1],
                                    strokeWidth = 6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                    // Render active stroke
                    if (currentPath.size > 1) {
                        for (i in 0 until currentPath.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = currentPath[i],
                                end = currentPath[i + 1],
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    if (paths.isEmpty() && currentPath.isEmpty()) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "Sign Here",
                            size.width / 2f - 100f,
                            size.height / 2f,
                            Paint().apply {
                                color = android.graphics.Color.LTGRAY
                                textSize = 48f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }

        // Dashboard buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPath.clear()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_signature_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
                Text("Clear", modifier = Modifier.padding(start = 6.dp))
            }

            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        // Generate bitmap of standard drawn bounds or whole canvas
                        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.TRANSPARENT)

                        val p = Paint().apply {
                            color = android.graphics.Color.BLACK
                            strokeWidth = 8f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            isAntiAlias = true
                        }

                        // Determine bounds of signature to crop tightly for nice overlay positioning!
                        var minX = Float.MAX_VALUE
                        var maxX = Float.MIN_VALUE
                        var minY = Float.MAX_VALUE
                        var maxY = Float.MIN_VALUE

                        paths.forEach { stroke ->
                            stroke.forEach { point ->
                                if (point.x < minX) minX = point.x
                                if (point.x > maxX) maxX = point.x
                                if (point.y < minY) minY = point.y
                                if (point.y > maxY) maxY = point.y
                            }
                        }

                        // Fallback bounds
                        if (minX == Float.MAX_VALUE) { minX = 0f; maxX = 800f; minY = 0f; maxY = 400f }
                        val width = (maxX - minX + 80f).coerceAtLeast(100f).coerceAtMost(2000f).toInt()
                        val height = (maxY - minY + 80f).coerceAtLeast(100f).coerceAtMost(2000f).toInt()

                        val croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val croppedCanvas = Canvas(croppedBitmap)
                        croppedCanvas.drawColor(android.graphics.Color.TRANSPARENT)

                        paths.forEach { stroke ->
                            if (stroke.size > 1) {
                                val path = Path()
                                path.moveTo(stroke[0].x - minX + 40f, stroke[0].y - minY + 40f)
                                for (i in 1 until stroke.size) {
                                    path.lineTo(stroke[i].x - minX + 40f, stroke[i].y - minY + 40f)
                                }
                                croppedCanvas.drawPath(path, p)
                            }
                        }

                        onSignatureSaved(croppedBitmap)
                    }
                },
                enabled = paths.isNotEmpty(),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("save_signature_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = "Save signature")
                Text("Apply Signature", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
