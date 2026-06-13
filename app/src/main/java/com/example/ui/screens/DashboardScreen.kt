package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion
import com.example.data.repository.DocumentRepository
import com.example.ui.AdBanner
import com.example.ui.AdPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    documents: List<ScannedDocument>,
    recentConversions: List<RecentConversion>,
    repository: DocumentRepository,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (ScannedDocument) -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToSignature: () -> Unit,
    appStore: com.example.data.repository.AppStore
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isPremiumUser by appStore.isPremium.collectAsState()
    val scansUsedCount by appStore.usageCount.collectAsState()
    val premiumRemaining by appStore.premiumRemaining.collectAsState()

    // Cloud Sync States
    val isGoogleDriveConnected by appStore.isGoogleDriveConnected.collectAsState()
    val googleDriveEmail by appStore.googleDriveEmail.collectAsState()
    val isDropboxConnected by appStore.isDropboxConnected.collectAsState()
    val dropboxAccount by appStore.dropboxAccount.collectAsState()
    val autoUploadEnabled by appStore.autoUploadEnabled.collectAsState()
    val syncHistory by appStore.syncHistory.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val existing = repository.allRecentConversions.first()
            if (existing.isEmpty()) {
                repository.insertRecentConversion(
                    RecentConversion(
                        id = "mock1",
                        name = "Invoice_Starbucks.pdf",
                        timestamp = System.currentTimeMillis() - 4 * 60 * 1000,
                        type = "image-to-pdf",
                        format = "PDF"
                    ),
                    incrementCount = false
                )
                repository.insertRecentConversion(
                    RecentConversion(
                        id = "mock2",
                        name = "Receipt_Uber_Ride.jpg",
                        timestamp = System.currentTimeMillis() - 120 * 60 * 1000,
                        type = "camera-scan",
                        format = "JPG"
                    ),
                    incrementCount = false
                )
                repository.insertRecentConversion(
                    RecentConversion(
                        id = "mock3",
                        name = "Contract_Signed_v2.png",
                        timestamp = System.currentTimeMillis() - 12 * 3600 * 1000,
                        type = "signature",
                        format = "PNG"
                    ),
                    incrementCount = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Quick explanation popup state for 12+ premium tools
    var popupToolName by remember { mutableStateOf("") }
    var popupToolDetail by remember { mutableStateOf("") }
    var showPopup by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPremiumModal by remember { mutableStateOf(false) }
    var showWelcomeModal by remember { mutableStateOf<Boolean>(!hasSeenWelcome(context)) }

    // Cloud Sync Dialog states
    var showGoogleDriveLinkDialog by remember { mutableStateOf(false) }
    var showDropboxLinkDialog by remember { mutableStateOf(false) }
    var selectedRecentConversionPreview by remember { mutableStateOf<RecentConversion?>(null) }

    val dfDate = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Logo + Animated Name representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pulsing logo icon box with gorgeous emerald-to-teal-to-cyan gradient background
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF06B6D4))
                                    )
                                )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                )
                                // Pulsing/Blinking tiny yellow active dot indicator at top right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (1).dp, y = (-1).dp)
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0xFFFBBF24))
                                        .border(1.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }

                        // App name & branding text column with horizontal brush styling
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCAN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "PRO",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF06B6D4))
                                        )
                                    ),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                                Text(
                                    text = "AI",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEF4444))
                                        )
                                    ),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Text(
                                text = "CONVERTER",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            Color(0xFF34D399),
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                            )
                        }
                    }

                    // Right side controls row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Remaining credits/limit status badge
                        val remaining = if (isPremiumUser) premiumRemaining else (50 - scansUsedCount).coerceAtLeast(0)
                        val badgeBg = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFECFDF5)
                        val badgeBorder = if (isDarkTheme) Color(0xFF059669) else Color(0xFFA7F3D0)
                        val badgeTextColor = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, RoundedCornerShape(16.dp))
                                .clickable { showPremiumModal = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isPremiumUser) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "PRO",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(
                                text = "$remaining ${if (isPremiumUser) "credits" else "free"}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }

                        // Theme Mode dynamic toggle action button
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = "Toggle Dark Mode",
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings action trigger button with popup handler
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (appStore.canUse()) {
                        onNavigateToCamera()
                    } else {
                        showPremiumModal = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("fab_trigger_scan")
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, "Auto Scan Document")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // 1. Top AdBanner
            item {
                AdBanner(
                    position = AdPosition.TOP,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 2. Interactive Premium "Export Wallet" Card Component
            item {
                val maxFreeUsage = 50
                val remaining = if (isPremiumUser) premiumRemaining else (maxFreeUsage - scansUsedCount).coerceAtLeast(0)
                val total = if (isPremiumUser) 100 else maxFreeUsage
                val pct = if (isPremiumUser) {
                    (((100 - premiumRemaining).toFloat() / 100f) * 100f).coerceAtLeast(2f)
                } else {
                    ((scansUsedCount.toFloat() / maxFreeUsage.toFloat()) * 100f).coerceAtLeast(2f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981).copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Row 1: Left (Icon & Wallet details), Right (PRO star tag badge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Beautiful emerald-to-teal gradient sphere background
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF10B981),
                                                        Color(0xFF14B8A6)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wallet,
                                            contentDescription = "Wallet",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Your Export Wallet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isPremiumUser) "Premium Member" else "Free Tier — Upgrade for more",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (isPremiumUser) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "PRO",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "PRO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFD97706),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            // Row 2: Big remaining count stats indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$remaining",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981),
                                        lineHeight = 44.sp
                                    )
                                    Column(
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "/ $total",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = if (isPremiumUser) "Credits Left" else "Free Exports",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.outline,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Trend",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Row 3: Progress slider beautifully styled matching emerald-teal-cyan theme
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct / 100f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF10B981),
                                                    Color(0xFF14B8A6),
                                                    Color(0xFF06B6D4)
                                                )
                                            )
                                        )
                                )
                            }

                            // Row 4: Customized premium upgrade action CTA trigger button
                            Button(
                                onClick = {
                                    showPremiumModal = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF059669),
                                                    Color(0xFF0D9488),
                                                    Color(0xFF0891B2)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OfflineBolt,
                                            contentDescription = "Upgrade",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isPremiumUser) "Refill Balance — 100 More Credits for ₹100" else "Upgrade Now — 100 Ad-Free Credits for ₹100",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Grid of 12+ AI-Powered Tools (High fidelity FeatureGrid)
            item {
                val isDark = isSystemInDarkTheme()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Title Header: Beautifully aligned with Sparkles and tools count badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Sparkles",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ALL FEATURES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "12 tools",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Define the list of 12 tools matching the React FeatureGrid exactly
                    val toolsList = listOf(
                        ToolItem(
                            id = "camera-scan",
                            label = "Camera Scan",
                            desc = "Auto-detect & scan docs",
                            icon = Icons.Default.Camera,
                            gradient = listOf(Color(0xFF10B981), Color(0xFF14B8A6)), // emerald-500 to teal-500
                            lightBg = Color(0xFFECFDF5),
                            darkBg = Color(0xFF10B981).copy(alpha = 0.12f),
                            badge = "AUTO",
                            badgeColor = Color(0xFF10B981),
                            action = onNavigateToCamera
                        ),
                        ToolItem(
                            id = "ocr",
                            label = "OCR Text Scan",
                            desc = "Extract text from any image",
                            icon = Icons.Default.DocumentScanner,
                            gradient = listOf(Color(0xFF3B82F6), Color(0xFF6366F1)), // blue-500 to indigo-500
                            lightBg = Color(0xFFEFF6FF),
                            darkBg = Color(0xFF3B82F6).copy(alpha = 0.12f),
                            badge = "AI",
                            badgeColor = Color(0xFF3B82F6),
                            action = null
                        ),
                        ToolItem(
                            id = "image-to-pdf",
                            label = "Image → PDF",
                            desc = "JPG, PNG to PDF document",
                            icon = Icons.Default.PictureAsPdf,
                            gradient = listOf(Color(0xFFF97316), Color(0xFFEF4444)), // orange-500 to red-500
                            lightBg = Color(0xFFFFF7ED),
                            darkBg = Color(0xFFF97316).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = null
                        ),
                        ToolItem(
                            id = "pdf-to-image",
                            label = "PDF → Image",
                            desc = "Extract pages as images",
                            icon = Icons.Default.PhotoLibrary,
                            gradient = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7)), // violet-500 to purple-500
                            lightBg = Color(0xFFF5F3FF),
                            darkBg = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = null
                        ),
                        ToolItem(
                            id = "format-convert",
                            label = "Format Convert",
                            desc = "JPEG ↔ PNG ↔ WEBP",
                            icon = Icons.Default.SwapHoriz,
                            gradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), // cyan-500 to blue-500
                            lightBg = Color(0xFFECFEFF),
                            darkBg = Color(0xFF06B6D4).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = null
                        ),
                        ToolItem(
                            id = "qr-scanner",
                            label = "QR / Barcode",
                            desc = "Scan any QR or barcode",
                            icon = Icons.Default.QrCodeScanner,
                            gradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // pink-500 to rose-500
                            lightBg = Color(0xFFFDF2F8),
                            darkBg = Color(0xFFEC4899).copy(alpha = 0.12f),
                            badge = "NEW",
                            badgeColor = Color(0xFFEC4899),
                            action = onNavigateToQr
                        ),
                        ToolItem(
                            id = "enhance",
                            label = "AI Enhance",
                            desc = "Auto brightness & contrast",
                            icon = Icons.Default.AutoFixHigh,
                            gradient = listOf(Color(0xFFF59E0B), Color(0xFFF97316)), // amber-500 to orange-500
                            lightBg = Color(0xFFFFFBEB),
                            darkBg = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            badge = "AI",
                            badgeColor = Color(0xFFF59E0B),
                            action = null
                        ),
                        ToolItem(
                            id = "compress",
                            label = "Compress",
                            desc = "Reduce file size smartly",
                            icon = Icons.Default.Compress,
                            gradient = listOf(Color(0xFF14B8A6), Color(0xFF10B981)), // teal-500 to emerald-500
                            lightBg = Color(0xFFF0FDFA),
                            darkBg = Color(0xFF14B8A6).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = null
                        ),
                        ToolItem(
                            id = "signature",
                            label = "Sign Document",
                            desc = "Draw & add signature",
                            icon = Icons.Default.Draw,
                            gradient = listOf(Color(0xFFF43F5E), Color(0xFFEC4899)), // rose-500 to pink-500
                            lightBg = Color(0xFFFFF1F2),
                            darkBg = Color(0xFFF43F5E).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = onNavigateToSignature
                        ),
                        ToolItem(
                            id = "batch",
                            label = "Batch Process",
                            desc = "Convert multiple files",
                            icon = Icons.Default.Layers,
                            gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), // indigo-500 to violet-500
                            lightBg = Color(0xFFEEF2FF),
                            darkBg = Color(0xFF6366F1).copy(alpha = 0.12f),
                            badge = "PRO",
                            badgeColor = Color(0xFF6366F1),
                            action = null
                        ),
                        ToolItem(
                            id = "background-remove",
                            label = "BG Remove",
                            desc = "Remove image background",
                            icon = Icons.Default.Wallpaper,
                            gradient = listOf(Color(0xFFD946EF), Color(0xFFA855F7)), // fuchsia-500 to purple-500
                            lightBg = Color(0xFFFDF4FF),
                            darkBg = Color(0xFFD946EF).copy(alpha = 0.12f),
                            badge = "AI",
                            badgeColor = Color(0xFFD946EF),
                            action = null
                        ),
                        ToolItem(
                            id = "text-to-image",
                            label = "Text Overlay",
                            desc = "Add text to images",
                            icon = Icons.Default.TextFields,
                            gradient = listOf(Color(0xFF0EA5E9), Color(0xFF3B82F6)), // sky-500 to blue-500
                            lightBg = Color(0xFFF0F9FF),
                            darkBg = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                            badge = null,
                            badgeColor = Color.Transparent,
                            action = null
                        )
                    )

                    // Render them compactly in a beautiful, adaptive 3-column grid
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in toolsList.indices step 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (j in i until i + 3) {
                                    if (j < toolsList.size) {
                                        val tool = toolsList[j]
                                        val itemBg = if (isDark) tool.darkBg else tool.lightBg
                                        
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.82f)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .clickable {
                                                    if (!appStore.canUse()) {
                                                        showPremiumModal = true
                                                    } else {
                                                        if (tool.action != null) {
                                                            tool.action.invoke()
                                                        } else {
                                                            popupToolName = tool.label
                                                            popupToolDetail = tool.desc
                                                            showPopup = true
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = itemBg)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                // 1. Tool Badge (if available, e.g., AUTO, AI, NEW, PRO)
                                                if (tool.badge != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(top = 8.dp, end = 8.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(tool.badgeColor)
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = tool.badge,
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                    }
                                                }

                                                // 2. Main Content
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    // High-contrast gradient sphere container for Lucide-inspired icon representation
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(
                                                                Brush.linearGradient(
                                                                    colors = tool.gradient
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = tool.icon,
                                                            contentDescription = tool.label,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Text(
                                                        text = tool.label,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(3.dp))

                                                    Text(
                                                        text = tool.desc,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        lineHeight = 11.sp,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Scans section list (Matches the React code CSS/colors/design principles completely!)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Recent",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECENT SCANS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    if (recentConversions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSystemInDarkTheme()) Color(0xFF374151) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = recentConversions.size.toString(),
                                fontSize = 10.sp,
                                color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (recentConversions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1F2937) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSystemInDarkTheme()) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFE5E7EB).copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Empty",
                                tint = if (isSystemInDarkTheme()) Color(0xFF374151) else Color(0xFFE5E7EB),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No scans yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select a tool above to get started",
                                fontSize = 12.sp,
                                color = if (isSystemInDarkTheme()) Color(0xFF4B5563) else Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1F2937) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSystemInDarkTheme()) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFE5E7EB).copy(alpha = 0.5f)
                        )
                    ) {
                        Column {
                            recentConversions.take(8).forEachIndexed { index, conv ->
                                val iconDetails = getIconForConversionType(conv.type)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRecentConversionPreview = conv
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("recent_scan_row_$index"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Rounded container for Icon
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(iconDetails.bgColor, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconDetails.icon,
                                                contentDescription = conv.type,
                                                tint = iconDetails.tintColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = conv.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSystemInDarkTheme()) Color(0xFFF3F4F6) else Color(0xFF374151),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = timeAgo(conv.timestamp),
                                                fontSize = 11.sp,
                                                color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF9CA3AF)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Format Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSystemInDarkTheme()) Color(0xFF374151) else Color(0xFFF3F4F6),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = conv.format.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                            )
                                        }

                                        // Share button
                                        IconButton(
                                            onClick = {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Check out my converted file: " + conv.name)
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF9CA3AF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < minOf(recentConversions.size, 8) - 1) {
                                    HorizontalDivider(
                                        color = if (isSystemInDarkTheme()) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFE5E7EB).copy(alpha = 0.5f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Saved Documents Title
            item {
                Text(
                    text = "Your Scans Library",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 5. Scans list
            if (documents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Scanned Documents",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "Tap the float action button below to scan your invoice, bills, receipts, or notes with high-contrast AI edge detection!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)
                        )
                    }
                }
            } else {
                items(documents) { doc ->
                    var pagesCount by remember { mutableStateOf(0) }
                    LaunchedEffect(doc) {
                        pagesCount = repository.getPages(doc.id).size
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("document_card_${doc.id}")
                            .clickable { onNavigateToDetail(doc) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Folder / Document icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = "Doc",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text(
                                        text = doc.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                "$pagesCount ${if (pagesCount == 1) "Page" else "Pages"}",
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = dfDate.format(Date(doc.createdAt)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Open",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 6. Bottom AdBanner
            item {
                AdBanner(
                    position = AdPosition.BOTTOM,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // 7. Premium High-Fidelity Responsive Footer Block
            item {
                val isDark = isSystemInDarkTheme()
                val footerBg = if (isDark) Color(0xFF000000) else Color(0xFF111827)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = footerBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo block
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF10B981),
                                                    Color(0xFF14B8A6)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = "ScanPro AI Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Row {
                                    Text(
                                        text = "SCAN",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "PRO",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF34D399),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = " AI",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Text details
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "© 2026 ScanPro AI Converter • Made with ",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Love",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "50 free scans/month • Premium ₹100 for 100 ad-free credits",
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }

    // Tools explanation dialog popup
    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, "Tool Spec", tint = MaterialTheme.colorScheme.primary)
                    Text(popupToolName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    popupToolDetail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPopup = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // High fidelity premium Settings dialog popup matching header requirements
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.primary)
                    Text("Workspace & Sync Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Manage scan parameters, premium workspace billing, and configure automated Google Drive & Dropbox synchronizations.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // --- SECTION 1: SYSTEM & BILLING ---
                    Text("Workspace Billing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Premium Workspaces", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isPremiumUser) "PRO ACTIVE" else "FREE ACCOUNT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPremiumUser) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Credits Remaining", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isPremiumUser) "$premiumRemaining Pro pages" else "$scansUsedCount / 50 scans used",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // --- SECTION 2: AUTOMATIC CLOUD SYNC SETTING ---
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Upload Scans", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Instantly sync compiled PDFs to active targets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoUploadEnabled,
                            onCheckedChange = { appStore.setAutoUploadEnabled(it) },
                            modifier = Modifier.testTag("switch_auto_upload")
                        )
                    }

                    // --- SECTION 3: CLOUD STORAGE PROVIDERS ---
                    Text("Cloud Targets", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Card for Google Drive
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp, 
                                if (isGoogleDriveConnected) Color(0xFF10B981).copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGoogleDriveConnected) Color(0xFF10B981).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "G-Drive",
                                    tint = if (isGoogleDriveConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text("Google Drive", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        googleDriveEmail ?: "Account disconnected",
                                        fontSize = 11.sp,
                                        color = if (isGoogleDriveConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    if (isGoogleDriveConnected) {
                                        appStore.disconnectGoogleDrive()
                                    } else {
                                        showGoogleDriveLinkDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGoogleDriveConnected) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isGoogleDriveConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_gdrive_sync_toggle")
                            ) {
                                Text(if (isGoogleDriveConnected) "Disconnect" else "Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Card for Dropbox
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp, 
                                if (isDropboxConnected) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDropboxConnected) Color(0xFF3B82F6).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Dropbox",
                                    tint = if (isDropboxConnected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text("Dropbox Sync", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        dropboxAccount ?: "Account disconnected",
                                        fontSize = 11.sp,
                                        color = if (isDropboxConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    if (isDropboxConnected) {
                                        appStore.disconnectDropbox()
                                    } else {
                                        showDropboxLinkDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDropboxConnected) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isDropboxConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_dropbox_sync_toggle")
                            ) {
                                Text(if (isDropboxConnected) "Disconnect" else "Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- SECTION 4: BACKUP ALL NOW BUTTON ---
                    if (isGoogleDriveConnected || isDropboxConnected) {
                        var backingUpAll by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (!backingUpAll && documents.isNotEmpty()) {
                                    backingUpAll = true
                                    scope.launch {
                                        try {
                                            for (doc in documents) {
                                                val file = repository.compileDocumentToPdf(doc.id)
                                                if (file != null) {
                                                    repository.syncFileToCloud(file, "Backup_${doc.title.replace("\\s+".toRegex(), "_")}.pdf")
                                                    kotlinx.coroutines.delay(500)
                                                }
                                            }
                                            android.widget.Toast.makeText(context, "All backups uploaded successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            backingUpAll = false
                                        }
                                    }
                                } else if (documents.isEmpty()) {
                                    android.widget.Toast.makeText(context, "No local documents created to backup!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_backup_all_now"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (backingUpAll) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (backingUpAll) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Archiving & Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudDownload, "Backup Now", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trigger Instant Full Cloud Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- SECTION 5: CLOUD SYNC LOGS & HISTORY ---
                    Text("Live Cloud Events", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    if (syncHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No sync events recorded yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            syncHistory.take(8).forEach { historyLine ->
                                val parts = historyLine.split("#")
                                if (parts.size >= 4) {
                                    val logName = parts[0]
                                    val logTime = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                                    val logService = parts[2]
                                    val logStatus = parts[3]
                                    
                                    val logDateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(logTime))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(logName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(logService, fontSize = 9.sp, color = if (logService == "Google Drive") Color(0xFF10B981) else Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
                                                Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(logDateStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        
                                        // Badge
                                        Surface(
                                            color = when (logStatus) {
                                                "Success" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                                "Connected" -> Color(0xFF3B82F6).copy(alpha = 0.12f)
                                                "Disconnected" -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = logStatus,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (logStatus) {
                                                    "Success" -> Color(0xFF10B981)
                                                    "Connected" -> Color(0xFF3B82F6)
                                                    "Disconnected" -> Color(0xFFEF4444)
                                                    else -> MaterialTheme.colorScheme.outline
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            TextButton(
                                onClick = { appStore.clearSyncHistory() },
                                modifier = Modifier.align(Alignment.End).testTag("btn_clear_sync_history"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Sync Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    if (showGoogleDriveLinkDialog) {
        var emailInput by remember { mutableStateOf("") }
        var isLinking by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showGoogleDriveLinkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudQueue, "Google Drive", tint = Color(0xFF10B981))
                    Text("Link Google Drive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Authorize ScanPro AI to automatically backup and store your scanned PDF documents securely inside your Google Drive.", fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text("enter your Google email address", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_gdrive_email"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.contains("@") && emailInput.contains(".")) {
                            isLinking = true
                            scope.launch {
                                kotlinx.coroutines.delay(1200)
                                appStore.connectGoogleDrive(emailInput.trim())
                                isLinking = false
                                showGoogleDriveLinkDialog = false
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Please enter a valid Google email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLinking && emailInput.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Connect Account")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDriveLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDropboxLinkDialog) {
        var accountInput by remember { mutableStateOf("") }
        var isLinking by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showDropboxLinkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Cloud, "Dropbox", tint = Color(0xFF3B82F6))
                    Text("Link Dropbox Storage", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Link Dropbox storage. Documents will sync instantly on download or compile operations.", fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        placeholder = { Text("enter your Dropbox username", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_dropbox_username"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountInput.length >= 3) {
                            isLinking = true
                            scope.launch {
                                kotlinx.coroutines.delay(1200)
                                appStore.connectDropbox(accountInput.trim())
                                isLinking = false
                                showDropboxLinkDialog = false
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Account identifier must be at least 3 characters", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLinking && accountInput.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Verify & Connect")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDropboxLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedRecentConversionPreview != null) {
        val conv = selectedRecentConversionPreview!!
        val iconDetails = getIconForConversionType(conv.type)
        val isDark = isSystemInDarkTheme()

        AlertDialog(
            onDismissRequest = { selectedRecentConversionPreview = null },
            modifier = Modifier.testTag("dialog_recent_scan_preview"),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(iconDetails.bgColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconDetails.icon,
                            contentDescription = conv.type,
                            tint = iconDetails.tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conv.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Format: ${conv.format} • ${timeAgo(conv.timestamp)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- HIGH-FIDELITY PROCEDURAL DOCUMENT PREVIEW CARD ---
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .height(240.dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1F2937) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Scanned Sheet Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated scan label
                                    Text(
                                        text = conv.format.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = iconDetails.tintColor,
                                        letterSpacing = 1.sp
                                    )
                                    // Sync State Badge
                                    Surface(
                                        color = if (isGoogleDriveConnected || isDropboxConnected) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isGoogleDriveConnected || isDropboxConnected) "SYNCED" else "LOCAL ONLY",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGoogleDriveConnected || isDropboxConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Procedural document lines
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Title Line Mock
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(6.dp)
                                            .background(
                                                if (isDark) Color(0xFF4B5563) else Color(0xFFD1D5DB),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    // Smaller Text Lines Mock
                                    repeat(4) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(if (idx % 2 == 0) 0.95f else 0.85f)
                                                .height(3.dp)
                                                .background(
                                                    if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Dynamic visual aspect representing OCR or specific scan type
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(
                                            if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(6.dp)
                                ) {
                                    if (conv.type == "ocr") {
                                        // OCR Mock Panel
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text("OCR METADATA RECOGNIZED", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = iconDetails.tintColor)
                                            repeat(2) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                        .background(if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB))
                                                )
                                            }
                                        }
                                    } else if (conv.type == "signature" || conv.type == "sign") {
                                        // Sign Mock Panel
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Draw, null, tint = iconDetails.tintColor, modifier = Modifier.size(12.dp))
                                                Text("ESIGN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = iconDetails.tintColor)
                                            }
                                        }
                                    } else {
                                        // Standard Page layout with camera outline representation
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(
                                                    1.dp,
                                                    iconDetails.tintColor.copy(alpha = 0.3f),
                                                    RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconDetails.icon,
                                                contentDescription = null,
                                                tint = iconDetails.tintColor.copy(alpha = 0.4f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Watermark / Seal in bottom corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "SCANPRO MOCK",
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iconDetails.tintColor.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Metadata details block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFF9FAFB),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Document ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(conv.id.take(8) + "...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (conv.format == "PDF") "1.2 MB" else "480 KB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export Button
                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, "${conv.name} exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            selectedRecentConversionPreview = null
                        },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("btn_preview_export"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Button
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out my converted file: " + conv.name)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                            selectedRecentConversionPreview = null
                        },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("btn_preview_share"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedRecentConversionPreview = null },
                    modifier = Modifier.testTag("btn_preview_dismiss")
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // High fidelity premium checkout dialog modal matching original React component layout
    if (showPremiumModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPremiumModal = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (isSystemInDarkTheme()) Color(0xFF111827) else Color.White,
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Close button (X) in the top right corner
                    IconButton(
                        onClick = { showPremiumModal = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.06f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Header (Emerald-to-Teal-to-Cyan gradient banner)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF059669),
                                            Color(0xFF0D9488),
                                            Color(0xFF06B6D4)
                                        )
                                    )
                                )
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                // Dynamic Golden Crown icon container with frosted background
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.35f),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Crown Logo",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Unlock PRO",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "12+ AI tools • No ads • Unlimited scans",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA7F3D0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 2. Body / Pricing & Features
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // High-contrast Pricing display
                            Row(
                                modifier = Modifier.padding(bottom = 16.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Just",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "₹100",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF059669), Color(0xFF0D9488))
                                        )
                                    )
                                )
                                Text(
                                    text = "one-time",
                                    fontSize = 13.sp,
                                    color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            // 4 Core Premium Features matching the React specs
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val features = listOf(
                                    PremiumFeatureItem(
                                        icon = Icons.Default.OfflineBolt,
                                        tint = Color(0xFF10B981),
                                        bgLight = Color(0xFFECFDF5),
                                        bgDark = Color(0xFF10B981).copy(alpha = 0.15f),
                                        title = "100 Ad-Free Credits",
                                        desc = "All 12 tools unlocked — scan, convert, enhance"
                                    ),
                                    PremiumFeatureItem(
                                        icon = Icons.Default.Shield,
                                        tint = Color(0xFF3B82F6),
                                        bgLight = Color(0xFFEFF6FF),
                                        bgDark = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                        title = "No Ad Banners",
                                        desc = "Clean, distraction-free experience"
                                    ),
                                    PremiumFeatureItem(
                                        icon = Icons.Default.AutoAwesome,
                                        tint = Color(0xFFF59E0B),
                                        bgLight = Color(0xFFFFFBEB),
                                        bgDark = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        title = "AI-Powered Processing",
                                        desc = "OCR, enhance, background remove & more"
                                    ),
                                    PremiumFeatureItem(
                                        icon = Icons.Default.Star,
                                        tint = Color(0xFF8B5CF6),
                                        bgLight = Color(0xFFF5F3FF),
                                        bgDark = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                        title = "Priority Speed",
                                        desc = "Fastest processing & highest quality output"
                                    )
                                )

                                features.forEach { item ->
                                    val itemBg = if (isSystemInDarkTheme()) item.bgDark else item.bgLight
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(itemBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.tint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSystemInDarkTheme()) Color(0xFFE5E7EB) else Color(0xFF1F2937)
                                            )
                                            Text(
                                                text = item.desc,
                                                fontSize = 11.sp,
                                                color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Main Checkout Action button: "Pay ₹100 & Unlock Everything"
                            Button(
                                onClick = {
                                    appStore.activatePremium()
                                    showPremiumModal = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF059669),
                                                    Color(0xFF0D9488),
                                                    Color(0xFF06B6D4)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OfflineBolt,
                                            contentDescription = "Zap",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Pay ₹100 & Unlock Everything",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "One-time payment • Instant activation • No subscription",
                                fontSize = 10.sp,
                                color = if (isSystemInDarkTheme()) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom secure highlight bullet list
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Secure Payment", "Instant Access", "No Recurring").forEach { bullet ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Checked",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = bullet,
                                            fontSize = 9.sp,
                                            color = if (isSystemInDarkTheme()) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                            fontWeight = FontWeight.Medium
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

    if (showWelcomeModal) {
        WelcomeModal(
            isOpen = showWelcomeModal,
            onClose = { showWelcomeModal = false }
        )
    }
}

private data class PremiumFeatureItem(
    val icon: ImageVector,
    val tint: Color,
    val bgLight: Color,
    val bgDark: Color,
    val title: String,
    val desc: String
)

data class ToolItem(
    val id: String,
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val lightBg: Color,
    val darkBg: Color,
    val badge: String?,
    val badgeColor: Color,
    val action: (() -> Unit)?
)

private data class IconDetails(val icon: ImageVector, val bgColor: Color, val tintColor: Color)
private data class BadgeColors(val bgColor: Color, val textColor: Color)

@Composable
private fun getIconForConversionType(type: String): IconDetails {
    val isDark = isSystemInDarkTheme()
    return when (type) {
        "camera-scan" -> IconDetails(
            icon = Icons.Default.Camera,
            bgColor = if (isDark) Color(0x3334D399) else Color(0xFFECFDF5),
            tintColor = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
        )
        "ocr" -> IconDetails(
            icon = Icons.Default.DocumentScanner,
            bgColor = if (isDark) Color(0x3360A5FA) else Color(0xFFEFF6FF),
            tintColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        )
        "image-to-pdf" -> IconDetails(
            icon = Icons.Default.PictureAsPdf,
            bgColor = if (isDark) Color(0x33FB923C) else Color(0xFFFFF7ED),
            tintColor = if (isDark) Color(0xFFFB923C) else Color(0xFFF97316)
        )
        "pdf-to-image" -> IconDetails(
            icon = Icons.Default.PhotoLibrary,
            bgColor = if (isDark) Color(0x33C084FC) else Color(0xFFF5F3FF),
            tintColor = if (isDark) Color(0xFFC084FC) else Color(0xFF8B5CF6)
        )
        "format-convert" -> IconDetails(
            icon = Icons.Default.SwapHoriz,
            bgColor = if (isDark) Color(0x3322D3EE) else Color(0xFFECFEFF),
            tintColor = if (isDark) Color(0xFF22D3EE) else Color(0xFF06B6D4)
        )
        "qr-scanner" -> IconDetails(
            icon = Icons.Default.QrCodeScanner,
            bgColor = if (isDark) Color(0x33F472B6) else Color(0xFFFDF2F8),
            tintColor = if (isDark) Color(0xFFF472B6) else Color(0xFFEC4899)
        )
        "enhance" -> IconDetails(
            icon = Icons.Default.AutoFixHigh,
            bgColor = if (isDark) Color(0x33FBBF24) else Color(0xFFFEF3C7),
            tintColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
        )
        "compress" -> IconDetails(
            icon = Icons.Default.Compress,
            bgColor = if (isDark) Color(0x332DD4BF) else Color(0xFFF0FDFA),
            tintColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
        )
        "signature", "sign" -> IconDetails(
            icon = Icons.Default.Draw,
            bgColor = if (isDark) Color(0x33F43F5E) else Color(0xFFFFF1F2),
            tintColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFF43F5E)
        )
        "batch" -> IconDetails(
            icon = Icons.Default.Layers,
            bgColor = if (isDark) Color(0x33818CF8) else Color(0xFFEEF2FF),
            tintColor = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1)
        )
        "background-remove" -> IconDetails(
            icon = Icons.Default.Wallpaper,
            bgColor = if (isDark) Color(0x33E879F9) else Color(0xFFFDF4FF),
            tintColor = if (isDark) Color(0xFFE879F9) else Color(0xFFD946EF)
        )
        "text-to-image" -> IconDetails(
            icon = Icons.Default.TextFields,
            bgColor = if (isDark) Color(0x3338BDF8) else Color(0xFFF0F9FF),
            tintColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0EA5E9)
        )
        else -> IconDetails(
            icon = Icons.Default.Description,
            bgColor = if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6),
            tintColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        )
    }
}

@Composable
private fun formatBadgeColor(format: String): BadgeColors {
    return when (format.uppercase()) {
        "PDF" -> BadgeColors(bgColor = Color(0xFFFFCDD2), textColor = Color(0xFFC62828))
        "PNG" -> BadgeColors(bgColor = Color(0xFFBBDEFB), textColor = Color(0xFF1565C0))
        "JPG", "JPEG" -> BadgeColors(bgColor = Color(0xFFFFE0B2), textColor = Color(0xFFE65100))
        "TXT" -> BadgeColors(bgColor = Color(0xFFE0E0E0), textColor = Color(0xFF424242))
        else -> BadgeColors(bgColor = Color(0xFFEEEEEE), textColor = Color(0xFF757575))
    }
}

private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60000
    if (mins < 1) return "just now"
    if (mins < 60) return "$mins min${if (mins > 1) "s" else ""} ago"
    val hrs = mins / 60
    if (hrs < 24) return "$hrs hr${if (hrs > 1) "s" else ""} ago"
    val days = hrs / 24
    return "$days day${if (days > 1) "s" else ""} ago"
}
