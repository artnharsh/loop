package com.example.loop.blocking

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.example.loop.data.LockRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StrictLockAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        // We must check if there is an active lock. If so, we block disabling it.
        val lockRepository = LockRepository(context)
        
        // We use runBlocking here because onDisableRequested requires a synchronous return.
        // Reading DataStore synchronously in a receiver is acceptable because the file is usually already cached.
        val lockState = runBlocking { lockRepository.lockStateFlow.first() }

        if (lockState.isLocked) {
            // Launch the block screen to force them out of the settings entirely!
            val blockIntent = Intent(context, BlockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(blockIntent)

            // Also return this warning which Android will show (if they manage to click it anyway).
            return "STRICT LOCK ACTIVE! You cannot disable protection until the timer finishes."
        }

        return super.onDisableRequested(context, intent)
    }
}
