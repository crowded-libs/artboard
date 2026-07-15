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

rootProject.name = "cafe-showcase"
include(":shared")
include(":androidApp")

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.crowdedlibs.artboard:artboard-codegen"))
            .using(project(":artboard-codegen"))
        substitute(module("io.github.crowdedlibs.artboard:artboard-runtime"))
            .using(project(":artboard-runtime"))
    }
}
