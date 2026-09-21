# loop

`loop` is a minimal, strict Android application that allows users to block distracting applications for a specific duration. Once a lock starts, the application provides **no way** to pause, cancel, or shorten the timer.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Storage**: Preferences DataStore
- **Core Mechanism**: AccessibilityService (for detecting foreground apps and blocking)

## Repository Structure
- `app/src/main/java/com/example/loop/blocking/` - Foreground app detection and block enforcement.
- `app/src/main/java/com/example/loop/timer/` - Deadline calculation and validation.
- `app/src/main/java/com/example/loop/data/` - Persistence of lock state.
- `app/src/main/java/com/example/loop/ui/` - Jetpack Compose UI (App Selection, Timer, Lock Screen).
- `docs/` - Architectural documentation and decisions.

## Architecture
The application uses a simple unidirectional data flow. `LockRepository` reads/writes the lock state (blocked packages and deadline) from DataStore. The UI layers read this state to present the correct screen. The `StrictBlockAccessibilityService` reads this state to determine whether to block the current foreground app.

## Development Commands
- **Build**: `./gradlew assembleDebug`
- **Test**: `./gradlew test`
- **Lint**: `./gradlew lint`

## Project Invariants
- **No Early Exit**: Active locks can never be shortened or bypassed.
- **Safety**: Critical system applications (Settings, System UI, Dialer) and the app itself must NEVER be blockable.
- **Persistence**: Active session state must survive process recreation and device reboot.

## Security Rules
- Local processing only. No network access. No analytics.

## Testing Rules
- Business logic (deadline calculation, validation) should be independently testable.
- Boundary conditions (time edge cases) must be tested.

## Definition of Done
- Feature works as described.
- Edge cases (like rebooting) handled correctly.
- UI looks minimal and functional.
- Code follows project invariants.

## Module Context Map
| Area | Context |
|---|---|
| Blocking | `app/src/main/java/com/example/loop/blocking/AGENTS.md` |
| Timer | `app/src/main/java/com/example/loop/timer/AGENTS.md` |
| Persistence | `app/src/main/java/com/example/loop/data/AGENTS.md` |
| UI | `app/src/main/java/com/example/loop/ui/AGENTS.md` |
