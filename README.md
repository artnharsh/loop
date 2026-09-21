# loop

A minimal, uncompromising Android app to block distracting applications.

## Features
- Select distracting apps to block.
- Set a timer (e.g., 15m, 30m, 1h, 2h).
- **Strict Mode**: Once the timer starts, there is absolutely no way to cancel, pause, or bypass the lock until the time expires.
- Survives device reboots.

## Requirements
- Android 10+ (API level 29+)
- Accessibility Services permission

## Build Instructions
1. Clone the repository.
2. Build the APK using Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```

## Running
Install the built APK on your device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then grant the Accessibility permission in your device's settings to allow the app to detect and block selected applications.
