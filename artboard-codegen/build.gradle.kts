plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
}

// KSP processor: discovers zero-arg @Preview composables and emits an ArtboardRegistry.

dependencies {
    implementation(libs.symbol.processing.api)
    compileOnly(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "artboard-codegen", version.toString())

    pom {
        name = "Artboard Codegen"
        description = "KSP processor that discovers Compose Multiplatform previews for Artboard"
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
