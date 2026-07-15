@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("io.github.crowdedlibs.artboard")
}

group = "com.crowdedlibs.cafe"

artboard {
    title.set("Crowded Café")
}

compose.resources {
    packageOfResClass = "com.crowdedlibs.cafe.resources"
}

kotlin {
    android {
        namespace = "com.crowdedlibs.cafe.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        // Compose 1.11's cached native artifacts link against iOS 18.5 on the
        // macOS-15 runner, while this app links its test binary at iOS 15.0.
        // Disable the incompatible cache until CMP-10179 is fixed upstream.
        target.binaries.all {
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_0`,
                reason = "Avoid CMP-10179 cached iOS simulator linker failure",
                issueUrl = URI("https://youtrack.jetbrains.com/issue/CMP-10179"),
            )
        }
        target.binaries.framework {
            baseName = "CafeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
