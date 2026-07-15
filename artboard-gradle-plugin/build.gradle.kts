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
            id = "io.github.crowdedlibs.artboard"
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
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest.attributes["Implementation-Version"] = project.version
}

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
