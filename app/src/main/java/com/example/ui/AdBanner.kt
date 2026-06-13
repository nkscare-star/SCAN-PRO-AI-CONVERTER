package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val topAds = listOf(
    "🚀 SCAN PRO AI — Convert, Scan & Enhance Documents Instantly!",
    "📸 Auto Camera Scan — AI Detects Document Edges Automatically!",
    "📄 Image to PDF in One Tap — No Watermark, 100% Free!",
    "🔍 OCR Text Extraction — Pull Text from Any Image!",
    "⚡ 12+ AI-Powered Tools — Scan, Convert, Compress, Sign!"
)

val bottomAds = listOf(
    "📱 Works on Phone & Desktop — Scan Anywhere, Anytime!",
    "🔒 Your Files Stay Private — Nothing Uploaded to Servers",
    "✨ First 50 Scans FREE — Premium Just ₹100 for 100 More!",
    "🎨 AI Enhance, Background Remove, QR Scanner — All Built In!"
)

enum class AdPosition { TOP, BOTTOM }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdBanner(
    position: AdPosition,
    modifier: Modifier = Modifier
) {
    val ads = if (position == AdPosition.TOP) topAds else bottomAds
    
    // Auto-cycle ads every 6.5 seconds for interactive realistic feel!
    var adIndex by remember { mutableStateOf((0..ads.lastIndex).random()) }
    
    LaunchedEffect(key1 = position) {
        while (true) {
            delay(6500)
            adIndex = (adIndex + 1) % ads.size
        }
    }

    val currentAd = ads[adIndex]

    // Create a beautiful premium ad banner frame matching gradient and look precisely
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_banner_${position.name.lowercase()}")
            .background(backgroundBrush, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag "Ad" corresponding exactly to: text-[8px] uppercase font-black tracking-wider px-1.5 py-0.5 rounded
        Text(
            text = "Ad",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(4.dp)
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )

        // Text transitioning automatically with animated sliding
        AnimatedContent(
            targetState = currentAd,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn() with
                        slideOutVertically { height -> -height } + fadeOut())
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) { targetAd ->
            Text(
                text = targetAd,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
