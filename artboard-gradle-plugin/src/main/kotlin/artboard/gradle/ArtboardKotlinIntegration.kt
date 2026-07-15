package artboard.gradle

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

/**
 * KGP-backed wiring loaded only after Kotlin Multiplatform is present.
 *
 * Keeping these types out of [ArtboardPlugin] lets diagnostics load even when a
 * consumer forgot to apply KMP, so `artboardStatus` can explain the problem.
 */
internal object ArtboardKotlinIntegration {
    fun configure(
        project: Project,
        generatedPackage: String,
        runtimeDependency: String,
        codegenDependency: String,
        pluginVersion: String,
        generateHost: TaskProvider<GenerateArtboardHostTask>,
        report: TaskProvider<ArtboardReportTask>,
        status: TaskProvider<ArtboardDoctorTask>,
        doctor: TaskProvider<ArtboardDoctorTask>,
        run: TaskProvider<ArtboardServeTask>,
    ) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.targets
            .withType(KotlinJsIrTarget::class.java)
            .matching { it.name == WASM_TARGET_NAME }
            .configureEach { target ->
                configureWasmTarget(
                    project = project,
                    kotlin = kotlin,
                    target = target,
                    generatedPackage = generatedPackage,
                    runtimeDependency = runtimeDependency,
                    codegenDependency = codegenDependency,
                    pluginVersion = pluginVersion,
                    generateHost = generateHost,
                    report = report,
                    status = status,
                    doctor = doctor,
                    run = run,
                )
            }
    }

    @Suppress("LongParameterList")
    private fun configureWasmTarget(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        target: KotlinJsIrTarget,
        generatedPackage: String,
        runtimeDependency: String,
        codegenDependency: String,
        pluginVersion: String,
        generateHost: TaskProvider<GenerateArtboardHostTask>,
        report: TaskProvider<ArtboardReportTask>,
        status: TaskProvider<ArtboardDoctorTask>,
        doctor: TaskProvider<ArtboardDoctorTask>,
        run: TaskProvider<ArtboardServeTask>,
    ) {
        configureDiagnostics(status, doctor) {
            it.hasWasmTarget.set(true)
            it.notes.add("wasmJsMain → Artboard runtime $pluginVersion")
            it.notes.add("kspWasmJs → Artboard preview discovery")
        }

        project.dependencies.add("kspWasmJs", codegenDependency)
        kotlin.sourceSets.named("wasmJsMain").configure { sourceSet ->
            sourceSet.dependencies {
                implementation(runtimeDependency)
            }
        }

        val mainCompilation: KotlinJsIrCompilation =
            target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val artboardCompilation: KotlinJsIrCompilation =
            target.compilations.maybeCreate(ARTBOARD_COMPILATION_NAME)
        artboardCompilation.associateWith(mainCompilation)
        artboardCompilation.defaultSourceSet.kotlin.srcDir(
            generateHost.flatMap { it.outputDirectory.dir("kotlin") },
        )
        artboardCompilation.defaultSourceSet.resources.srcDir(
            generateHost.flatMap { it.outputDirectory.dir("resources") },
        )
        artboardCompilation.compileTaskProvider.configure { it.dependsOn(generateHost) }
        project.tasks.named(artboardCompilation.processResourcesTaskName).configure {
            it.dependsOn(generateHost)
        }

        val developmentBinary = target.binaries.executable(artboardCompilation)
            .filterIsInstance<Executable>()
            .single { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
        val runDirectory = project.layout.buildDirectory.dir("artboard/run")
        val syncRunContent = project.tasks.register(
            "syncArtboardDevelopmentDistribution",
            Sync::class.java,
        ) { sync ->
            sync.description = "Assembles the isolated Artboard browser distribution"
            sync.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            sync.dependsOn(developmentBinary.linkSyncTask)
            sync.from(developmentBinary.linkSyncTask.flatMap { it.destinationDirectory })
            sync.from(project.tasks.named(mainCompilation.processResourcesTaskName))
            sync.from(project.tasks.named(artboardCompilation.processResourcesTaskName))
            sync.exclude { details -> CONFLICT_COPY.matches(details.file.name) }
            sync.into(runDirectory)
        }
        generateHost.configure { task ->
            task.entryScript.set(artboardCompilation.outputModuleName.map { "$it.mjs" })
        }
        run.configure { task ->
            task.contentDirectory.set(runDirectory)
            task.nodeModulesDirectory.set(
                project.rootProject.layout.buildDirectory.dir("wasm/node_modules"),
            )
            task.dependsOn(syncRunContent)
            task.dependsOn(project.rootProject.tasks.named("kotlinWasmNpmInstall"))
        }

        report.configure { it.dependsOn("kspKotlinWasmJs") }
        doctor.configure {
            it.dependsOn(report)
            it.dependsOn(artboardCompilation.compileTaskProvider)
        }

        configureDiagnostics(status, doctor) {
            it.hasIsolatedRunTask.set(true)
            it.notes.add("gallery executable → isolated '$ARTBOARD_COMPILATION_NAME' compilation")
        }
    }

    private fun configureDiagnostics(
        first: TaskProvider<ArtboardDoctorTask>,
        second: TaskProvider<ArtboardDoctorTask>,
        configure: (ArtboardDoctorTask) -> Unit,
    ) {
        first.configure(configure)
        second.configure(configure)
    }

    private const val WASM_TARGET_NAME = "wasmJs"
    private const val ARTBOARD_COMPILATION_NAME = "artboard"
    private val CONFLICT_COPY = Regex(".+ [2-9][0-9]*(\\.[^.]+)?")
}
