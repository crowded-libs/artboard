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

rootProject.name = "artboard-minimal"

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.crowded-libs.artboard:artboard-codegen"))
            .using(project(":artboard-codegen"))
        substitute(module("io.github.crowded-libs.artboard:artboard-runtime"))
            .using(project(":artboard-runtime"))
    }
}
