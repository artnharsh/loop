# Architecture

## Core Components

1. **Jetpack Compose UI (`ui/`)**
   - Renders the user interface.
   - States: App Selection, Timer Setup, Active Lock display.
   - Observes state from `LockRepository`.

2. **Lock Repository (`data/`)**
   - Wraps Preferences DataStore.
   - Single source of truth for:
     - `blockedPackages`: Set of application package names.
     - `lockEndTime`: Long timestamp indicating when the lock expires (0 if inactive).
   
3. **Timer Logic (`timer/`)**
   - Validates user input.
   - Ensures time only increases, never decreases.

4. **Block Enforcement (`blocking/`)**
   - `StrictBlockAccessibilityService`: Uses `AccessibilityService` to listen for `TYPE_WINDOW_STATE_CHANGED`.
   - Checks the current foreground package against `blockedPackages` and compares the current time against `lockEndTime`.
   - Triggers `performGlobalAction(GLOBAL_ACTION_HOME)` and launches a block screen if a violation occurs.

## State Management
- Simple unidirectional flow.
- Repositories expose Kotlin `Flow` which is collected by the UI.

## Persistence
- Datastore is used because it is lightweight, asynchronous, and reliable across process restarts.
