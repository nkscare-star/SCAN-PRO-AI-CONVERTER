package com.example.data.database

import androidx.room.*
import com.example.data.models.DocumentPage
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocumentsFlow(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    suspend fun getAllDocuments(): List<ScannedDocument>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): ScannedDocument?

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesFlow(documentId: String): Flow<List<DocumentPage>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPages(documentId: String): List<DocumentPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPage)

    @Update
    suspend fun updatePage(page: DocumentPage)

    @Update
    suspend fun updateDocument(document: ScannedDocument)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: String)

    @Query("DELETE FROM document_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: String)

    @Transaction
    suspend fun deleteDocumentWithPages(id: String) {
        deletePagesByDocumentId(id)
        deleteDocumentById(id)
    }

    @Transaction
    suspend fun updatePageOrder(pages: List<DocumentPage>) {
        pages.forEachIndexed { index, page ->
            insertPage(page.copy(pageNumber = index))
        }
    }

    @Query("SELECT * FROM recent_conversions ORDER BY timestamp DESC")
    fun getAllRecentConversionsFlow(): Flow<List<RecentConversion>>

    @Query("SELECT * FROM recent_conversions ORDER BY timestamp DESC")
    suspend fun getAllRecentConversions(): List<RecentConversion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentConversion(conversion: RecentConversion)

    @Query("DELETE FROM recent_conversions WHERE id = :id")
    suspend fun deleteRecentConversionById(id: String)
}
