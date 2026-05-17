# iOS-Style Android Screen Recorder

A fully native Android screen recorder built with Kotlin, using the MediaProjection API. It's designed to be clean, powerful, and match the simplicity of the iOS built-in recorder.

## Features
- **Core Recording Engine**: Uses MediaProjection, VirtualDisplay, and MediaRecorder/MediaCodec under the hood. No root required.
- **Background Support**: Runs as a Foreground Service, continuing to record while gaming or switching apps.
- **Custom Settings UI**: Dropdown selections for Resolution, FPS slider, Bitrate, Orientation mode, Audio source, and Video Encoder formats.
- **Quick Settings Tile**: Start and stop recording directly from the Android notification shade using a custom `TileService`.
- **Persistent Storage**: Utilizes `SharedPreferences` to remember user choices.
- **Storage Access Framework (SAF)**: Let the user choose exactly where recordings should be saved.

## Setup & Building
1. Open this project folder in **Android Studio**.
2. Sync Gradle dependencies.
3. Build and run on a device with Android 8.0 (API 26) or higher.

## Android 15 (API 35) Known Changes
In Android 15, the `MediaProjection` API has introduced stricter security rules:
- Users may be required to choose between recording the whole screen or just a specific application window.
- Persisting permissions across sessions is blocked. The user must grant the MediaProjection permission **every** time the app starts recording. Our implementation handles this by checking for an active token and re-prompting if necessary.

## Permissions Explained
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Required to capture the screen in the background without being killed.
- `RECORD_AUDIO`: To capture from the microphone or internal audio (API 29+).
- `POST_NOTIFICATIONS`: To show the persistent recording notification (Android 13+).
