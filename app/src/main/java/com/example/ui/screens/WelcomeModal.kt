package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Helpers to read and write the "has seen welcome" state using SharedPreferences
fun hasSeenWelcome(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences("scanpro_welcome_prefs", Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean("scanpro_welcome_seen", false)
}

fun markWelcomeSeen(context: Context) {
    val sharedPrefs = context.getSharedPreferences("scanpro_welcome_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean("scanpro_welcome_seen", true).apply()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WelcomeModal(
    isOpen: Boolean,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }

    fun handleClose() {
        markWelcomeSeen(context)
        onClose()
    }

    fun handleNext() {
        if (step < 1) {
            step++
        } else {
            handleClose()
        }
    }

    Dialog(
        onDismissRequest = { handleClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = true, onClick = { handleClose() }),
            contentAlignment = Alignment.Center
        ) {
            // Modal Card container, width constraint max 400dp
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF111827) else Color.White)
                    .border(
                        1.dp,
                        if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFE5E7EB).copy(alpha = 0.5f),
                        RoundedCornerShape(28.dp)
                    )
                    .clickable(enabled = true, onClick = {}) // stop propagation of click to background backdrop
                    .testTag("welcome_modal_container")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Gradient Header with dynamic floating particles and bounce-in bounce animation
                    WelcomeGradientHeader()

                    // 2. Content section with steps
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width / 2 } + fadeOut()
                                } else {
                                    slideInHorizontally { width -> -width / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> width / 2 } + fadeOut()
                                }
                            },
                            label = "welcome_step_anim"
                        ) { currentStep ->
                            if (currentStep == 0) {
                                WelcomeStepZero()
                            } else {
                                WelcomeStepOne()
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress indicators (Two dots)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(2) { idx ->
                                val active = idx == step
                                val widthAnim by animateDpAsState(
                                    targetValue = if (active) 32.dp else 6.dp,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "dot_width"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(widthAnim)
                                        .clip(RoundedCornerShape(3.dp))
                                        .then(
                                            if (active) {
                                                Modifier.background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF14B8A6))))
                                            } else {
                                                Modifier.background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF4B5563) else Color(0xFFD1D5DB))
                                            }
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Footer Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { handleClose() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_welcome_skip"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                )
                            ) {
                                Text(
                                    text = "Skip",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Button(
                                onClick = { handleNext() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("btn_welcome_next"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp), // handle gradient padding
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF14B8A6))),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (step < 1) "Next" else "Get Started",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "ForwardArrow",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeGradientHeader() {
    val linearGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF10B981), // Emerald-500
            Color(0xFF14B8A6), // Teal-500
            Color(0xFF06B6D4)  // Cyan-500
        )
    )

    // Floating particles state/animation
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val p1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )

    // Bounce in animation for Logo
    val logoScale = remember { Animatable(0.2f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(linearGradient),
        contentAlignment = Alignment.Center
    ) {
        // Render Floating Particles in background using Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Particle 1
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = 12.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = w * 0.15f,
                    y = h * (0.8f - (p1 * 0.7f))
                )
            )

            // Particle 2
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = w * 0.45f,
                    y = h * (0.95f - (((p1 + 0.3f) % 1.0f) * 0.8f))
                )
            )

            // Particle 3
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = 16.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = w * 0.85f,
                    y = h * (0.75f - (((p1 + 0.6f) % 1.0f) * 0.7f))
                )
            )

            // Particle 4
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = w * 0.65f,
                    y = h * (0.9f - (((p1 + 0.15f) % 1.0f) * 0.85f))
                )
            )
        }

        // Overlay vignette gradient shading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                    )
                )
        )

        // Logo & Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(logoScale.value)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = "ScanPro Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "SCAN",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "PRO",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFD1FAE5), // Emerald-100 style
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 2.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp)) // Amber-100
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB45309) // Amber-700
                    )
                }
            }

            Text(
                text = "Document Scanner & Converter",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WelcomeStepZero() {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Text Title
        Text(
            text = "Welcome to SCANPRO AI!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Description Paragraph
        Text(
            text = "Your all-in-one AI-powered document scanner. Scan, convert, enhance, and manage documents — all from your device.",
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Grid of 4 features (2x2 Grid)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Camera,
                    label = "Camera Scan",
                    desc = "Auto-detect & scan",
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669))
                )
                FeatureItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DocumentScanner,
                    label = "OCR Text",
                    desc = "Extract text instantly",
                    gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Article,
                    label = "PDF Convert",
                    desc = "Image to PDF in 1-tap",
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                )
                FeatureItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Enhance",
                    desc = "Auto levels & contrast",
                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Free Credits Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    if (isDark) Color(0x2210B981) else Color(0xFFECFDF5),
                    RoundedCornerShape(50.dp)
                )
                .border(
                    1.dp,
                    if (isDark) Color(0x4410B981) else Color(0xFFA7F3D0).copy(alpha = 0.7f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Zap",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Get 50 FREE scans to start!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
            )
        }
    }
}

@Composable
private fun FeatureItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    desc: String,
    gradientColors: List<Color>
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = modifier
            .background(
                if (isDark) Color(0xFF1F2937) else Color(0xFFF9FAFB),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isDark) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFF3F4F6),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF374151)
        )

        Text(
            text = desc,
            fontSize = 9.sp,
            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF9CA3AF),
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun WelcomeStepOne() {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Guide text titles
        Text(
            text = "How It Works",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Three simple steps to scan anything",
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3 Simple steps (1, 2, 3)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GuideStepRow(
                number = "1",
                title = "Capture or Upload",
                desc = "Use your camera or upload an image",
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
            )

            GuideStepRow(
                number = "2",
                title = "AI Processes",
                desc = "Auto-enhance, OCR, or convert format",
                gradientColors = listOf(Color(0xFF14B8A6), Color(0xFF06B6D4))
            )

            GuideStepRow(
                number = "3",
                title = "Download & Share",
                desc = "Save as PDF, PNG, or copy text",
                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
            )
        }
    }
}

@Composable
private fun GuideStepRow(
    number: String,
    title: String,
    desc: String,
    gradientColors: List<Color>
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) Color(0xFF1F2937) else Color(0xFFF9FAFB),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isDark) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFF3F4F6),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF374151)
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
            )
        }
    }
}
