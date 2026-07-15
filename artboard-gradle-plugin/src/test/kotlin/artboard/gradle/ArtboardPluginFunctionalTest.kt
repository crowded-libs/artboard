package artboard.gradle

import org.gradle.testkit.runner.GradleRunner
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains

class ArtboardPluginFunctionalTest {
    @Test
    fun statusWorksWithoutMutatingOrRequiringPlatformPlugins() {
        withFixture { projectDir ->
            projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            projectDir.resolve("build.gradle.kts").writeText(
                "plugins { id(\"io.github.crowded-libs.artboard\") }",
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("artboardStatus", "--stacktrace")
                .withPluginClasspath()
                .build()

            assertContains(result.output, "✗ Kotlin Multiplatform plugin")
            assertContains(result.output, "ready to run = false")
        }
    }

    @Test
    fun doctorFailsWithAnActionableOptInMessage() {
        withFixture { projectDir ->
            projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            projectDir.resolve("build.gradle.kts").writeText(
                "plugins { id(\"io.github.crowded-libs.artboard\") }",
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("artboardDoctor", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

            assertContains(result.output, "Artboard does not add platform targets")
            assertContains(result.output, "Artboard is not ready to run")
        }
    }

    private fun withFixture(block: (java.io.File) -> Unit) {
        val directory = createTempDirectory("artboard-plugin-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
