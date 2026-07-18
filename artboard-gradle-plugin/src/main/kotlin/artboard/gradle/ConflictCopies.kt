package artboard.gradle

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * macOS Finder / iCloud style duplicate names (`FreeSans 2.ttf`, `composeResources 3`).
 *
 * These accumulate under `build/` when cloud sync or Finder collides with Gradle writes.
 * Artboard never wants them in gallery distributions; purge rather than fail the run.
 */
internal object ConflictCopies {
    /**
     * Matches `name N` or `name N.ext` where N ≥ 2 (Finder's "keep both" numbering),
     * including two-digit copies (`file 10.ttf`).
     */
    private val NAME: Regex = Regex("""^(.+) ([1-9]\d*)(\.[^.]+)?$""")

    fun matches(fileName: String): Boolean {
        val match = NAME.matchEntire(fileName) ?: return false
        return match.groupValues[2].toIntOrNull()?.let { it >= 2 } == true
    }

    /**
     * Deletes every conflict-copy file or directory under [root] (deepest paths first).
     *
     * @return relative paths that were removed, sorted for stable logs/tests
     */
    fun purge(root: Path): List<String> {
        if (!Files.isDirectory(root)) return emptyList()
        val absoluteRoot = root.toAbsolutePath().normalize()
        val matches = Files.walk(absoluteRoot).use { stream ->
            stream
                .filter { path -> matches(path.fileName.toString()) }
                .sorted(compareByDescending<Path> { it.nameCount }.thenBy { it.toString() })
                .toList()
        }
        val removed = mutableListOf<String>()
        for (path in matches) {
            if (!Files.exists(path)) continue
            val relative = absoluteRoot.relativize(path).toString().replace('\\', '/')
            when {
                path.isDirectory() -> path.toFile().deleteRecursively()
                path.isRegularFile() -> Files.deleteIfExists(path)
                else -> Files.deleteIfExists(path)
            }
            removed += relative
        }
        return removed.sorted()
    }
}
