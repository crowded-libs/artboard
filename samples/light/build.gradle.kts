plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("io.github.crowded-libs.artboard")
}

group = "io.github.crowded-libs.artboard.samples"

compose.resources {
    packageOfResClass = "artboard.sample.light.resources"
}

// Deliberately declares no wasmJs target. Artboard binds to the jvm target this
// module already has and renders snapshot images instead of a browser bundle.
kotlin {
    jvm()

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
