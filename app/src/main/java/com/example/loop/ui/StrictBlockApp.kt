package com.example.loop.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import android.text.TextUtils
import android.widget.NumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loop.timer.TimerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun StrictBlockApp(viewModel: MainViewModel) {
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val isAddingToLock by viewModel.isAddingToActiveLock.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!isAccessibilityServiceEnabled(context, com.example.loop.blocking.StrictBlockAccessibilityService::class.java)) {
        AccessibilityPromptScreen(context)
    } else {
        if (lockState.isLocked && !isAddingToLock) {
            ActiveLockScreen(viewModel, lockState.lockEndTime)
        } else {
            var showTimerScreen by remember { mutableStateOf(false) }
            var showReviewScreen by remember { mutableStateOf(false) }

            if (showTimerScreen) {
                TimerScreen(
                    viewModel = viewModel,
                    onBack = { showTimerScreen = false }
                )
            } else if (showReviewScreen) {
                ReviewSelectionScreen(
                    viewModel = viewModel,
                    onBack = {
                        showReviewScreen = false
                        if (isAddingToLock) viewModel.cancelAddingToLock()
                    },
                    onConfirm = {
                        if (isAddingToLock) {
                            viewModel.confirmAddingToLock()
                            showReviewScreen = false
                        } else {
                            showTimerScreen = true
                            showReviewScreen = false
                        }
                    }
                )
            } else {
                AppSelectionScreen(
                    viewModel = viewModel,
                    isAddingToLock = isAddingToLock,
                    onNext = { showReviewScreen = true },
                    onCancelAdd = { viewModel.cancelAddingToLock() }
                )
            }
        }
    }
}

@Composable
fun AccessibilityPromptScreen(context: Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Accessibility Required", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("loop requires Accessibility Service permission to instantly detect and block distracting applications.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
            Text("Enable Accessibility")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: MainViewModel, isAddingToLock: Boolean, onNext: () -> Unit, onCancelAdd: () -> Unit) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isAddingToLock) "Add Apps to Lock" else "Select Apps to Block") }) },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                FloatingActionButton(onClick = onNext) {
                    Text("Next", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isAddingToLock) {
                Button(onClick = onCancelAdd, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Text("Cancel Adding")
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            val filteredApps = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps) { app ->
                    val isSelected = selected.contains(app.packageName)
                    val lockState = viewModel.lockState.value
                    val isAlreadyBlocked = isAddingToLock && lockState.blockedPackages.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAlreadyBlocked) { viewModel.toggleSelection(app.packageName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, fontSize = 18.sp, color = if (isAlreadyBlocked) Color.Gray else Color.Unspecified)
                            Text(text = app.packageName, fontSize = 12.sp, color = Color.Gray)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSelection(app.packageName) },
                            enabled = !isAlreadyBlocked
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSelectionScreen(viewModel: MainViewModel, onBack: () -> Unit, onConfirm: () -> Unit) {
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isAddingToLock by viewModel.isAddingToActiveLock.collectAsStateWithLifecycle()
    val lockState = viewModel.lockState.value

    val selectedApps = apps.filter { selected.contains(it.packageName) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review Selected Apps") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "You have selected ${selectedApps.size} apps to block.",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectedApps) { app ->
                    val isAlreadyBlocked = isAddingToLock && lockState.blockedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = app.name, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        if (!isAlreadyBlocked) {
                            IconButton(onClick = { viewModel.toggleSelection(app.packageName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        } else {
                            Text("Already Blocked", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider()
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onConfirm, enabled = selectedApps.isNotEmpty()) {
                    Text(if (isAddingToLock) "Confirm & Add" else "Proceed to Timer")
                }
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val selected by viewModel.selectedPackages.collectAsStateWithLifecycle()
    var selectedDurationMs by remember { mutableStateOf(15 * 60 * 1000L) }
    var isCustom by remember { mutableStateOf(false) }

    var customHours by remember { mutableStateOf(0) }
    var customMinutes by remember { mutableStateOf(15) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Set Timer") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        val options = listOf(
            "15 minutes" to 15 * 60 * 1000L,
            "30 minutes" to 30 * 60 * 1000L,
            "1 hour" to 60 * 60 * 1000L,
            "2 hours" to 2 * 60 * 60 * 1000L
        )

        options.forEach { (label, duration) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    isCustom = false
                    selectedDurationMs = duration
                }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = !isCustom && selectedDurationMs == duration, onClick = {
                    isCustom = false
                    selectedDurationMs = duration
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, fontSize = 18.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { isCustom = true }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isCustom, onClick = { isCustom = true })
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Custom Timer (Up to 8 hrs)", fontSize = 18.sp)
        }

        if (isCustom) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hours")
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 8
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    textColor = android.graphics.Color.WHITE
                                }
                                setOnValueChangedListener { _, _, newVal ->
                                    customHours = newVal
                                    if (customHours == 8) customMinutes = 0
                                    selectedDurationMs = (customHours * 60 + customMinutes) * 60 * 1000L
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minutes")
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 59
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    textColor = android.graphics.Color.WHITE
                                }
                                setOnValueChangedListener { _, _, newVal ->
                                    customMinutes = newVal
                                    if (customHours == 8) {
                                        customMinutes = 0
                                        value = 0
                                    }
                                    selectedDurationMs = (customHours * 60 + customMinutes) * 60 * 1000L
                                }
                            }
                        },
                        update = { picker ->
                            if (customHours == 8 && picker.value != 0) {
                                picker.value = 0
                                customMinutes = 0
                                selectedDurationMs = (8 * 60) * 60 * 1000L
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "WARNING: Once started, this session cannot be paused, cancelled or shortened.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(
                onClick = {
                    val finalDuration = if (isCustom) (customHours * 60 + customMinutes) * 60 * 1000L else selectedDurationMs
                    if (finalDuration > 0) {
                        viewModel.startLock(finalDuration)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("START STRICT LOCK")
            }
        }
        }
    }
}

@Composable
fun ActiveLockScreen(viewModel: MainViewModel, lockEndTime: Long) {
    var remainingTime by remember { mutableStateOf(TimerManager.formatRemainingTime(lockEndTime)) }
    var showExtendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lockEndTime) {
        while (true) {
            remainingTime = TimerManager.formatRemainingTime(lockEndTime)
            delay(1000)
        }
    }

    if (showExtendDialog) {
        AlertDialog(
            onDismissRequest = { showExtendDialog = false },
            title = { Text("Confirm Extension") },
            text = { Text("Do you really want to extend the strict lock timer by 30 minutes? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.extendLock(30 * 60 * 1000L)
                    showExtendDialog = false
                }) {
                    Text("Extend")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExtendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("STRICT LOCK ACTIVE \uD83D\uDD12", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.height(16.dp))
        Text(remainingTime, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = { showExtendDialog = true }) {
            Text("Extend Timer (+30m)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.startAddingToLock() }) {
            Text("Add More Apps to Lock")
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = context.packageManager
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = pm.getApplicationIcon(packageName)
                val bmp = (drawable as? BitmapDrawable)?.bitmap ?: run {
                    val b = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(b)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    b
                }
                bitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color.LightGray))
    }
}

fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
    var accessibilityEnabled = 0
    val service = context.packageName + "/" + accessibilityService.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) { }
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
