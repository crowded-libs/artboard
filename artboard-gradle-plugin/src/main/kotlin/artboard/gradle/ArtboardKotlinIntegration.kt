package artboard.gradle

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
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
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
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
                    runTasks = runTasks,
                    export = export,
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
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
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

        val executables = target.binaries.executable(artboardCompilation)
            .filterIsInstance<Executable>()
        val developmentBinary = executables.single { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
        val productionBinary = executables.single { it.mode == KotlinJsBinaryMode.PRODUCTION }
        // Keep intermediate resource trees free of Finder/iCloud "file 2.ext" junk so
        // Kotlin/Compose packaging and the gallery sync do not trip over duplicates.
        listOf(mainCompilation, artboardCompilation).forEach { compilation ->
            project.tasks.named(compilation.processResourcesTaskName, Copy::class.java).configure { copy ->
                copy.exclude { details -> ConflictCopies.matches(details.file.name) }
                copy.doFirst {
                    ConflictCopies.purge(copy.destinationDir.toPath())
                }
            }
        }

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
            sync.exclude { details -> ConflictCopies.matches(details.file.name) }
            sync.into(runDirectory)
            sync.doLast {
                val removed = ConflictCopies.purge(sync.destinationDir.toPath())
                if (removed.isNotEmpty()) {
                    sync.logger.lifecycle(
                        "Artboard purged ${removed.size} conflict-copy path(s) from development distribution",
                    )
                }
            }
        }
        generateHost.configure { task ->
            task.entryScript.set(artboardCompilation.outputModuleName.map { "$it.mjs" })
        }
        runTasks.forEach { run ->
            run.configure { task ->
                task.contentDirectory.set(runDirectory)
                task.nodeModulesDirectory.set(
                    project.rootProject.layout.buildDirectory.dir("wasm/node_modules"),
                )
                task.dependsOn(syncRunContent)
                task.dependsOn(project.rootProject.tasks.named("kotlinWasmNpmInstall"))
            }
        }
        export.configure { task ->
            task.contentDirectories.from(
                productionBinary.linkSyncTask.flatMap { it.destinationDirectory },
            )
            task.contentDirectories.from(
                project.tasks.named(mainCompilation.processResourcesTaskName),
            )
            task.contentDirectories.from(
                project.tasks.named(artboardCompilation.processResourcesTaskName),
            )
            task.nodeModulesDirectory.set(
                project.rootProject.layout.buildDirectory.dir("wasm/node_modules"),
            )
            task.dependsOn(productionBinary.linkSyncTask)
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
}
