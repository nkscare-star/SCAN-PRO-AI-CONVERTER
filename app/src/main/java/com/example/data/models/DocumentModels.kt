package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class ScannedDocument(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val notes: String = ""
)

@Entity(tableName = "document_pages")
data class DocumentPage(
    @PrimaryKey val id: String,
    val documentId: String,
    val imagePath: String, // local file path to the cropped/enhanced JPEG
    val originalImagePath: String, // backup original JPEG
    val pageNumber: Int,
    val ocrText: String? = null,
    val signaturePath: String? = null, // signature image placed on this page
    val signatureX: Float = 0f,
    val signatureY: Float = 0f,
    val signatureScale: Float = 1.0f
)

@Entity(tableName = "recent_conversions")
data class RecentConversion(
    @PrimaryKey val id: String,
    val name: String,
    val timestamp: Long,
    val type: String,   // 'pdf', 'image', 'jpeg', 'png', 'excel', 'ocr', 'sign', 'compress'
    val format: String  // 'PDF', 'PNG', 'JPG', 'TXT'
)
