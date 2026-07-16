package artboard.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArtboardServeTaskTest {
    @Test
    fun localAndLanTasksUseExplicitSafeBindings() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(ArtboardPlugin::class.java)

        val local = project.tasks.named("artboardRun", ArtboardServeTask::class.java).get()
        val lan = project.tasks.named("artboardRunLan", ArtboardServeTask::class.java).get()

        assertEquals(LOOPBACK_ADDRESS, local.bindAddress.get())
        assertEquals(ALL_INTERFACES_ADDRESS, lan.bindAddress.get())
        assertEquals(8080, local.preferredPort.get())
        assertEquals(8080, lan.preferredPort.get())
    }

    @Test
    fun lanUrlsIncludeOnlySortedPrivateIpv4Addresses() {
        val addresses = listOf(
            InetAddress.getByName("192.168.1.42"),
            InetAddress.getByName("127.0.0.1"),
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("10.0.0.9"),
            InetAddress.getByName("169.254.1.2"),
        )

        assertEquals(
            listOf(
                "http://10.0.0.9:8080/",
                "http://192.168.1.42:8080/",
            ),
            artboardLanUrls(port = 8080, addresses = addresses),
        )
    }

    @Test
    fun serverFallsBackWhenThePreferredPortIsOccupied() {
        val loopback = InetAddress.getByName(LOOPBACK_ADDRESS)
        ServerSocket(0, 50, loopback).use { occupied ->
            val server = firstAvailableServer(
                startPort = occupied.localPort,
                bindAddress = LOOPBACK_ADDRESS,
            )
            try {
                assertTrue(server.address.port > occupied.localPort)
                assertTrue(server.address.port <= occupied.localPort + 20)
            } finally {
                server.start()
                server.stop(0)
            }
        }
    }

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

    @Test
    fun resolvesTransitiveAndSubpathBrowserModulesForStaticExport() {
        val root = createTempDirectory("artboard-export")
        try {
            val content = root.resolve("content").createDirectories()
            val modules = root.resolve("node_modules").createDirectories()
            content.resolve("imports.mjs").writeText("import helper from 'demo-package/feature';")

            val demo = modules.resolve("demo-package").createDirectories()
            demo.resolve("package.json").writeText("""{"module":"index.js"}""")
            demo.resolve("feature.js").writeText("import value from 'helper-package'; export default value;")

            val helper = modules.resolve("helper-package").createDirectories()
            helper.resolve("package.json").writeText("""{"module":"module.mjs"}""")
            helper.resolve("module.mjs").writeText("export default 42;")

            val result = resolveBrowserModules(content, modules)

            assertEquals(emptyList(), result.unresolvedSpecifiers)
            assertEquals(
                listOf("demo-package/feature", "helper-package"),
                result.resolutions.map { it.specifier },
            )
            assertEquals(
                listOf("feature.js", "module.mjs"),
                result.resolutions.map { it.entryPath },
            )
            val html = injectImportMap(
                "<html><head></head></html>",
                result.resolutions.associate {
                    it.specifier to "./node_modules/${it.packageName}/${it.entryPath}"
                },
            )
            assertContains(
                html,
                "\"demo-package/feature\": \"./node_modules/demo-package/feature.js\"",
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun reportsUnresolvedBrowserModules() {
        val root = createTempDirectory("artboard-export-unresolved")
        try {
            val content = root.resolve("content").createDirectories()
            val modules = root.resolve("node_modules").createDirectories()
            content.resolve("imports.mjs").writeText("import 'missing-package';")

            val result = resolveBrowserModules(content, modules)

            assertEquals(listOf("missing-package"), result.unresolvedSpecifiers)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
