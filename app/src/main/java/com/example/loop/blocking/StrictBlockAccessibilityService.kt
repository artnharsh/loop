package com.example.loop.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.loop.data.LockRepository
import com.example.loop.data.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class StrictBlockAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private lateinit var lockRepository: LockRepository
    private var currentLockState: LockState = LockState()

    override fun onServiceConnected() {
        super.onServiceConnected()
        lockRepository = LockRepository(applicationContext)
        lockRepository.lockStateFlow.onEach { state ->
            currentLockState = state
        }.launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (currentLockState.isLocked && currentLockState.blockedPackages.contains(packageName)) {
                // Instantly send them home
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                // Show strict block screen
                val intent = Intent(this, BlockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // Ignored
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
