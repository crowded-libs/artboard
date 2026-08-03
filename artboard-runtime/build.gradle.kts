import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "artboard-runtime", version.toString())

    pom {
        name = "Artboard Runtime"
        description = "Runtime for the Artboard spatial Compose Multiplatform preview gallery"
        url = "https://github.com/crowded-libs/artboard"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id.set("coreykaylor")
                name.set("Corey Kaylor")
                email.set("corey@kaylors.net")
            }
        }
        scm {
            url = "https://github.com/crowded-libs/artboard"
            connection = "scm:git:git://github.com/crowded-libs/artboard.git"
            developerConnection = "scm:git:ssh://github.com/crowded-libs/artboard.git"
        }
    }
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "artboard.resources"
    }
}

kotlin {
    // Two orthogonal groupings, so jvmMain draws from both:
    //  - jvmShared: java.* seams (locale, preferences) that Android also has.
    //  - skiko: APIs absent on Android, notably LocalSystemTheme.
    // Declared through the hierarchy template rather than raw dependsOn edges, which
    // would suppress the default template and warn.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                // AGP's KMP library plugin registers its target as "android"; the
                // built-in withAndroidTarget() only matches the legacy androidTarget().
                withCompilations { it.target.name == "android" }
            }
            group("skiko") {
                withJvm()
                withWasmJs()
            }
        }
    }

    jvm()

    // Snapshot-mode consumers that only declare an Android target must be able to
    // resolve this library; a jvm variant cannot satisfy an androidJvm consumer.
    android {
        namespace = "artboard.runtime"
        compileSdk = 36
        minSdk = 24
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
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Skiko natives for the host OS, so JVM tests can raster-render Compose
        // scenes offscreen (renderComposeScene / ImageComposeScene).
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            // Validates that the hand-written snapshot manifest is real JSON.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
