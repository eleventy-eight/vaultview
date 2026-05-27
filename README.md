# VaultView

VaultView is a Google TV / Android TV app for browsing and streaming private cloud media.

The initial target provider is MEGA. The codebase is structured around a provider-neutral `StorageProvider` contract so OneDrive, Google Drive, Dropbox, SMB/NAS, or media-server providers can be added without changing the TV UI.

## Current State

This repository contains the first Android project scaffold:

- Kotlin Android app module
- Jetpack Compose UI aimed at D-pad and 10-foot browsing
- Provider-neutral media models and repository
- Demo storage provider for local UI development
- MEGA-first sign-in flow
- MEGA provider boundary with a replaceable SDK client adapter
- Encrypted local MEGA session storage hook
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
2. Add the official MEGA Android SDK dependency, local module, or AAR.
3. Implement `MegaClient` using the MEGA SDK Java/Android bindings.
4. Replace `MissingMegaClient` in `BrowseViewModel.factory` with the real SDK-backed client.
5. Map MEGA nodes into `MegaNode` values for folder browsing.
6. Decide whether MEGA video playback can use direct URLs or needs a local encrypted streaming bridge.

## MEGA SDK Notes

The official MEGA SDK is not currently a simple public Maven dependency. The upstream SDK repository contains Android Java bindings and examples, while the official Android app repository documents a native SDK build flow involving submodules, the Android NDK, and local build steps.

The app isolates this complexity behind `MegaClient`, so the UI and repository code do not need to change when the SDK is wired in.

## Development Notes

The app currently uses the demo provider so the browsing, image viewing, and video playback flow can be developed without cloud credentials.
