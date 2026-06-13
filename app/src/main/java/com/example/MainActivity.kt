package com.example

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.models.DocumentPage
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CompressorScreen
import com.example.ui.screens.CropFilterScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SignatureDrawScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ScannerViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by viewModel.appStore.darkMode.collectAsState()
            val isDarkTheme = darkMode
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation(
                            viewModel = viewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = {
                                viewModel.appStore.toggleDarkMode()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    viewModel: ScannerViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("dashboard") }

    // Core state buffers
    val documents by viewModel.documents.collectAsState()
    val recentConversions by viewModel.recentConversions.collectAsState()
    var activeDocument by remember { mutableStateOf<ScannedDocument?>(null) }
    var activePage by remember { mutableStateOf<DocumentPage?>(null) }
    var rawCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedSignaturePath by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (targetState == "dashboard") {
                fadeIn() with fadeOut()
            } else {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            }
        }
    ) { route ->
        when (route) {
            "dashboard" -> {
                DashboardScreen(
                    documents = documents,
                    recentConversions = recentConversions,
                    repository = viewModel.repository,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onNavigateToCamera = {
                        currentRoute = "camera"
                    },
                    onNavigateToDetail = { doc ->
                        activeDocument = doc
                        savedSignaturePath = null
                        currentRoute = "detail"
                    },
                    onNavigateToQr = {
                        currentRoute = "qr_scanner"
                    },
                    onNavigateToSignature = {
                        currentRoute = "signature"
                    },
                    appStore = viewModel.appStore
                )
            }
            "camera" -> {
                CameraScannerScreen(
                    onBack = { currentRoute = "dashboard" },
                    onCapturedPage = { bitmap ->
                        rawCapturedBitmap = bitmap
                        currentRoute = "crop_filter"
                    }
                )
            }
            "crop_filter" -> {
                rawCapturedBitmap?.let { bitmap ->
                    CropFilterScreen(
                        rawBitmap = bitmap,
                        onBack = { currentRoute = "camera" },
                        onSaveCroppedAndFiltered = { processedBitmap ->
                            scope.launch {
                                val doc = activeDocument
                                if (doc != null) {
                                    // Add page into existing document folder
                                    viewModel.repository.addPageToDocument(doc.id, processedBitmap)
                                    currentRoute = "detail"
                                } else {
                                    // Create a beautiful new document
                                    val newDoc = viewModel.repository.createNewDocument("", listOf(processedBitmap))
                                    viewModel.repository.insertRecentConversion(
                                        RecentConversion(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = newDoc.title,
                                            timestamp = System.currentTimeMillis(),
                                            type = "camera-scan",
                                            format = "JPG"
                                        )
                                    )
                                    activeDocument = newDoc
                                    currentRoute = "detail"
                                }
                            }
                        }
                    )
                }
            }
            "detail" -> {
                activeDocument?.let { doc ->
                    DocumentDetailScreen(
                        document = doc,
                        repository = viewModel.repository,
                        onBack = {
                            activeDocument = null
                            currentRoute = "dashboard"
                        },
                        onGoToCompressor = { page ->
                            activePage = page
                            currentRoute = "compressor"
                        },
                        onGoToSignature = {
                            currentRoute = "signature"
                        },
                        savedSignaturePath = savedSignaturePath
                    )
                }
            }
            "signature" -> {
                SignatureDrawScreen(
                    onBack = {
                        currentRoute = if (activeDocument != null) "detail" else "dashboard"
                    },
                    onSignatureSaved = { sigBitmap ->
                        scope.launch {
                            val path = viewModel.repository.saveSignatureToDisk(sigBitmap)
                            savedSignaturePath = path
                            viewModel.repository.insertRecentConversion(
                                RecentConversion(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = "Sig_" + (System.currentTimeMillis() / 1000),
                                    timestamp = System.currentTimeMillis(),
                                    type = "signature",
                                    format = "PNG"
                                )
                            )
                            currentRoute = if (activeDocument != null) "detail" else "dashboard"
                        }
                    }
                )
            }
            "compressor" -> {
                activePage?.let { page ->
                    CompressorScreen(
                        page = page,
                        repository = viewModel.repository,
                        onBack = {
                            activePage = null
                            currentRoute = "detail"
                        },
                        onCompressed = {
                            activePage = null
                            currentRoute = "detail"
                        }
                    )
                }
            }
            "qr_scanner" -> {
                QrScannerScreen(
                    onBack = { currentRoute = "dashboard" },
                    onQrScanned = { _ -> }
                )
            }
        }
    }
}
