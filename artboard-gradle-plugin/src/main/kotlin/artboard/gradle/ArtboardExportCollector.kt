package artboard.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Combines every module's `artboardExport` in one build into a single static site.
 *
 * A module's own export is never changed. Once a second module exports in the same
 * build, the combined site is rebuilt from scratch, so modules not part of this build
 * never linger; each later export is added as it finishes. A build exporting one
 * module leaves any earlier combined site alone.
 */
abstract class ArtboardExportCollector : BuildService<ArtboardExportCollector.Parameters> {
    interface Parameters : BuildServiceParameters {
        val outputDirectory: DirectoryProperty
        val buildName: Property<String>
    }

    private val exports = LinkedHashMap<String, Pair<MountedGallery, Path>>()

    /** Returns the combined site's directory once it holds two or more modules, else null. */
    @Synchronized
    internal fun register(gallery: MountedGallery, exportDirectory: Path): Path? {
        exports[gallery.projectPath] = gallery to exportDirectory
        if (exports.size < 2) return null
        val output = parameters.outputDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val mounts = if (exports.size == 2) {
            deleteRecursively(output)
            exports.values.toList()
        } else {
            listOf(gallery to exportDirectory)
        }
        mounts.forEach { (mount, source) -> copyTree(source, output.resolve(MOUNT_DIRECTORY).resolve(mount.slug)) }
        writeShell(output, parameters.buildName.get(), exports.values.map { it.first })
        return output
    }

    private fun copyTree(source: Path, destination: Path) {
        deleteRecursively(destination)
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val target = destination.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target)
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
    }

    internal companion object {
        const val SERVICE_NAME = "artboardExportCollector"

        /** Beside, never inside, a root module's own `build/artboard/export`. */
        const val OUTPUT_PATH = "build/artboard/combined-export"
    }
}
