# VaultView

VaultView is a Google TV / Android TV app for browsing and streaming private cloud media.

The initial target provider is MEGA. The codebase is structured around a provider-neutral `StorageProvider` contract so OneDrive, Google Drive, Dropbox, SMB/NAS, or media-server providers can be added without changing the TV UI.

## Current State

This repository contains the first Android project scaffold:

- Kotlin Android app module
- Jetpack Compose UI aimed at D-pad and 10-foot browsing
- Provider-neutral media models and repository
- Demo storage provider for local UI development
- MEGA provider boundary with TODOs for official SDK integration
- Media3 / ExoPlayer video playback screen
- Full-screen image viewer with left/right D-pad navigation

## Project Shape

```text
app/
  src/main/java/com/vaultview/
    data/
    model/
    playback/
    providers/
      fake/
      mega/
    ui/
```

## Next Implementation Steps

1. Install/open with Android Studio and sync Gradle.
2. Add the official MEGA Android SDK dependency or local module.
3. Replace `FakeStorageProvider` injection with a provider selector and `MegaProvider`.
4. Persist authenticated MEGA sessions with AndroidX Security or MEGA SDK session APIs.
5. Decide whether MEGA video playback can use direct URLs or needs a local encrypted streaming bridge.

## Development Notes

The app currently uses the demo provider so the browsing, image viewing, and video playback flow can be developed without cloud credentials.
