# Sleep Timer – Jetpack Compose & Foreground Service

A fully native Android app that lets you drag around a circular dial to set up to 120 minutes, start a sleep timer, and automatically mute/pause media when time is up. The timer keeps running in the background via a foreground service and surfaces status through persistent notifications.

## Project structure

```
android/
 ├─ app/src/main/java/com/anonymous/sleep_timer/
 │   ├─ MainActivity.kt          # Jetpack Compose UI + timer state
 │   ├─ sleep/SleepTimerService.kt
 │   ├─ volume/VolumeActions.kt  # Volume + media-focus helpers
 │   └─ ui/theme/...             # Compose theme/colors
 └─ app/src/main/res/...         # Compose-friendly resources (strings/colors/icons)
README.md
```

All former React Native / Expo files were removed. This repo now builds like any standard Android project.

## Requirements

- Android Studio Ladybug or later
- JDK 17
- Android SDKs / build-tools API 34 (installed automatically by Android Studio)
- NDK 29.0.14206865 (already configured in `gradle.properties`)

## Build & run

```bash
cd android
./gradlew assembleDebug   # or installDebug / connectedAndroidTest
```

To run from Android Studio:
1. Open the `android/` folder as a project.
2. Sync Gradle.
3. Use **Run > Run 'app'** to install on a device/emulator.

> **Android 13+ notification permission:** the first launch will request POST_NOTIFICATIONS so the foreground notification is visible. If you deny it, grant it later from Settings ▸ Apps ▸ Sleep Timer ▸ Notifications.

## Features

- Jetpack Compose dial UI with drag gestures, real-time countdown, and pause/resume controls.
- Foreground service (`SleepTimerService`) keeps the countdown alive while the app is backgrounded.
- System notification updates remaining time and signals completion with toast + notification.
- Media stream is muted, audio focus is toggled, and a media-pause key event is dispatched to encourage third-party players to stop.

## Extending the app

- Customize the dial visuals or typography inside `MainActivity.kt`.
- Add notification actions (Pause/Cancel) in `SleepTimerService`.
- Persist timer settings with DataStore or Room if you want cross-session defaults.
- Add instrumentation tests under `android/app/src/androidTest` for service behavior.

## License

This project is owned by the original author. Use internally unless a license is added.
