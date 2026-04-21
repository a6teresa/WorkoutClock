# TickClockApp

A specialized timer application for Android that generates precise audio tones in a repeating 30-second cycle.

## Features

- **Audio Cycle (30 Seconds):**
  - **01–13s:** Short beeps (392 Hz - G4).
  - **14–15s:** One continuous long tone (440 Hz - A4).
  - **16–25s:** Silent period.
  - **26–28s:** 3 short beeps (392 Hz - G4).
  - **29–30s:** One continuous long tone (440 Hz - A4).
- **Cumulative Timer:** Displays total elapsed time in `MM:SS` format.
- **Controls:** 
  - **Start/Pause:** Toggle to start or pause the timer and audio.
  - **Exit:** Closes the application.
- **High Volume Output:** Optimized using `AudioTrack` with `USAGE_ALARM` and maximum volume settings for clarity.
- **Custom Icon:** Features an adaptive icon designed for the timing sequence.

## Technical Details

- **UI Framework:** Jetpack Compose with Material 3.
- **Audio Engine:** Programmatic sine wave generation using `AudioTrack` for exact frequency control (392 Hz and 440 Hz).
- **Minimum SDK:** 26 (Android 8.0).
- **Target SDK:** 34 (Android 14).

## Installation

1. Download the `app-release-unsigned.apk` from the `build` outputs.
2. Transfer the APK to your Android device.
3. Enable "Install from Unknown Sources" in your device settings.
4. Open and install the APK.

## Development

Built using Android Studio with Kotlin and Jetpack Compose.
