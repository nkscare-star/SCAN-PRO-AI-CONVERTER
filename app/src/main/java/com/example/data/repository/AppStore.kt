package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("scanpro_prefs", Context.MODE_PRIVATE)

    private val _usageCount = MutableStateFlow(prefs.getInt("scanpro_usage", 0))
    val usageCount: StateFlow<Int> = _usageCount.asStateFlow()

    val maxFreeUsage = 50

    private val _isPremium = MutableStateFlow(prefs.getBoolean("scanpro_premium", false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumRemaining = MutableStateFlow(prefs.getInt("scanpro_premiumRem", 0))
    val premiumRemaining: StateFlow<Int> = _premiumRemaining.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("scanpro_dark", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _activeTool = MutableStateFlow<String?>(null)
    val activeTool: StateFlow<String?> = _activeTool.asStateFlow()

    // Cloud Sync Core Preferences
    private val _isGoogleDriveConnected = MutableStateFlow(prefs.getBoolean("scanpro_gdrive_connected", false))
    val isGoogleDriveConnected: StateFlow<Boolean> = _isGoogleDriveConnected.asStateFlow()

    private val _googleDriveEmail = MutableStateFlow(prefs.getString("scanpro_gdrive_email", null))
    val googleDriveEmail: StateFlow<String?> = _googleDriveEmail.asStateFlow()

    private val _isDropboxConnected = MutableStateFlow(prefs.getBoolean("scanpro_dropbox_connected", false))
    val isDropboxConnected: StateFlow<Boolean> = _isDropboxConnected.asStateFlow()

    private val _dropboxAccount = MutableStateFlow(prefs.getString("scanpro_dropbox_account", null))
    val dropboxAccount: StateFlow<String?> = _dropboxAccount.asStateFlow()

    private val _autoUploadEnabled = MutableStateFlow(prefs.getBoolean("scanpro_auto_upload", true))
    val autoUploadEnabled: StateFlow<Boolean> = _autoUploadEnabled.asStateFlow()

    private val _syncHistory = MutableStateFlow<List<String>>(
        prefs.getString("scanpro_sync_history", "")?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val syncHistory: StateFlow<List<String>> = _syncHistory.asStateFlow()

    fun connectGoogleDrive(email: String) {
        _isGoogleDriveConnected.value = true
        _googleDriveEmail.value = email
        prefs.edit()
            .putBoolean("scanpro_gdrive_connected", true)
            .putString("scanpro_gdrive_email", email)
            .apply()
        addSyncLog("Google Drive Account Connected ($email)", "Google Drive", "Connected")
    }

    fun disconnectGoogleDrive() {
        val email = _googleDriveEmail.value ?: "unknown"
        _isGoogleDriveConnected.value = false
        _googleDriveEmail.value = null
        prefs.edit()
            .putBoolean("scanpro_gdrive_connected", false)
            .remove("scanpro_gdrive_email")
            .apply()
        addSyncLog("Google Drive Account Disconnected ($email)", "Google Drive", "Disconnected")
    }

    fun connectDropbox(account: String) {
        _isDropboxConnected.value = true
        _dropboxAccount.value = account
        prefs.edit()
            .putBoolean("scanpro_dropbox_connected", true)
            .putString("scanpro_dropbox_account", account)
            .apply()
        addSyncLog("Dropbox Account Connected ($account)", "Dropbox", "Connected")
    }

    fun disconnectDropbox() {
        val account = _dropboxAccount.value ?: "unknown"
        _isDropboxConnected.value = false
        _dropboxAccount.value = null
        prefs.edit()
            .putBoolean("scanpro_dropbox_connected", false)
            .remove("scanpro_dropbox_account")
            .apply()
        addSyncLog("Dropbox Account Disconnected ($account)", "Dropbox", "Disconnected")
    }

    fun setAutoUploadEnabled(enabled: Boolean) {
        _autoUploadEnabled.value = enabled
        prefs.edit().putBoolean("scanpro_auto_upload", enabled).apply()
    }

    fun addSyncLog(fileName: String, service: String, status: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "$fileName#$timestamp#$service#$status"
        val currentList = _syncHistory.value.toMutableList()
        // limit history to 50 logs
        if (currentList.size >= 50) {
            currentList.removeAt(currentList.size - 1)
        }
        currentList.add(0, entry)
        _syncHistory.value = currentList
        prefs.edit().putString("scanpro_sync_history", currentList.joinToString("||")).apply()
    }

    fun clearSyncHistory() {
        _syncHistory.value = emptyList()
        prefs.edit().remove("scanpro_sync_history").apply()
    }

    fun canUse(): Boolean {
        if (_isPremium.value && _premiumRemaining.value > 0) return true
        return _usageCount.value < maxFreeUsage
    }

    fun getRemaining(): Int {
        if (_isPremium.value) return _premiumRemaining.value
        return (maxFreeUsage - _usageCount.value).coerceAtLeast(0)
    }

    fun incrementUsage(): Boolean {
        if (_isPremium.value && _premiumRemaining.value > 0) {
            val r = _premiumRemaining.value - 1
            _premiumRemaining.value = r
            prefs.edit().putInt("scanpro_premiumRem", r).apply()
            return true
        }
        if (_usageCount.value < maxFreeUsage) {
            val c = _usageCount.value + 1
            _usageCount.value = c
            prefs.edit().putInt("scanpro_usage", c).apply()
            return true
        }
        return false
    }

    fun activatePremium() {
        _isPremium.value = true
        _premiumRemaining.value = 100
        prefs.edit()
            .putBoolean("scanpro_premium", true)
            .putInt("scanpro_premiumRem", 100)
            .apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit().putBoolean("scanpro_dark", enabled).apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!_darkMode.value)
    }

    fun setActiveTool(tool: String?) {
        _activeTool.value = tool
    }
}
