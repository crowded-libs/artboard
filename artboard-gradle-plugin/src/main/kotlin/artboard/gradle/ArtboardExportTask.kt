package artboard.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.inject.Inject

/** Assembles an optimized Artboard gallery for static HTTP hosting. */
@DisableCachingByDefault(because = "Resolves Kotlin/Wasm browser modules from the npm installation")
abstract class ArtboardExportTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectories: ConfigurableFileCollection

    @get:Internal
    abstract val nodeModulesDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun export() {
        val output = outputDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        fileSystemOperations.sync { copy ->
            copy.from(contentDirectories)
            copy.into(output)
            copy.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            copy.exclude { details -> ConflictCopies.matches(details.file.name) }
        }
        // Sync exclude skips sources; purge removes leftover Finder/iCloud copies already on disk.
        ConflictCopies.purge(output)

        val index = output.resolve("index.html")
        check(Files.isRegularFile(index)) {
            "Artboard export did not produce index.html. Run artboardDoctor for diagnostics."
        }

        val nodeModules = nodeModulesDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val modules = resolveBrowserModules(output, nodeModules)
        check(modules.unresolvedSpecifiers.isEmpty()) {
            buildString {
                appendLine("Artboard cannot export unresolved browser modules:")
                modules.unresolvedSpecifiers.forEach { appendLine("  $it") }
                append("Verify the package is installed for the consumer's Wasm browser target.")
            }
        }
        copyBrowserModules(modules.resolutions, output)
        ConflictCopies.purge(output.resolve("node_modules"))

        val importMap = modules.resolutions.associate { resolution ->
            resolution.specifier to
                "./node_modules/${resolution.packageName}/${resolution.entryPath}"
        }
        Files.writeString(index, injectImportMap(Files.readString(index), importMap))

        check(Files.walk(output).use { paths -> paths.noneMatch(Files::isSymbolicLink) }) {
            "Artboard export contains symbolic links, which static hosts cannot publish safely."
        }
        logger.lifecycle("Artboard static export → $output")
    }

    private fun copyBrowserModules(
        resolutions: Collection<BrowserModuleResolution>,
        output: Path,
    ) {
        resolutions.distinctBy { it.packageName }.forEach { resolution ->
            val destination = output.resolve("node_modules").resolve(resolution.packageName)
            copyRegularTree(resolution.packageDirectory, destination)
        }
    }

    private fun copyRegularTree(sourceRoot: Path, destinationRoot: Path) {
        Files.walk(sourceRoot).use { paths ->
            paths.forEach { source ->
                check(!Files.isSymbolicLink(source)) {
                    "Artboard cannot export symbolic link from browser module: $source"
                }
                val relative = sourceRoot.relativize(source)
                val destination = destinationRoot.resolve(relative).normalize()
                check(destination.startsWith(destinationRoot)) {
                    "Browser module path escapes the Artboard export: $relative"
                }
                when {
                    Files.isDirectory(source) -> Files.createDirectories(destination)
                    Files.isRegularFile(source) -> {
                        Files.createDirectories(destination.parent)
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

}
