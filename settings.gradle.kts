pluginManagement {
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

rootProject.name = "artboard"

include(":artboard-runtime")
include(":artboard-codegen")
include(":artboard-gradle-plugin")
include(":artboard-viewer")
include(":artboard-viewer-dist")
