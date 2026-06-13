package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.DocumentPage
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion

@Database(entities = [ScannedDocument::class, DocumentPage::class, RecentConversion::class], version = 2, exportSchema = false)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: DocumentDatabase? = null

        fun getDatabase(context: Context): DocumentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocumentDatabase::class.java,
                    "document_scanner_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
