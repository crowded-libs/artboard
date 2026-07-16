# Working on Artboard

@/Users/corey/.codex/RTK.md

Artboard is a Kotlin/Wasm spatial gallery of Compose Multiplatform `@Preview`s.
The root build contains `artboard-runtime`, `artboard-codegen`, and
`artboard-gradle-plugin`. Independent consumers are `samples/minimal` (fast
Wasm testbed) and `showcase/cafe` (Wasm gallery plus Android/iOS product app).

## Commands

```bash
./gradlew test :artboard-runtime:jvmTest :artboard-runtime:compileKotlinWasmJs
./gradlew -p samples/minimal artboardDoctor artboardReport compileKotlinWasmJs
./gradlew -p samples/minimal artboardRun
./gradlew -p showcase/cafe :shared:artboardExport
./gradlew -p showcase/cafe :shared:artboardRunLan
./gradlew -p showcase/cafe :androidApp:assembleDebug
./gradlew -p showcase/cafe :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

Run `artboardRun` and inspect the browser whenever gallery UI or host behavior
changes. Keep screenshots and verification dumps in `/tmp`.

## Rules

- `@Preview` is the catalog API; support current and legacy Compose Preview FQCNs.
- Stable frame IDs derive from FQCN plus preview name.
- Consumers apply only `io.github.crowded-libs.artboard`; never require source imports,
  manual registries, KSP declarations, Artboard dependencies, or generated `actual`s.
- Never add Wasm or other targets for a consumer. Wasm is explicit opt-in.
- Gallery work stays commonMain/Wasm-first; Android and iOS are Café product targets.
- `commonMain` previews are supported; their dependency graph must compile for the opted-in
  Wasm gallery target, and failed discovery must be reported rather than hidden.
- Use official Kotlin style, immutable models, KDoc on public APIs, and Material3 for chrome.
