# SmartIsland — Development Guide

## Project Overview
SmartIsland is a native Android overlay application that provides a Dynamic Island-style interface for system interactions and notifications. Built with Kotlin and Jetpack Compose.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Hilt DI
- **Persistence:** DataStore (settings + stats), SQLite (notification history)
- **Min SDK:** Check `app/build.gradle.kts` for `minSdk`
- **Build:** Gradle with Kotlin DSL

## Project Structure
```
app/src/main/java/com/agupta07505/smartisland/
├── data/          # Repositories, DataStore settings, notification history
├── di/            # Hilt DI modules and entry points
├── model/         # Domain models (IslandMode, IslandNotification, IslandPet)
├── service/       # AccessibilityService (overlay), NotificationListenerService
├── ui/            # Compose UI (overlay, home screen, sections, pet)
└── util/          # Parsers (LiveActivity, Navigation, Timer), utilities
```

## Key Features
- Dynamic Island overlay with expand/collapse animations
- Virtual pet that reacts to system events (charging, music, bluetooth, etc.)
- Notification statistics & insights dashboard
- Parsers for live activities, navigation, timers across many apps
- Customizable colors, positions, gestures

## Building
This is an Android project — it requires the Android SDK and Gradle to build. It cannot run in a web browser. Use Android Studio or `./gradlew assembleDebug` with the Android SDK installed.

## Testing
Unit tests are in `app/src/test/` and instrumented tests in `app/src/androidTest/`. Run with `./gradlew test` (unit) or `./gradlew connectedAndroidTest` (instrumented, requires emulator/device).

## Notes
- The app uses an AccessibilityService for the overlay and a NotificationListenerService for notifications
- Hilt is used for DI; `SmartIslandRepositories` provides entry-point access for non-Hilt contexts
- Settings are stored in DataStore; notification history in SQLite
- The virtual pet feature is controlled by `enablePet`, `petColor`, and `petInsideIsland` settings
