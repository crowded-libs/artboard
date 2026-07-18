package artboard.gradle

import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConflictCopiesTest {
    @Test
    fun matchesFinderStyleDuplicateNames() {
        assertTrue(ConflictCopies.matches("FreeSans 2.ttf"))
        assertTrue(ConflictCopies.matches("SpaceGrotesk-Regular 12.ttf"))
        assertTrue(ConflictCopies.matches("composeResources 3"))
        assertTrue(ConflictCopies.matches("artboard 2.resources"))
        assertFalse(ConflictCopies.matches("FreeSans.ttf"))
        assertFalse(ConflictCopies.matches("composeResources"))
        assertFalse(ConflictCopies.matches("file 1.ttf")) // Finder starts at 2
    }

    @Test
    fun purgeRemovesConflictFilesAndDirectoriesButKeepsCanonicals() {
        val root = createTempDirectory("artboard-conflict-copies")
        try {
            val fonts = root.resolve("composeResources/artboard.resources/font").createDirectories()
            fonts.resolve("FreeSans.ttf").writeText("ok")
            fonts.resolve("FreeSans 2.ttf").writeText("dup")
            fonts.resolve("FreeSans 3.ttf").writeText("dup")
            val junkDir = root.resolve("composeResources 2/font").createDirectories()
            junkDir.resolve("FreeSans.ttf").writeText("nested")
            root.resolve("index.html").writeText("<html/>")

            val removed = ConflictCopies.purge(root)

            assertEquals(
                listOf(
                    "composeResources 2",
                    "composeResources/artboard.resources/font/FreeSans 2.ttf",
                    "composeResources/artboard.resources/font/FreeSans 3.ttf",
                ),
                removed,
            )
            assertTrue(fonts.resolve("FreeSans.ttf").isRegularFile())
            assertFalse(fonts.resolve("FreeSans 2.ttf").exists())
            assertFalse(root.resolve("composeResources 2").exists())
            assertTrue(root.resolve("index.html").isRegularFile())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
