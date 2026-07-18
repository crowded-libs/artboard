package artboard.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Prints and optionally enforces the Artboard gallery readiness contract. */
abstract class ArtboardDoctorTask : DefaultTask() {
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val generatedPackage: Property<String>

    @get:Input
    abstract val hasKotlinMultiplatform: Property<Boolean>

    @get:Input
    abstract val hasCompose: Property<Boolean>

    @get:Input
    abstract val hasKsp: Property<Boolean>

    @get:Input
    abstract val hasWasmTarget: Property<Boolean>

    @get:Input
    abstract val hasIsolatedRunTask: Property<Boolean>

    @get:Input
    abstract val failWhenNotReady: Property<Boolean>

    @get:Input
    abstract val notes: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResources: ConfigurableFileCollection

    init {
        hasKotlinMultiplatform.convention(false)
        hasCompose.convention(false)
        hasKsp.convention(false)
        hasWasmTarget.convention(false)
        hasIsolatedRunTask.convention(false)
        failWhenNotReady.convention(true)
        notes.convention(emptyList())
    }

    @TaskAction
    fun diagnose() {
        val invalidResources = composeResources.files
            .asSequence()
            .filter { it.isFile }
            .filter { file ->
                file.name.any(Char::isWhitespace) || ConflictCopies.matches(file.name)
            }
            .map { it.relativeTo(projectDirectory.get().asFile).path }
            .sorted()
            .toList()

        val ready = hasKotlinMultiplatform.get() &&
            hasCompose.get() &&
            hasKsp.get() &&
            hasWasmTarget.get() &&
            hasIsolatedRunTask.get() &&
            invalidResources.isEmpty()

        logger.lifecycle(
            buildString {
                appendLine("Artboard doctor — ${projectPath.get()}")
                appendLine()
                appendLine(check(hasKotlinMultiplatform.get(), "Kotlin Multiplatform plugin"))
                appendLine(check(hasCompose.get(), "Compose Multiplatform plugin"))
                appendLine(check(hasKsp.get(), "Plugin-managed KSP discovery"))
                appendLine(check(hasWasmTarget.get(), "Consumer-declared wasmJs target"))
                appendLine(check(hasIsolatedRunTask.get(), "Isolated Artboard browser executable"))
                appendLine(check(invalidResources.isEmpty(), "Compose resource filenames"))
                invalidResources.forEach { appendLine("  ! invalid/conflict resource: $it") }
                notes.get().forEach { appendLine("  · $it") }
                appendLine()
                appendLine("generated package = ${generatedPackage.get()}")
                appendLine("ready to run = $ready")
                if (!ready) appendLine(REMEDY)
            },
        )

        if (!ready && failWhenNotReady.get()) {
            throw GradleException("Artboard is not ready to run. See the checklist above.")
        }
    }

    private fun check(ok: Boolean, label: String): String =
        if (ok) "  ✓ $label" else "  ✗ $label"

    private companion object {
        val REMEDY =
            """

            Artboard does not add platform targets. Apply Compose Multiplatform and declare:

              kotlin {
                  @OptIn(ExperimentalWasmDsl::class)
                  wasmJs { browser() }
              }

            Keep preview bodies Wasm-safe, then run artboardDoctor again.
            """.trimIndent()
    }
}

/** Copies KSP's deterministic discovery report to the stable Artboard report location. */
abstract class ArtboardReportTask : DefaultTask() {
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedReports: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun materialize() {
        val source = generatedReports.files.singleOrNull { it.isFile }
            ?: throw GradleException("Artboard discovery did not produce artboard-previews.json")
        val destination = reportFile.get().asFile
        destination.parentFile.mkdirs()
        source.copyTo(destination, overwrite = true)
        logger.lifecycle("Artboard report → ${destination.relativeTo(projectDirectory.get().asFile)}")
    }
}
