# Smart Island — Base44 Dev Environment

## Project Type
Android app (Kotlin + Jetpack Compose). No web server — builds produce an APK, not a preview URL.

## Build & Verify
```bash
docker compose -f docker-compose.base44.yml run --rm builder ./gradlew compileDebugKotlin --no-daemon -x lint
```
- `Dockerfile.base44` provides JDK 17 + Android SDK (compileSdk 36, build-tools 36.0.0).
- `gradlew` must be executable (`chmod +x gradlew`).
- Gradle cache is persisted in the `gradle-cache` volume.

## Architecture Notes
- Hilt for DI; `SmartIslandOverlayService` (AccessibilityService) renders the dynamic island overlay.
- Settings persisted via DataStore (`SmartIslandSettings` / `SmartIslandSettingsRepository`).
- Virtual pet (`PixiPetOverlay`) is a separate overlay window with its own lifecycle owners.
- Night mode for the pet is computed inside the Composable (checks time every 60s).
