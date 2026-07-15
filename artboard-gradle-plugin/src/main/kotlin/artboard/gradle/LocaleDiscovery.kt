package artboard.gradle

import java.io.File
import java.util.Locale
import java.util.SortedSet
import java.util.TreeSet

/**
 * Discovers gallery language tags from Compose Multiplatform
 * `composeResources/values*` folders.
 *
 * Rules:
 * - `values-<lang>` / `values-<lang>-rXX` → language tag `<lang>`
 * - bare `values/` (default fallback) → `en` when no `values-en` exists
 *   (matches the usual “default strings are English” layout)
 */
internal object LocaleDiscovery {

    fun composeResourceDirs(projectDir: File): List<File> = listOf(
        File(projectDir, "src/commonMain/composeResources"),
        File(projectDir, "src/composeResources"),
    ).filter { it.isDirectory }

    fun scan(projectDir: File): SortedSet<String> {
        return scanRoots(composeResourceDirs(projectDir))
    }

    fun scanRoots(roots: Iterable<File>): SortedSet<String> {
        val tags = TreeSet<String>()
        var hasDefaultValues = false
        for (root in roots) {
            val children = root.listFiles()?.filter { it.isDirectory } ?: continue
            for (dir in children) {
                when {
                    dir.name == "values" -> hasDefaultValues = true
                    dir.name.startsWith("values-") -> {
                        val rest = dir.name.removePrefix("values-")
                        val lang = rest.substringBefore('-').lowercase(Locale.US)
                        if (lang.matches(LANG_TAG)) {
                            tags += lang
                        }
                    }
                }
            }
        }
        if (hasDefaultValues && "en" !in tags) {
            tags += "en"
        }
        return tags
    }

    private val LANG_TAG = Regex("[a-z]{2,3}")
}
