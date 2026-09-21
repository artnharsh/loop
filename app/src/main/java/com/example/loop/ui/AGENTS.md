# Module: UI

## Purpose
Present the App Selection, Timer Configuration, and Active Lock screens.

## Key Files
- `MainActivity.kt`: The main entry point hosting Jetpack Compose content.
- `StrictBlockApp.kt`: The Compose navigation and root UI.

## Flow
```text
SELECT APPS
    ↓
SET TIMER
    ↓
CONFIRM
    ↓
ACTIVE LOCK
    ↓
EXPIRED
```

## Invariants
> During ACTIVE LOCK, never expose controls for cancelling, pausing, taking a break, removing apps, or shortening the timer.

## Dependencies
- `data/LockRepository`
- Jetpack Compose Material 3

## Modification Rules
- UI must remain minimal. No gamification, no ads, no complex dashboards.
