pluginManagement {
    includeBuild("../..")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "artboard-light"

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.crowded-libs.artboard:artboard-codegen"))
            .using(project(":artboard-codegen"))
        substitute(module("io.github.crowded-libs.artboard:artboard-runtime"))
            .using(project(":artboard-runtime"))
        substitute(module("io.github.crowded-libs.artboard:artboard-viewer-dist"))
            .using(project(":artboard-viewer-dist"))
    }
}
