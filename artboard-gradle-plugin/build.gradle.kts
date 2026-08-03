plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
}

// Use the ambient JDK (repo uses 17+; CI/dev commonly 21). Avoid strict
// toolchain download requirements in the included build.

gradlePlugin {
    website.set("https://github.com/crowded-libs/artboard")
    vcsUrl.set("https://github.com/crowded-libs/artboard.git")
    plugins {
        create("artboard") {
            id = "io.github.crowded-libs.artboard"
            implementationClass = "artboard.gradle.ArtboardPlugin"
            displayName = "Artboard"
            description =
                "Spatial Wasm gallery for Compose Multiplatform @Preview components"
        }
    }
}

dependencies {
    implementation(gradleApi())
    // KMP public plugin API — used only when the consumer already applies KMP.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    // Compose Multiplatform public plugin API — used only to resolve the host-OS
    // Skiko runtime for snapshot rendering, from the consumer's own compose version.
    compileOnly(
        "org.jetbrains.compose:compose-gradle-plugin:${libs.versions.composeMultiplatform.get()}",
    )
    // AGP public DSL — used only to enable a host-test compilation on a consumer's
    // Android target, so snapshot rendering stays zero-config for them.
    compileOnly("com.android.tools.build:gradle-api:${libs.versions.agp.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest.attributes["Implementation-Version"] = project.version
}

// Surfaces catalog versions to plugin code, so the Android toolchain the plugin
// injects into consumer builds is visible to dependency-update tooling instead of
// hiding in string literals.
val generateArtboardVersions by tasks.registering {
    val output = layout.buildDirectory.dir("generated/artboard-versions")
    val junit = libs.versions.junit.get()
    val robolectric = libs.versions.robolectric.get()
    val roborazzi = libs.versions.roborazzi.get()
    outputs.dir(output)
    inputs.property("junit", junit)
    inputs.property("robolectric", robolectric)
    inputs.property("roborazzi", roborazzi)
    doLast {
        val file = output.get().asFile.resolve("artboard/gradle/ArtboardVersions.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            // Generated from gradle/libs.versions.toml — do not edit.
            package artboard.gradle

            internal object ArtboardVersions {
                const val JUNIT = "$junit"
                const val ROBOLECTRIC = "$robolectric"
                const val ROBORAZZI = "$roborazzi"
            }

            """.trimIndent(),
        )
    }
}

sourceSets.main { kotlin.srcDir(generateArtboardVersions) }

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "artboard-gradle-plugin", version.toString())

    pom {
        name = "Artboard Gradle Plugin"
        description = "Gradle plugin for generating, serving, and exporting Artboard Compose Multiplatform galleries"
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
