# Module: Blocking

## Purpose
Enforce the strict lock by detecting the current foreground application and preventing access if it is blocked.

## Key Files
- `StrictBlockAccessibilityService.kt`: The AccessibilityService that listens for window state changes.
- `BlockScreenActivity.kt`: The simple activity displayed when an app is blocked.

## Public Interface
- Bound by the Android system via Accessibility Service APIs.

## Invariants
> Blocking logic must never expose an early unlock.
> Must handle edge cases where the Accessibility Service may crash or restart.

## Dependencies
- `data/LockRepository`
- Android `AccessibilityService` APIs

## Modification Rules
- Keep the `onAccessibilityEvent` callback extremely lightweight as it fires constantly.
- Never add a bypass button in the block screen.
