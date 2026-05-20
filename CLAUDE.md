# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**here** (package `net.tosak.here`) is a proximity-based social Android app — "Presence over content. Moments over feeds." Users opt into location visibility, see friends within a 400m radius, and post ephemeral content that expires on exit. Everything is ephemeral and privacy-first.

Built with Kotlin + Jetpack Compose. Targets SDK 36, min SDK 24.

## Build & Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device/emulator
./gradlew installDebug

# Compile only (fast error check)
./gradlew :app:compileDebugKotlin

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "net.tosak.here.ExampleUnitTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| Font | JetBrains Mono via Google Fonts (`ui-text-google-fonts`) |
| Build system | Gradle 8.13 with Kotlin DSL (`.gradle.kts`) |
| Android Gradle Plugin | 8.13.2 |
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 36 |

Dependency versions are managed centrally in `gradle/libs.versions.toml` (version catalog).

## Design System — Ember Theme

All colours are defined in `ui/theme/Color.kt`:

| Token | Value | Use |
|-------|-------|-----|
| `EmberBg` | `#0a0a0a` | All screen backgrounds |
| `EmberFg` | `#f4f4f4` | Primary text, borders, icons |
| `EmberMuted` | `#f4f4f4` @ 40% | Secondary text, hints |
| `EmberAccent` | `#ff5a3c` | CTA shadows, progress arc, blinking badges |
| `EmberSurface` | `#141414` | Card surfaces |
| `EmberBorder` | `#f4f4f4` @ 20% | Subtle dividers |

Font is JetBrains Mono everywhere (set as default in `Typography`). All labels are uppercase/monospace — use the `Mono()` composable for labels, not plain `Text`.

## Architecture & Code Structure

```
app/src/main/java/net/tosak/here/
├── MainActivity.kt                  # Single activity; launches ProximityApp
├── model/
│   └── ProximityModels.kt           # Data classes: Friend, FriendPost, AppScreen enum, sampleFriends
├── ui/
│   ├── ProximityApp.kt              # Root composable — nav state machine + toast + ping layer
│   ├── theme/
│   │   ├── Color.kt                 # Ember palette tokens
│   │   ├── Theme.kt                 # HereTheme (always dark, no dynamic colour)
│   │   └── Type.kt                  # JetBrains Mono font family + Typography
│   ├── components/
│   │   ├── Components.kt            # Shared: Mono, HudStrip, StatusChip, AmbientReadout, PxButton, Rule
│   │   ├── MapCanvas.kt             # SchematicMap — Canvas-drawn Debar Maalo street network
│   │   └── HoldGesture.kt           # 3-second press-and-hold presence button (progress arc)
│   └── screens/
│       ├── OnboardingScreen.kt      # 4 philosophy panels → handle picker
│       ├── MapScreen.kt             # Map home with empty/cluster states
│       ├── PresenceScreen.kt        # Go-live screen with HoldGesture
│       ├── ComposerScreen.kt        # Post kind picker + Photo/Text/Voice composers
│       ├── PostViewScreen.kt        # Single post fullscreen with quick-reply chips
│       ├── ChatScreen.kt            # Minimal coordination thread (no history/receipts)
│       ├── SettingsScreen.kt        # Privacy-first settings (data: 3KB device / 0KB server)
│       └── PingOverlay.kt           # Modal ping card with ember shadow + blinking badge
```

### Navigation model

`ProximityApp.kt` holds all app state (screen, presenceOn, handle, activeFriend, ping) using `remember {}` — no Navigation library. Screen transitions use `AnimatedContent` with fade. The `AppScreen` enum in `ProximityModels.kt` lists all screens.

### Map

`SchematicMap` in `MapCanvas.kt` uses Compose `Canvas` to draw:
- `STREETS` list — semi-irregular paths modelling Debar Maalo, Skopje
- Range circle with `rangeGlow` radial gradient  
- "You" marker with crosshair + animated pulse ring
- `FriendMarker` squares with "just posted" ring animation

All coordinates are in a 380×680 virtual space, scaled to the actual Canvas size at draw time.

### Presence activation (HoldGesture)

`HoldGesture.kt` tracks `pointerInput` with `awaitPointerEventScope` + a coroutine that advances `progress` every 16ms over 3000ms. Progress drives a `drawArc` sweep on a circular progress ring. Releasing early cancels the coroutine and snaps progress back to 0. Haptic feedback fires on press-down and on completion.