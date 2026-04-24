# Workout Clock

A specialized minimalist timer application for Android designed for high-intensity interval training or any activity requiring precise 30-second audio cycles.

## Features

- **Dynamic Audio Cycle (30 Seconds):**
  - **01–13s:** Short beeps (392 Hz - G4) for pacing.
  - **14–15s:** 2-second continuous long tone (440 Hz - A4).
  - **16–25s:** Silent period for recovery or focus.
  - **26–28s:** 3 short beeps (523 Hz - C5).
  - **29–30s:** 2-second continuous long tone (659 Hz - E5).
- **Smoothed Audio:** High-quality sine wave generation with 50ms fade-in/fade-out to eliminate clicking noises.
- **Minimalist Aesthetic:** 
  - **Dark Mode:** Easy on the eyes in low-light environments.
  - **Always On:** Keeps the screen from sleeping while the app is active.
  - **Status Indicator:** Large circular button that fills with dark green when running and clears when paused.
- **Cumulative Timer:** Large, bold display of total elapsed time in `MM:SS` format.
- **Standard Volume Control:** Uses `USAGE_MEDIA` to respond directly to your phone's volume buttons.

## Layout & Dimensions

The UI is built using Jetpack Compose with the following precise measurements to ensure high visibility and ease of use during workouts:

- **Main Container:** 16dp padding on all sides, vertical centering.
- **Main Status Button (Circle):**
  - **Diameter:** 240dp
  - **Border:** 3dp (Light Green: #90EE90)
  - **Fill:** Dark Green (#006400) when running, Transparent when paused.
- **Time Display:**
  - **Font Size:** 56sp, Bold
  - **Spacing:** 60dp top margin (distance from main button).
- **Exit Button:**
  - **Dimensions:** 120dp wide x 50dp high
  - **Border:** 1dp (Light Green with 70% opacity)
  - **Label Font Size:** 18sp
  - **Spacing:** 40dp top margin (distance from time display).

## Technical Details

- **UI Framework:** Jetpack Compose with Material 3.
- **Audio Engine:** Low-level `AudioTrack` API for precise frequency control and latency.
- **Minimum SDK:** 26 (Android 8.0).
- **Target SDK:** 34 (Android 14).
- **Build Optimization:** R8 enabled for a compact APK size.

## Installation

1. Download the `app-release.apk` from the latest build.
2. Transfer the APK to your Android device.
3. Since the APK is signed for testing, it should install directly on most modern devices (like Galaxy S22).
4. Open and install the APK.

## Development

Built using Android Studio with Kotlin and Jetpack Compose.
