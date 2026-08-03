plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("io.github.crowded-libs.artboard")
}

group = "io.github.crowded-libs.artboard.samples"

compose.resources {
    packageOfResClass = "artboard.sample.androidlight.resources"
}

// Android is the only target. No wasmJs, no jvm — Artboard must bind here or not
// at all, which is the case this sample exists to prove.
kotlin {
    android {
        namespace = "artboard.sample.androidlight"
        compileSdk = 36
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}
