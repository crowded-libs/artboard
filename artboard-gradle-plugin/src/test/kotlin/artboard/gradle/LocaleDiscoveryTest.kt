package artboard.gradle

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleDiscoveryTest {
    @Test
    fun discoversDefaultAndQualifiedValueDirectories() {
        val root = createTempDirectory("artboard-locales").toFile()
        try {
            listOf("values", "values-es", "values-ar-rEG", "values-nope", "font")
                .forEach { File(root, it).mkdirs() }

            assertEquals(setOf("ar", "en", "es"), LocaleDiscovery.scanRoots(listOf(root)))
        } finally {
            root.deleteRecursively()
        }
    }
}
