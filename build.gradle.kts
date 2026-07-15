plugins {
    // Keep root free of target plugins; modules apply what they need.
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
}

group = "io.github.crowdedlibs.artboard"
version = libs.versions.artboard.get()

allprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("artboardSeedStatus") {
    group = "artboard"
    description = "Prints seed-project status and next implementation targets"
    val projectVersion = provider { version.toString() }
    doLast {
        println(
            """
            |Artboard ${projectVersion.get()}
            |  core modules: runtime, codegen, gradle-plugin
            |  minimal gallery: ./gradlew -p samples/minimal artboardRun
            |  café gallery: ./gradlew -p showcase/cafe :shared:artboardRun
            |  doctor: ./gradlew -p samples/minimal artboardDoctor
            |  release: GitHub Release v${projectVersion.get()} publishes to Maven Central
            |  docs: README.md, AGENTS.md
            """.trimMargin()
        )
    }
}
