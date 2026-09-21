package com.example.loop.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loop.timer.TimerManager
import kotlinx.coroutines.delay

@Composable
fun StrictBlockApp(viewModel: MainViewModel) {
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (lockState.isLocked) {
        ActiveLockScreen(viewModel, lockState.lockEndTime)
    } else {
        if (!isAccessibilityServiceEnabled(context, com.example.loop.blocking.StrictBlockAccessibilityService::class.java)) {
            AccessibilityPromptScreen(context)
        } else {
            var showTimerScreen by remember { mutableStateOf(false) }
            if (showTimerScreen) {
                TimerScreen(
                    viewModel = viewModel,
                    onBack = { showTimerScreen = false }
                )
            } else {
                AppSelectionScreen(
                    viewModel = viewModel,
                    onNext = { showTimerScreen = true }
                )
            }
        }
    }
}

@Composable
fun AccessibilityPromptScreen(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Accessibility Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "loop requires Accessibility Service permission to instantly detect and block distracting applications.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Enable Accessibility")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: MainViewModel, onNext: () -> Unit) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Apps to Block") }
            )
        },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                FloatingActionButton(onClick = onNext) {
                    Text("Next", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Text("?") },
                singleLine = true
            )

            val filteredApps = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps) { app ->
                    val isSelected = selected.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleSelection(app.packageName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, fontSize = 18.sp)
                            Text(text = app.packageName, fontSize = 12.sp, color = Color.Gray)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSelection(app.packageName) }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()
    var selectedDurationMs by remember { mutableStateOf(15 * 60 * 1000L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Set Timer", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "You are about to block ${selected.size} apps.")
        Spacer(modifier = Modifier.height(24.dp))

        val options = listOf(
            "15 minutes" to 15 * 60 * 1000L,
            "30 minutes" to 30 * 60 * 1000L,
            "1 hour" to 60 * 60 * 1000L,
            "2 hours" to 2 * 60 * 60 * 1000L
        )

        options.forEach { (label, duration) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedDurationMs = duration }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedDurationMs == duration,
                    onClick = { selectedDurationMs = duration }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "WARNING: Once started, this session cannot be paused, cancelled or shortened. The selected apps will remain blocked until the timer expires.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onBack) {
                Text("Cancel")
            }
            Button(
                onClick = { viewModel.startLock(selectedDurationMs) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("START STRICT LOCK")
            }
        }
    }
}

@Composable
fun ActiveLockScreen(viewModel: MainViewModel, lockEndTime: Long) {
    var remainingTime by remember { mutableStateOf(TimerManager.formatRemainingTime(lockEndTime)) }

    LaunchedEffect(lockEndTime) {
        while (true) {
            remainingTime = TimerManager.formatRemainingTime(lockEndTime)
            delay(1000)
            if (System.currentTimeMillis() >= lockEndTime) {
                // Should technically be cleared by viewmodel
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("STRICT LOCK ACTIVE \uD83D\uDD12", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.height(16.dp))
        Text(remainingTime, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = { viewModel.extendLock(30 * 60 * 1000L) }) {
            Text("Extend Timer (+30m)")
        }
    }
}

// Utility function to check Accessibility Service status
fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
    var accessibilityEnabled = 0
    val service = context.packageName + "/" + accessibilityService.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        // Ignored
    }
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            colonSplitter.setString(settingValue)
            while (colonSplitter.hasNext()) {
                val accessibilityServiceName = colonSplitter.next()
                if (accessibilityServiceName.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
    }
    return false
}
