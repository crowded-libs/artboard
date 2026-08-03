# Artboard

[![CI](https://github.com/crowded-libs/artboard/actions/workflows/ci.yml/badge.svg)](https://github.com/crowded-libs/artboard/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.crowded-libs.artboard/artboard-gradle-plugin?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.crowded-libs.artboard/artboard-gradle-plugin)
[![Kotlin 2.4.0](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform 1.11.1](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4?logo=jetbrains&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)

Artboard is a spatial browser gallery for Compose Multiplatform `@Preview`s.
It discovers previews with KSP, hangs them on a pan-and-zoom board, and gives
every frame a stable URL-addressable ID.

[Try the live Crowded Café demo](https://crowded-libs.github.io/artboard/).

![Artboard gallery showing the Crowded Café showcase](artboard_sample.png)

## Features

- Discovers stock Compose `@Preview` annotations, including repeat previews and
  both current and legacy Compose Preview packages.
- **Live gallery** (optional `wasmJs`): fully interactive previews on a
  pan-and-zoom board with mouse, trackpad, one-finger pan, and pinch-zoom.
- **Snapshot gallery** (`jvm` or `android` only): pre-rendered PNG tiles in the
  same board chrome — theme, locale, device, search, deep links, and the screen
  layout-grid overlay work without a Wasm target.
- Organizes frames into Screen and Component zones with search, group, device,
  locale, grid, and light/dark controls.
- Downloads the current state of any live preview as a PNG, with native pixel
  sizes for the built-in device viewports.
- Generates a deterministic registry and JSON report; incompatible previews are
  listed with their reason instead of silently disappearing.
- Never adds platform targets for you. You opt in; Artboard binds to what you
  already declared.
- Exports an optimized, self-contained static gallery for GitHub Pages or any
  other static HTTP host.

## Gallery modes

Artboard picks **one** gallery mode from the targets you already declare
(preference order: live Wasm → JVM snapshots → Android snapshots):

| You declare | Mode | What you get |
|---|---|---|
| `wasmJs { browser() }` | **Live** | Interactive Compose in the browser. Previews must compile for Wasm. |
| `jvm()` (no `wasmJs`) | **Snapshot** | Headless Skia renders every theme × locale to PNGs; a prebuilt viewer browses them. |
| `android { … }` only | **Snapshot** | Same as JVM, but Robolectric host tests render the images. Needs an Android SDK. |

`wasmJs` is **not** required. Without it you still get a spatial gallery of
images with the board chrome (pan/zoom, search, theme, locale, layout grid,
export). You do **not** get live interaction inside a preview, animation scrubbing,
or per-frame PNG capture of a running composition — those need the live path.

## Use Artboard

Artboard releases are published to Maven Central. Add Maven Central to plugin
resolution and apply the plugin:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.crowded-libs.artboard") version "0.2.0"
}
```

### Live gallery (Wasm)

```kotlin
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
}
```

### Snapshot gallery (no Wasm)

Use a target you already have. Previews only need to compile for that target:

```kotlin
// JVM snapshots — fastest local loop, no Android SDK
kotlin {
    jvm()
}

// Or Android-only modules (Robolectric host tests render the tiles)
kotlin {
    android {
        namespace = "com.example.ui"
        compileSdk = 36
        minSdk = 24
    }
}
```

Use stock Compose `@Preview` annotations, including previews declared in
`commonMain`. Artboard adds its KSP processor and runtime only to the gallery
graph; it never adds targets, platform `actual`s, or source-level Artboard APIs
to your application.

```bash
./gradlew :ui:artboardDoctor
./gradlew :ui:artboardReport   # build/reports/artboard/previews.json
./gradlew :ui:artboardRun      # live or snapshot gallery, depending on targets
./gradlew :ui:artboardRunLan   # same build, reachable on the local network
./gradlew :ui:artboardSnapshot # snapshot mode only: PNGs + manifest.json
./gradlew :ui:artboardExport   # build/artboard/export
```

`artboardRun` serves the gallery for whichever mode you are in. In snapshot mode
it runs `artboardSnapshot`, unpacks Artboard's prebuilt viewer, and serves the
assembled board. `artboardExport` produces the optimized production site without
a long-running server. Neither task builds Android or iOS app targets for their
own sake.

## Theme-aware previews

The gallery light/dark control provides Compose's standard system-theme signal
to each preview. Artboard deliberately does not wrap preview content in its own
Material theme, so your application remains responsible for its design system.

To make a preview respond to the gallery toggle, wrap it in your normal app
theme and derive its mode from `isSystemInDarkTheme()`:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme

@Preview(name = "Account")
@Composable
fun AccountPreview() {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        AccountScreen(state = previewState)
    }
}
```

For a **live** gallery the theme wrapper and its dependencies must compile for
`wasmJs`. For a **snapshot** gallery they must compile for the snapshot target
(`jvm` or `android`). A preview that hard-codes light or dark mode remains valid,
but will not react to the gallery theme control.

## Preview environment (IDE parity)

Inside each frame body (not the gallery chrome), Artboard sets
`LocalInspectionMode` to `true`, the same signal IDE Compose previews provide.
Code that already branches on inspection mode — sample drawables instead of
network images, placeholders, skipped side effects — behaves on the board the
way it does in Android Studio / IDEA. Gallery toolbar, board, and menus stay
outside inspection mode so the host remains a normal interactive UI.

## Layout grid on screens

The toolbar **Grid** control toggles a Figma-style column / margin / gutter
overlay on every **Screen** frame. It is chrome painted above the frame body, so
it works for live composables and for snapshot PNG tiles the same way. PNG
download (live mode) still excludes the overlay so store artwork stays clean.

## Download preview images

In the **live** gallery, each frame has a `PNG ↓` action that captures the
preview body exactly as it is currently composed. Camera position and zoom do
not affect the image, and Artboard chrome, selection marks, and layout grids are
excluded.

Screen previews matching a built-in device viewport download at that device's
native pixel size and are flattened to an opaque background. Other screens and
components use a 2× logical-size fallback; component transparency is preserved.
Theme, locale, interaction state, and the current animation frame are included
in the capture.

In **snapshot** mode the board already *is* the image set: every theme × locale
variant was rendered by `artboardSnapshot`. Use the locale and theme controls to
switch tiles; per-frame download is a live-gallery feature.

Native-sized images are convenient for store artwork, but should still be
checked against the Android or iOS app before submission when platform rendering
details matter.

## Develop Artboard

Requirements: JDK 17+, a WasmGC-capable browser for live gallery work, Android
SDK for Android snapshot samples and the café Android app, and Xcode for iOS
showcase work.

```bash
# Core tests, runtime Wasm, and the prebuilt snapshot viewer jar
./gradlew test :artboard-runtime:jvmTest :artboard-runtime:compileKotlinWasmJs :artboard-viewer-dist:jar

# Live Wasm consumer contract
./gradlew -p samples/minimal artboardDoctor artboardReport compileKotlinWasmJs

# JVM snapshot consumer (no wasmJs)
./gradlew -p samples/light artboardDoctor artboardReport artboardSnapshot artboardExport

# Android-only snapshot consumer (no wasmJs / jvm)
./gradlew -p samples/android-light artboardDoctor artboardReport artboardSnapshot artboardExport

# Café gallery, Android, and iOS verification
./gradlew -p showcase/cafe :shared:artboardExport
./gradlew -p showcase/cafe :androidApp:assembleDebug
./gradlew -p showcase/cafe :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

For UI or host changes, run the relevant `artboardRun`, open its printed URL,
check the browser console, and exercise the changed control (including **Grid**
on a Screen frame in snapshot mode). Keep screenshots and verification artifacts
under `/tmp`, never in the repository.

## License

Artboard is licensed under [Apache-2.0](LICENSE). Bundled font notices are in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
