package com.example.loop.data

data class LockState(
    val blockedPackages: Set<String> = emptySet(),
    val lockEndTime: Long = 0L
) {
    val isLocked: Boolean
        get() = System.currentTimeMillis() < lockEndTime
}
