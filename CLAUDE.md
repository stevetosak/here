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

The project is **MVVM + Hilt** with a **feature-based package layout**. Each screen owns its `viewmodel/` (and optionally `components/`); cross-cutting concerns live under `shared/`.

```
app/src/main/java/net/tosak/here/
├── MainActivity.kt                  # Single @AndroidEntryPoint activity; launches ProximityApp, handles ping deep-links + POST_NOTIFICATIONS
├── HereApplication.kt               # @HiltAndroidApp
├── di/                              # Hilt modules (DatabaseModule, LocationModule, …)
├── ui/
│   ├── ProximityApp.kt              # Root composable — AnimatedContent router over AppScreen + toast + incoming-ping overlay
│   └── theme/                       # Color.kt (Ember palette), Theme.kt, Type.kt (JetBrains Mono)
├── screens/<feature>/               # e.g. mapscreen/, presence/, composer/, post/, chat/, dm/, friends/,
│   │                                #      friendprofile/, settings/, handshake/, onboarding/, ping/
│   ├── <Feature>Screen.kt           # display-only Composable
│   ├── viewmodel/                   # @HiltViewModel — all logic/state lives here
│   └── components/                  # screen-specific composables
└── shared/
    ├── model/ProximityModels.kt     # Friend, FriendPost, AppScreen enum, anchoredSampleFriends
    ├── navigation/NavigationViewModel.kt   # back-stack (Activity-scoped)
    ├── state/AppStateRepository.kt  # cross-screen state: activeFriend, chatSeed, pendingMemento
    ├── events/EventBus.kt           # Event.Nav / .AppState / .Toast / .Auth / .Ping
    ├── storage/                     # Room (HereDatabase + entities/DAOs) + repositories + AppStorage (SharedPreferences)
    ├── ping/                        # PingModels, ProximityRepository, PingRepository, PingSettingsRepository, PingEngine, IncomingPingController
    ├── notifications/               # PingNotifier, PingActionReceiver
    ├── location/, auth/, permissions/, components/   # Components.kt: Mono, PxButton, Rule, BackButton, HudStrip, SettingToggle, SettingRow
```

### Navigation model (event-driven, no Navigation library)

Screens never receive callbacks. They emit `Event.Nav.*` / `Event.AppState.*` / `Event.Toast.*` via the singleton **`EventBus`**. `NavigationViewModel` (Activity-scoped) owns the back-stack (`SnapshotStateList<AppScreen>`) and applies `Event.Nav`; `AppStateRepository` owns cross-screen data (`activeFriend`, `chatSeed`, `pendingMemento`). `ProximityApp.kt` reads these flows and renders the current screen via `AnimatedContent` (fade). The `AppScreen` enum lives in `shared/model/ProximityModels.kt`.

### Persistence

Room database `HereDatabase` (`shared/storage/`) holds posts, DM messages, friends, and per-friend ping settings; repositories wrap the DAOs. `AppStorage` wraps SharedPreferences (handle, auth token, presence flag, global ping pause + active-hours).

### Map (Mapbox)

`SchematicMap` in `screens/mapscreen/MapCanvas.kt` renders a **Mapbox** map (`MapboxMap` + `ViewAnnotation` markers); pan/pinch enabled, rotate/pitch/zoom-out locked, camera bounded to the 400 m radius. A Compose `Canvas` overlay (`MapOverlayCanvas`) draws the zoom-scaled range rings + vignette. The "You" marker and `FriendMarkerView` markers are `ViewAnnotation`s anchored to real WGS-84 coordinates. Live friends within radius are shown whenever presence is on; tapping a friend marker opens the quick-action sheet (chat / ping).

### Pings

Two modes (`shared/ping/`): **auto** (per-friend toggle in the Friend Profile; fires when a friend enters radius, gated by 2h cooldown, global active-hours window, presence, and a global pause) and **manual** (the map quick-action sheet; optional ≤40-char intent, anti-spam cooldown). `ProximityRepository` tracks who's in radius (and runs a demo arrival simulation); `PingEngine` watches arrivals and emits `Event.Ping.Incoming` for auto pings (coalescing near-simultaneous arrivals); `IncomingPingController` drives the in-app `PingOverlay`; `PingNotifier` posts the system notification with "I'm on my way" / "Ignore" actions (`PingActionReceiver`). No backend — sends are mocked; incoming pings are demoed via a Settings debug trigger and the timed arrival sim.

### Presence activation (HoldGesture)

`HoldGesture.kt` tracks `pointerInput` with `awaitPointerEventScope` + a coroutine that advances `progress` every 16ms over 3000ms. Progress drives a `drawArc` sweep on a circular progress ring. Releasing early cancels the coroutine and snaps progress back to 0. Haptic feedback fires on press-down and on completion.