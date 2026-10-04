package artboard.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtboardExportCollectorTest {
    private val workspace = createTempDirectory("artboard-export-collector")
    private val combined = workspace.resolve("root/build/artboard/combined-export")

    @AfterTest
    fun tearDown() {
        workspace.toFile().deleteRecursively()
    }

    @Test
    fun singleModuleExportLeavesTheCombinedSiteAlone() {
        val earlier = combined.createDirectories().resolve("keep.txt").apply { writeText("earlier build") }

        assertNull(collector().register(gallery(":feature:a"), moduleExport(":feature:a")))

        assertTrue(earlier.exists())
    }

    @Test
    fun secondModuleBuildsACombinedStaticSite() {
        val collector = collector()
        combined.createDirectories().resolve("m/feature-stale").createDirectories()

        collector.register(gallery(":feature:b", GalleryKind.Snapshot), moduleExport(":feature:b"))
        val output = collector.register(gallery(":feature:a"), moduleExport(":feature:a"))

        assertEquals(combined.toAbsolutePath().normalize(), output)
        assertFalse(combined.resolve("m/feature-stale").exists(), "modules from earlier builds are cleared")
        assertEquals("export :feature:a", combined.resolve("m/feature-a/index.html").readText())
        assertEquals("nested", combined.resolve("m/feature-b/images/frame.png").readText())
        assertContains(combined.resolve("index.html").readText(), "<html data-static lang=")
        assertTrue(Files.size(combined.resolve("$SHELL_ASSETS_DIRECTORY/IBMPlexMono-Regular.ttf")) > 0)
        assertEquals(
            """{"build":"umbrella","galleries":[""" +
                """{"path":":feature:a","title":"a","kind":"live","url":"m/feature-a/"},""" +
                """{"path":":feature:b","title":"b","kind":"snapshot","url":"m/feature-b/"}]}""",
            combined.resolve(GALLERIES_JSON).readText(),
        )
    }

    @Test
    fun laterModulesAreAddedWithoutRecopyingEarlierOnes() {
        val collector = collector()
        collector.register(gallery(":feature:a"), moduleExport(":feature:a"))
        collector.register(gallery(":feature:b"), moduleExport(":feature:b"))
        combined.resolve("m/feature-a/marker.txt").writeText("untouched")

        collector.register(gallery(":feature:c"), moduleExport(":feature:c"))

        assertEquals("untouched", combined.resolve("m/feature-a/marker.txt").readText())
        assertEquals("export :feature:c", combined.resolve("m/feature-c/index.html").readText())
        assertContains(combined.resolve(GALLERIES_JSON).readText(), "\":feature:c\"")
    }

    private fun collector(): ArtboardExportCollector =
        ProjectBuilder.builder().build().gradle.sharedServices.registerIfAbsent(
            ArtboardExportCollector.SERVICE_NAME,
            ArtboardExportCollector::class.java,
        ) { spec ->
            spec.parameters.outputDirectory.set(combined.toFile())
            spec.parameters.buildName.set("umbrella")
        }.get()

    private fun gallery(path: String, kind: GalleryKind = GalleryKind.Live) =
        MountedGallery(path, path.substringAfterLast(':'), kind)

    private fun moduleExport(path: String): Path {
        val root = workspace.resolve(path.trim(':').replace(':', '-')).resolve("build/artboard/export")
        root.resolve("images").createDirectories().resolve("frame.png").writeText("nested")
        root.resolve("index.html").writeText("export $path")
        return root
    }
}
