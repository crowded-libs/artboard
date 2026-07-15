package artboard.gradle

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains

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
}
