package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.models.DocumentPage
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion
import com.example.data.repository.DocumentRepository
import com.example.network.GeminiService
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: ScannedDocument,
    repository: DocumentRepository,
    onBack: () -> Unit,
    onGoToCompressor: (DocumentPage) -> Unit,
    onGoToSignature: () -> Unit,
    savedSignaturePath: String? = null // Passed if user just signature created!
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val pages by repository.getPagesFlow(document.id).collectAsState(initial = emptyList())
    var selectedPageIdx by remember { mutableStateOf(0) }

    // Selected Page
    val currentPage = pages.getOrNull(selectedPageIdx)

    // OCR AI States
    var aiResultTitle by remember { mutableStateOf("") }
    var aiResultContent by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var showAiResultDialog by remember { mutableStateOf(false) }

    // Signature stamp dragging values
    var sigOffset by remember { mutableStateOf(Offset(200f, 400f)) }
    var sigScale by remember { mutableStateOf(1f) }
    var isPlacingSignature by remember { mutableStateOf(false) }

    // Sync state if signature just created
    LaunchedEffect(savedSignaturePath) {
        if (savedSignaturePath != null && currentPage != null) {
            isPlacingSignature = true
            repository.updatePage(
                currentPage.copy(
                    signaturePath = savedSignaturePath,
                    signatureX = sigOffset.x,
                    signatureY = sigOffset.y,
                    signatureScale = sigScale
                )
            )
        }
    }

    // Function to launch Share intents of standard generated PDFs
    fun sharePdf(file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document.title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val pdfFile = repository.compileDocumentToPdf(document.id)
                            if (pdfFile != null) {
                                val cleanTitle = document.title.replace("\\s+".toRegex(), "_")
                                val displayName = "Scan_${cleanTitle}_${System.currentTimeMillis() / 1000}.pdf"
                                val success = repository.savePdfToPublicDownloads(pdfFile, displayName)
                                if (success) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Saved to Downloads: $displayName",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error saving PDF to Downloads folder",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }, modifier = Modifier.testTag("detail_download_pdf_top_btn")) {
                        Icon(Icons.Default.Download, "Download as PDF")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val pdfFile = repository.compileDocumentToPdf(document.id)
                            if (pdfFile != null) {
                                printPdf(context, pdfFile)
                            }
                        }
                    }, modifier = Modifier.testTag("detail_print_pdf_top_btn")) {
                        Icon(Icons.Default.Print, "Print PDF")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val pdfFile = repository.compileDocumentToPdf(document.id)
                            if (pdfFile != null) {
                                repository.insertRecentConversion(
                                    RecentConversion(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = pdfFile.name,
                                        timestamp = System.currentTimeMillis(),
                                        type = "image-to-pdf",
                                        format = "PDF"
                                    )
                                )
                                sharePdf(pdfFile)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, "Share PDF")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            repository.deleteDocument(document.id)
                            onBack()
                        }
                    }, modifier = Modifier.testTag("delete_doc_btn")) {
                        Icon(Icons.Default.Delete, "Delete Doc", tint = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (pages.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Main Page canvas container
                currentPage?.let { page ->
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = remember(page.imagePath) {
                            BitmapFactory.decodeFile(page.imagePath)
                        }

                        if (bitmap != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Active Document Page",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Signature Stamp draggable node overlay
                                if (page.signaturePath != null) {
                                    val sigBitmap = remember(page.signaturePath) {
                                        BitmapFactory.decodeFile(page.signaturePath)
                                    }
                                    if (sigBitmap != null) {
                                        Image(
                                            bitmap = sigBitmap.asImageBitmap(),
                                            contentDescription = "Signature Overlay",
                                            modifier = Modifier
                                                .size(140.dp)
                                                .offset {
                                                    IntOffset(
                                                        sigOffset.x.roundToInt(),
                                                        sigOffset.y.roundToInt()
                                                    )
                                                }
                                                .border(
                                                    1.dp,
                                                    if (isPlacingSignature) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .pointerInput(Unit) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        sigOffset = sigOffset + dragAmount
                                                        scope.launch {
                                                            repository.updatePage(
                                                                page.copy(
                                                                    signatureX = sigOffset.x,
                                                                    signatureY = sigOffset.y
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("No preview file available")
                        }

                        // Bottom index label
                        Badge(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            Text(
                                "Page ${selectedPageIdx + 1} of ${pages.size}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Horizontal list of thumbnails
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pages) { index, page ->
                        val isSelected = index == selectedPageIdx
                        val thumbBitmap = remember(page.imagePath) {
                            BitmapFactory.decodeFile(page.imagePath)
                        }
                        Card(
                            modifier = Modifier
                                .size(64.dp, 84.dp)
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedPageIdx = index },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (thumbBitmap != null) {
                                Image(
                                    bitmap = thumbBitmap.asImageBitmap(),
                                    contentDescription = "Thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val pdfFile = repository.compileDocumentToPdf(document.id)
                            if (pdfFile != null) {
                                val cleanTitle = document.title.replace("\\s+".toRegex(), "_")
                                val displayName = "Scan_${cleanTitle}_${System.currentTimeMillis() / 1000}.pdf"
                                val success = repository.savePdfToPublicDownloads(pdfFile, displayName)
                                if (success) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Saved to Downloads: $displayName",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error saving PDF to Downloads folder",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_download_pdf"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download as PDF",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download as PDF",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pdfFile = repository.compileDocumentToPdf(document.id)
                            if (pdfFile != null) {
                                printPdf(context, pdfFile)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_print_pdf"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print PDF",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Print Document",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Action Operations grid scrollable
                Text(
                    text = "AI-Powered Smart Actions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: OCR & Translation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                currentPage?.let { page ->
                                    val bitmap = BitmapFactory.decodeFile(page.imagePath)
                                    if (bitmap != null) {
                                        isAiLoading = true
                                        showAiResultDialog = true
                                        aiResultTitle = "AI OCR text Extraction"
                                        scope.launch {
                                            aiResultContent = GeminiService.performOcr(bitmap)
                                            repository.insertRecentConversion(
                                                RecentConversion(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = "OCR_" + document.title.take(10),
                                                    timestamp = System.currentTimeMillis(),
                                                    type = "ocr",
                                                    format = "TXT"
                                                )
                                            )
                                            isAiLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_ocr_extract"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DocumentScanner, "OCR")
                            Text("Extract OCR", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                currentPage?.let { page ->
                                    val bitmap = BitmapFactory.decodeFile(page.imagePath)
                                    if (bitmap != null) {
                                        isAiLoading = true
                                        showAiResultDialog = true
                                        aiResultTitle = "AI Translate (Hindi/Spanish)"
                                        scope.launch {
                                            val ocrText = GeminiService.performOcr(bitmap)
                                            aiResultContent = GeminiService.translateText(ocrText, "Spanish / Hindi")
                                            isAiLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Translate, "Translate")
                            Text("AI Translate", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                        }
                    }

                    // Row 2: AI Summarize & Invoice Parser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                currentPage?.let { page ->
                                    val bitmap = BitmapFactory.decodeFile(page.imagePath)
                                    if (bitmap != null) {
                                        isAiLoading = true
                                        showAiResultDialog = true
                                        aiResultTitle = "AI Document Analysis Summary"
                                        scope.launch {
                                            val ocrText = GeminiService.performOcr(bitmap)
                                            aiResultContent = GeminiService.summarizeText(ocrText)
                                            isAiLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Summarize, "Summarize")
                            Text("AI Summarize", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                currentPage?.let { page ->
                                    val bitmap = BitmapFactory.decodeFile(page.imagePath)
                                    if (bitmap != null) {
                                        isAiLoading = true
                                        showAiResultDialog = true
                                        aiResultTitle = "AI Invoice facts auditing"
                                        scope.launch {
                                            aiResultContent = GeminiService.parseInvoice(bitmap)
                                            isAiLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Default.ReceiptLong, "Invoice Audit")
                            Text("Invoice Audit", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                        }
                    }

                    // Row 3: Signature & Compression Size Optimizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGoToSignature,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Draw, "Draw")
                            Text("Sign Doc", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                        }

                        currentPage?.let { page ->
                            OutlinedButton(
                                onClick = { onGoToCompressor(page) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OfflineBolt, "Compress")
                                Text("Aesthetic Compress", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // AI modal dialogues
    if (showAiResultDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAiLoading) showAiResultDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(aiResultTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isAiLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "AI Analyzing Document...",
                                modifier = Modifier.padding(top = 16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = aiResultContent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(aiResultContent))
                        },
                        enabled = !isAiLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                        Text("Copy Info", modifier = Modifier.padding(start = 6.dp))
                    }

                    Button(
                        onClick = { showAiResultDialog = false },
                        enabled = !isAiLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}

// Adapter for printing PDF file using Android PrintManager
class PdfPrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null
        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination?.fileDescriptor)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } >= 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                output.write(buffer, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

// Helper function to trigger printing in Android
fun printPdf(context: Context, pdfFile: File) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val jobName = "${pdfFile.nameWithoutExtension}_PrintJob"
            printManager.print(jobName, PdfPrintDocumentAdapter(pdfFile), null)
        } else {
            android.widget.Toast.makeText(context, "Printing system not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error printing document: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}
