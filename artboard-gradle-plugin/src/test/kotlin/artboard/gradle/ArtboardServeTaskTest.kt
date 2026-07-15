package artboard.gradle

import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ArtboardServeTaskTest {
    @Test
    fun resolvesBareImportsToInstalledBrowserModules() {
        val root = createTempDirectory("artboard-server")
        try {
            val content = root.resolve("content").createDirectories()
            val modules = root.resolve("node_modules").createDirectories()
            content.resolve("imports.mjs").writeText("import * as time from '@js-joda/core';")
            val packageDir = modules.resolve("@js-joda/core").createDirectories()
            packageDir.resolve("package.json").writeText(
                """{"main":"dist/main.js","module":"dist/module.js"}""",
            )
            packageDir.resolve("dist").createDirectories().resolve("module.js").writeText("export {};")

            val imports = browserImportMap(content, modules)

            assertEquals(
                mapOf("@js-joda/core" to "/node_modules/@js-joda/core/dist/module.js"),
                imports,
            )
            val html = injectImportMap("<html><head></head></html>", imports)
            assertContains(html, "<script type=\"importmap\">")
            assertContains(html, "\"@js-joda/core\": \"/node_modules/@js-joda/core/dist/module.js\"")
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
