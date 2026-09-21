package com.example.loop.timer

object TimerManager {

    /**
     * Calculates the absolute timestamp for when a lock should expire.
     * 
     * @param durationMs The duration to lock for in milliseconds.
     * @return The absolute timestamp (System.currentTimeMillis() + durationMs).
     */
    fun calculateNewDeadline(durationMs: Long): Long {
        return System.currentTimeMillis() + durationMs
    }

    /**
     * Extends an active deadline by the given duration.
     * If the current deadline is in the past, starts from now.
     */
    fun calculateExtendedDeadline(currentDeadlineMs: Long, additionalDurationMs: Long): Long {
        val baseTime = if (currentDeadlineMs > System.currentTimeMillis()) {
            currentDeadlineMs
        } else {
            System.currentTimeMillis()
        }
        return baseTime + additionalDurationMs
    }
    
    fun formatRemainingTime(endTimeMs: Long): String {
        val remainingMs = endTimeMs - System.currentTimeMillis()
        if (remainingMs <= 0) return "00:00"
        val seconds = (remainingMs / 1000) % 60
        val minutes = (remainingMs / (1000 * 60)) % 60
        val hours = (remainingMs / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%dh %02dm", hours, minutes)
        } else {
            String.format("%02dm %02ds", minutes, seconds)
        }
    }
}
