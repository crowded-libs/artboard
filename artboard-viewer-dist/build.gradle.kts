plugins {
    `java-library`
    alias(libs.plugins.mavenPublish)
}

/**
 * Ships Artboard's prebuilt Wasm gallery as an ordinary jar.
 *
 * Snapshot-mode consumers resolve this instead of compiling Wasm themselves. It is a
 * separate module from `:artboard-viewer` so the payload stays out of the Gradle
 * plugin jar (the bundle is tens of megabytes) while still resolving through normal
 * Maven coordinates, which keeps composite-build substitution straightforward.
 */
// Must match VIEWER_RESOURCE_PREFIX in the Gradle plugin.
val viewerResourcePrefix = "artboard-viewer"

// Snapshot-mode consumers unpack this jar. The browser distribution is the only
// payload: whenever :artboard-viewer sources change, this jar must rebuild so
// artboardExport / artboardRun never ship a stale schema or UI.
val viewerBrowserDistribution =
    project(":artboard-viewer").tasks.named("wasmJsBrowserDistribution")

tasks.named<Jar>("jar") {
    dependsOn(viewerBrowserDistribution)
    from(viewerBrowserDistribution) {
        into(viewerResourcePrefix)
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "artboard-viewer-dist", version.toString())

    pom {
        name = "Artboard Viewer Distribution"
        description = "Prebuilt Artboard Wasm gallery for snapshot-mode consumers"
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
