package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DocumentDatabase
import com.example.data.models.DocumentPage
import com.example.data.models.ScannedDocument
import com.example.data.models.RecentConversion
import com.example.data.repository.DocumentRepository
import com.example.data.repository.AppStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    val repository: DocumentRepository
    val appStore: AppStore
    val documents: StateFlow<List<ScannedDocument>>
    val recentConversions: StateFlow<List<RecentConversion>>

    init {
        val database = DocumentDatabase.getDatabase(application)
        repository = DocumentRepository(application, database.documentDao())
        appStore = AppStore(application)
        documents = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        recentConversions = repository.allRecentConversions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}

