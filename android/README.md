# ClipMind Android

📱 **Phase 1.** This directory will hold the Kotlin + Compose app. To start:

1. Open Android Studio (Hedgehog or newer).
2. `File → New → New Project → Empty Activity`. Set the project location to
   `clipmind/android/`.
3. Configure: Kotlin, min SDK 26, target SDK 34, Compose enabled.
4. Add Hilt, Room, ExoPlayer, Coil, and Coroutines dependencies.

See [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for the architecture you're targeting.

## Phase 1 acceptance criterion

> Import a video from device storage → it appears in the library → tap it → ExoPlayer plays it.

No backend, no AI, no auth. Just the local round-trip.
