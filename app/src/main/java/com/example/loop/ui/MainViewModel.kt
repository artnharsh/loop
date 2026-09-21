package com.example.loop.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.loop.data.LockRepository
import com.example.loop.data.LockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val name: String,
    val isSystem: Boolean,
    val isCritical: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val lockRepository = LockRepository(application)
    
    val lockState: StateFlow<LockState> = lockRepository.lockStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LockState()
    )

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    init {
        loadInstalledApps()
        viewModelScope.launch {
            lockRepository.clearLockIfExpired()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                
                val defaultLauncherPackage = getDefaultLauncherPackage(pm)
                
                resolveInfos.mapNotNull { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val packageName = appInfo.packageName
                    val name = appInfo.loadLabel(pm).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    
                    val isCritical = isSystem || isCriticalPackage(packageName, defaultLauncherPackage)
                    
                    if (isCritical) {
                        null // Filter out critical apps so they can't even be seen/selected
                    } else {
                        AppInfo(
                            packageName = packageName,
                            name = name,
                            isSystem = isSystem,
                            isCritical = false
                        )
                    }
                }.distinctBy { it.packageName }.sortedBy { it.name }
            }
            _installedApps.value = apps
        }
    }
    
    private fun getDefaultLauncherPackage(pm: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun isCriticalPackage(packageName: String, defaultLauncherPackage: String?): Boolean {
        if (packageName == getApplication<Application>().packageName) return true
        if (packageName == defaultLauncherPackage) return true
        
        val criticalKeywords = listOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.dialer",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.emergency"
        )
        return criticalKeywords.any { packageName.contains(it) }
    }

    fun toggleSelection(packageName: String) {
        _selectedPackages.value = _selectedPackages.value.toMutableSet().apply {
            if (contains(packageName)) remove(packageName) else add(packageName)
        }
    }

    fun startLock(durationMs: Long) {
        val packages = _selectedPackages.value
        if (packages.isEmpty()) return
        
        viewModelScope.launch {
            lockRepository.startLock(packages, durationMs)
        }
    }
    
    fun extendLock(additionalDurationMs: Long) {
        viewModelScope.launch {
            lockRepository.extendLock(additionalDurationMs)
        }
    }
}
