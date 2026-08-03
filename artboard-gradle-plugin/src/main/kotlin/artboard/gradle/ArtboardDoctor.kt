package artboard.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Prints and optionally enforces the Artboard gallery readiness contract. */
@DisableCachingByDefault(because = "Diagnostic task that prints a readiness checklist to the console")
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

    /**
     * Label of the consumer target the gallery binds to, or blank when none is usable.
     *
     * Artboard never adds a target, so this records what the consumer already declared
     * rather than anything the plugin arranged.
     */
    @get:Input
    abstract val galleryTarget: Property<String>

    @get:Input
    abstract val hasIsolatedRunTask: Property<Boolean>

    /**
     * Set when the plugin could not enable an Android host-test compilation.
     *
     * Blank in every other case. Drives the one-line consumer opt-in remedy.
     */
    @get:Input
    abstract val androidHostTestFailure: Property<String>

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
        galleryTarget.convention("")
        hasIsolatedRunTask.convention(false)
        androidHostTestFailure.convention("")
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

        val target = galleryTarget.get()
        val ready = hasKotlinMultiplatform.get() &&
            hasCompose.get() &&
            hasKsp.get() &&
            target.isNotBlank() &&
            hasIsolatedRunTask.get() &&
            invalidResources.isEmpty()

        logger.lifecycle(
            buildString {
                appendLine("Artboard doctor — ${projectPath.get()}")
                appendLine()
                appendLine(check(hasKotlinMultiplatform.get(), "Kotlin Multiplatform plugin"))
                appendLine(check(hasCompose.get(), "Compose Multiplatform plugin"))
                appendLine(check(hasKsp.get(), "Plugin-managed KSP discovery"))
                appendLine(
                    check(
                        target.isNotBlank(),
                        if (target.isBlank()) "Consumer-declared gallery target" else "Gallery target — $target",
                    ),
                )
                appendLine(check(hasIsolatedRunTask.get(), "Isolated Artboard gallery executable"))
                appendLine(check(invalidResources.isEmpty(), "Compose resource filenames"))
                invalidResources.forEach { appendLine("  ! invalid/conflict resource: $it") }
                notes.get().forEach { appendLine("  · $it") }
                appendLine()
                appendLine("generated package = ${generatedPackage.get()}")
                appendLine("ready to run = $ready")
                if (!ready) {
                    val hostTestFailure = androidHostTestFailure.get()
                    appendLine(if (hostTestFailure.isBlank()) REMEDY else androidRemedy(hostTestFailure))
                }
            },
        )

        if (!ready && failWhenNotReady.get()) {
            throw GradleException("Artboard is not ready to run. See the checklist above.")
        }
    }

    private fun check(ok: Boolean, label: String): String =
        if (ok) "  ✓ $label" else "  ✗ $label"

    /**
     * Fallback when Artboard could not enable the Android host-test compilation itself.
     *
     * Normally the plugin calls `withHostTestBuilder` for the consumer; if a future AGP
     * stops allowing that, this is the one line they need to add.
     */
    private fun androidRemedy(reason: String): String =
        """

        Artboard renders Android previews in a host-test compilation, and could not
        enable one automatically ($reason).

        Add the opt-in to this module and run artboardDoctor again:

          kotlin {
              android {
                  withHostTestBuilder {}
              }
          }

        Alternatively declare `jvm()` — Artboard prefers it, renders faster, and needs
        no Android SDK.
        """.trimIndent()

    private companion object {
        val REMEDY =
            """

            Artboard does not add platform targets. It binds to one you already declare,
            so pick whichever is cheaper for this module:

              kotlin { jvm() }                          // snapshot gallery
                  Pre-rendered images browsed in Artboard's prebuilt viewer.
                  No Node toolchain and no WasmGC browser; previews only need to
                  compile for the JVM.

              kotlin {                                  // live gallery
                  @OptIn(ExperimentalWasmDsl::class)
                  wasmJs { browser() }
              }
                  Fully interactive, but every preview's dependency graph must
                  compile for Wasm.

            Declare one, keep preview bodies buildable for it, then run artboardDoctor again.
            """.trimIndent()
    }
}

/** Copies KSP's deterministic discovery report to the stable Artboard report location. */
@CacheableTask
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
