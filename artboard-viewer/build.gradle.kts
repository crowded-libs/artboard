import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * Artboard's own prebuilt Wasm gallery.
 *
 * Artboard compiles this once and ships the result, so a consumer in snapshot mode
 * browses their previews with the full spatial board without ever declaring a
 * `wasmJs` target themselves. It reads `manifest.json` plus PNGs at runtime rather
 * than linking any consumer code.
 */
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("artboard-viewer")
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":artboard-runtime"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
