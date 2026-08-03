# Working on Artboard

@/Users/corey/.codex/RTK.md

Artboard is a spatial gallery of Compose Multiplatform `@Preview`s. The root
build contains `artboard-runtime`, `artboard-codegen`, `artboard-gradle-plugin`,
`artboard-viewer`, and `artboard-viewer-dist`. Independent consumers:

- `samples/minimal` — live Wasm gallery (fast contract testbed)
- `samples/light` — JVM snapshot gallery (no `wasmJs`)
- `samples/android-light` — Android-only snapshot gallery (no `wasmJs` / `jvm`)
- `showcase/cafe` — Wasm gallery plus Android/iOS product app

## Commands

```bash
./gradlew test :artboard-runtime:jvmTest :artboard-runtime:compileKotlinWasmJs :artboard-viewer-dist:jar
./gradlew -p samples/minimal artboardDoctor artboardReport compileKotlinWasmJs
./gradlew -p samples/minimal artboardRun
./gradlew -p samples/light artboardDoctor artboardReport artboardSnapshot artboardExport
./gradlew -p samples/android-light artboardDoctor artboardReport artboardSnapshot artboardExport
./gradlew -p showcase/cafe :shared:artboardExport
./gradlew -p showcase/cafe :shared:artboardRunLan
./gradlew -p showcase/cafe :androidApp:assembleDebug
./gradlew -p showcase/cafe :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

Run `artboardRun` and inspect the browser whenever gallery UI or host behavior
changes (live *and* snapshot). Keep screenshots and verification dumps in
`/tmp`. Android SDK is required for root `artboard-runtime` (android target),
`samples/android-light`, and café Android.

## Rules

- `@Preview` is the catalog API; support current and legacy Compose Preview FQCNs.
- Stable frame IDs derive from FQCN plus preview name.
- Consumers apply only `io.github.crowded-libs.artboard`; never require source imports,
  manual registries, KSP declarations, Artboard dependencies, or generated `actual`s.
- Never add Wasm or other targets for a consumer. Targets are explicit opt-in.
  Prefer live Wasm when present; otherwise bind to `jvm` or `android` snapshot mode.
- Gallery work stays commonMain-first; live gallery needs a consumer-declared
  `wasmJs` target, snapshot galleries do not.
- `commonMain` previews are supported; their dependency graph must compile for the
  chosen gallery target, and failed discovery must be reported rather than hidden.
- Screen layout-grid overlay is board chrome and must work for live bodies and
  snapshot image tiles.
- Use official Kotlin style, immutable models, KDoc on public APIs, and Material3 for chrome.
