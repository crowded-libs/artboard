package artboard.gradle

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostGeneratorTest {
    @Test
    fun generatedHostEscapesUserControlledTextAndUsesModuleScript() {
        val root = createTempDirectory("artboard-host").toFile()
        try {
            HostGenerator.generate(
                outputDir = root,
                registryPackage = "example.generated",
                hostPackage = "example.generated.host",
                title = "Demo </title> \"$",
                languageTags = listOf("es", "ar"),
                entryScript = "demo-artboard.mjs",
                mode = GalleryMode.Live,
            )

            val main = File(root, "kotlin/example/generated/host/ArtboardHostMain.kt").readText()
            val html = File(root, "resources/index.html").readText()
            assertContains(main, "title = \"Demo </title> \\\"\\$\"")
            assertContains(html, "Demo &lt;/title&gt; &quot;$")
            assertContains(html, "<link rel=\"icon\" href=\"data:image/svg+xml,")
            assertContains(html, "<script type=\"module\" src=\"demo-artboard.mjs\"></script>")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun snapshotModeGeneratesHeadlessRendererAndNoBrowserAssets() {
        val root = createTempDirectory("artboard-snapshot-host").toFile()
        try {
            HostGenerator.generate(
                outputDir = root,
                registryPackage = "example.generated",
                hostPackage = "example.generated.host",
                title = "Demo \"$",
                languageTags = listOf("es"),
                entryScript = "demo-artboard.mjs",
                mode = GalleryMode.Snapshot,
            )

            val main = File(root, "kotlin/example/generated/host/ArtboardSnapshotMain.kt").readText()
            assertContains(main, "package example.generated.host")
            assertContains(main, "import example.generated.GeneratedArtboardRegistry")
            assertContains(main, "ArtboardSnapshotRenderer.render(")
            assertContains(main, "title = \"Demo \\\"\\$\"")
            // Template interpolation must survive into the generated source.
            assertContains(main, "\${result.imageCount}")

            assertFalse(
                File(root, "resources/index.html").exists(),
                "snapshot mode must not emit browser assets",
            )
            assertFalse(
                File(root, "kotlin/example/generated/host/ArtboardHostMain.kt").exists(),
                "snapshot mode must not emit the Wasm host",
            )
            assertTrue(
                main.contains("fun main(args: Array<String>)"),
                "snapshot entry point must accept the output directory argument",
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun androidSnapshotPinsRobolectricSdkCompatibleWithJdk17() {
        val root = createTempDirectory("artboard-android-snapshot-host").toFile()
        try {
            HostGenerator.generate(
                outputDir = root,
                registryPackage = "example.generated",
                hostPackage = "example.generated.host",
                title = "Android light",
                languageTags = listOf("ar"),
                entryScript = "unused.mjs",
                mode = GalleryMode.AndroidSnapshot,
            )

            val test = File(root, "kotlin/example/generated/host/ArtboardSnapshotTest.kt").readText()
            assertContains(test, "@RunWith(RobolectricTestRunner::class)")
            assertContains(
                test,
                "@Config(sdk = [${HostGenerator.ANDROID_SNAPSHOT_SDK}], qualifiers = \"w2000dp-h2000dp-xhdpi\")",
            )
            assertContains(test, "captureRoboImage")
            assertContains(test, "withSnapshotLocale")
        } finally {
            root.deleteRecursively()
        }
    }
}
