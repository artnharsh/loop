# Module: Persistence

## Purpose
Manage the persistent lock state (deadline and blocked packages).

## Key Files
- `LockRepository.kt`: Wrapper around Preferences DataStore.

## Public Interface
- `val lockStateFlow: Flow<LockState>`
- `suspend fun startLock(packages: Set<String>, endTime: Long)`
- `suspend fun clearLockIfExpired()`

## Invariants
> Active session state survives process recreation and reboot.

## Dependencies
- `androidx.datastore:datastore-preferences`

## Used By
- `ui/`
- `blocking/`
