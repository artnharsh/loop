package com.example.loop.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_lock_prefs")

class LockRepository(private val context: Context) {

    private val BLOCKED_PACKAGES_KEY = stringSetPreferencesKey("blocked_packages")
    private val LOCK_END_TIME_KEY = longPreferencesKey("lock_end_time")

    val lockStateFlow: Flow<LockState> = context.dataStore.data.map { preferences ->
        LockState(
            blockedPackages = preferences[BLOCKED_PACKAGES_KEY] ?: emptySet(),
            lockEndTime = preferences[LOCK_END_TIME_KEY] ?: 0L
        )
    }

    suspend fun startLock(packages: Set<String>, durationMs: Long) {
        val newEndTime = System.currentTimeMillis() + durationMs
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_PACKAGES_KEY] = packages
            // Ensure we never decrease an active deadline
            val currentEndTime = preferences[LOCK_END_TIME_KEY] ?: 0L
            if (newEndTime > currentEndTime) {
                preferences[LOCK_END_TIME_KEY] = newEndTime
            }
        }
    }

    suspend fun extendLock(additionalDurationMs: Long) {
        context.dataStore.edit { preferences ->
            val currentEndTime = preferences[LOCK_END_TIME_KEY] ?: 0L
            // If the lock hasn't expired, we add to it, else we start from now
            val baseTime = if (currentEndTime > System.currentTimeMillis()) currentEndTime else System.currentTimeMillis()
            preferences[LOCK_END_TIME_KEY] = baseTime + additionalDurationMs
        }
    }

    suspend fun clearLockIfExpired() {
        context.dataStore.edit { preferences ->
            val currentEndTime = preferences[LOCK_END_TIME_KEY] ?: 0L
            if (System.currentTimeMillis() >= currentEndTime) {
                preferences.remove(BLOCKED_PACKAGES_KEY)
                preferences.remove(LOCK_END_TIME_KEY)
            }
        }
    }
}
