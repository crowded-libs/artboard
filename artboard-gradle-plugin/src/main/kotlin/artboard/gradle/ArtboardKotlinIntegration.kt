package artboard.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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
        viewerDistDependency: String,
        pluginVersion: String,
        generateHost: TaskProvider<GenerateArtboardHostTask>,
        report: TaskProvider<ArtboardReportTask>,
        status: TaskProvider<ArtboardDoctorTask>,
        doctor: TaskProvider<ArtboardDoctorTask>,
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
    ) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val wasm = GalleryTarget.Wasm
        kotlin.targets
            .withType(KotlinJsIrTarget::class.java)
            .matching { it.name == wasm.targetName }
            .configureEach { target ->
                configureWasmTarget(
                    project = project,
                    kotlin = kotlin,
                    target = target,
                    galleryTarget = wasm,
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

        // Snapshot mode binds to a target the consumer already has, so a gallery never
        // requires opting into Wasm. Live mode still wins when both exist.
        //
        // Deferred to afterEvaluate because the two modes are mutually exclusive and
        // the choice depends on the *complete* target set: a `configureEach` predicate
        // fires as each target is added, so `jvm()` declared before `wasmJs {}` would
        // wire both and leave the generated host non-deterministic.
        // Android renders through a host-test compilation, which AGP's KMP library
        // plugin does not create by default. Enabling it must happen while the target
        // is still being configured, so this hook runs eagerly rather than in
        // afterEvaluate — and it stays a no-op when Wasm already wins.
        AndroidHostTest.enableEarly(project, kotlin, codegenDependency)

        // KSP captures the processor classpath during evaluation. Registering
        // artboard-codegen only from afterEvaluate leaves kspKotlinJvm permanently
        // skipped (onlyIf false). addLater keeps the edge early while deferring the
        // "is JVM the selected gallery target?" decision until every target exists.
        registerJvmKspProcessorEarly(project, kotlin, codegenDependency)

        val jvm = GalleryTarget.Jvm
        project.afterEvaluate {
            val declaredTargets = kotlin.targets.names.toSet()
            if (GalleryTarget.select(declaredTargets) != jvm) return@afterEvaluate
            val jvmTarget = kotlin.targets.findByName(jvm.targetName) as? KotlinJvmTarget
                ?: return@afterEvaluate

            configureJvmTarget(
                project = project,
                kotlin = kotlin,
                target = jvmTarget,
                galleryTarget = jvm,
                generatedPackage = generatedPackage,
                runtimeDependency = runtimeDependency,
                codegenDependency = codegenDependency,
                viewerDistDependency = viewerDistDependency,
                pluginVersion = pluginVersion,
                generateHost = generateHost,
                report = report,
                status = status,
                doctor = doctor,
                runTasks = runTasks,
                export = export,
            )
        }

        val android = GalleryTarget.Android
        project.afterEvaluate {
            val declaredTargets = kotlin.targets.names.toSet()
            if (GalleryTarget.select(declaredTargets) != android) return@afterEvaluate

            configureAndroidTarget(
                project = project,
                kotlin = kotlin,
                galleryTarget = android,
                generatedPackage = generatedPackage,
                runtimeDependency = runtimeDependency,
                codegenDependency = codegenDependency,
                viewerDistDependency = viewerDistDependency,
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

    /**
     * Wires the Robolectric snapshot renderer onto a consumer's existing Android target.
     *
     * Android has no offscreen Skia scene, so previews are composed for real and
     * captured through Compose's test API in a host-test compilation that
     * [AndroidHostTest] enabled on the consumer's behalf.
     */
    @Suppress("LongParameterList")
    private fun configureAndroidTarget(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        galleryTarget: GalleryTarget,
        generatedPackage: String,
        runtimeDependency: String,
        codegenDependency: String,
        viewerDistDependency: String,
        pluginVersion: String,
        generateHost: TaskProvider<GenerateArtboardHostTask>,
        report: TaskProvider<ArtboardReportTask>,
        status: TaskProvider<ArtboardDoctorTask>,
        doctor: TaskProvider<ArtboardDoctorTask>,
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
    ) {
        configureDiagnostics(status, doctor) {
            it.galleryTarget.set(galleryTarget.label)
            it.notes.add("${galleryTarget.mainSourceSetName} → Artboard runtime $pluginVersion")
            it.notes.add("${galleryTarget.kspConfigurationName} → Artboard preview discovery")
            it.notes.add("snapshot images → rendered by Robolectric, no emulator")
        }

        if (!AndroidHostTest.enabled) {
            configureDiagnostics(status, doctor) {
                it.androidHostTestFailure.set(AndroidHostTest.failureReason ?: "unavailable")
            }
            return
        }

        generateHost.configure { it.mode.set(GalleryMode.AndroidSnapshot) }

        kotlin.sourceSets.named(galleryTarget.mainSourceSetName).configure { sourceSet ->
            sourceSet.dependencies {
                implementation(runtimeDependency)
            }
        }

        // The renderer is a generated JUnit test, so it and its harness live in the
        // plugin-created host-test source set — never in the consumer's own tests.
        kotlin.sourceSets.named(ANDROID_HOST_TEST_SOURCE_SET).configure { sourceSet ->
            sourceSet.kotlin.srcDir(generateHost.flatMap { it.outputDirectory.dir("kotlin") })
            sourceSet.dependencies {
                implementation(runtimeDependency)
                // Roborazzi exposes Robolectric only as `implementation`, so the
                // generated test's own `org.robolectric` imports need it declared.
                implementation(JUNIT_DEPENDENCY)
                implementation(ROBOLECTRIC_DEPENDENCY)
                // Roborazzi owns the capture: its composable overload sidesteps the
                // Compose test-host idling that times out under Robolectric, and never
                // launches an Activity — so no Compose ui-test artifacts are needed.
                implementation(ROBORAZZI_DEPENDENCY)
                implementation(ROBORAZZI_COMPOSE_DEPENDENCY)
            }
        }

        project.tasks.named(ANDROID_HOST_TEST_COMPILE_TASK).configure { it.dependsOn(generateHost) }

        val snapshotDirectory = project.layout.buildDirectory.dir("artboard/snapshots")
        // Resolved out here: Gradle forbids configureEach while a task is being realized.
        val composeResources = androidComposeResources(project)
        val renderTask = project.tasks.named(ANDROID_HOST_TEST_TASK, Test::class.java) { task ->
            task.systemProperty(
                HostGenerator.ANDROID_OUTPUT_PROPERTY,
                snapshotDirectory.get().asFile.absolutePath,
            )
            // Without record mode Roborazzi treats captureRoboImage as a no-op.
            task.systemProperty("roborazzi.test.record", "true")
            task.classpath += composeResources
            task.outputs.dir(snapshotDirectory)
            task.filter.includeTestsMatching(
                "$generatedPackage.host.${HostGenerator.ANDROID_SNAPSHOT_TEST_CLASS}",
            )
            // iCloud "file 2.cvr" junk under build/ breaks Compose resource lookup.
            val buildDirectory = project.layout.buildDirectory
            task.doFirst {
                ConflictCopies.purge(buildDirectory.get().asFile.toPath())
            }
        }

        // Consumers use one vocabulary across every renderer, so the Android path gets
        // the same task name even though the work happens in a test task.
        val snapshot = project.tasks.register("artboardSnapshot") { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Renders every @Preview to a PNG plus a snapshot manifest"
            task.dependsOn(renderTask)
            task.outputs.dir(snapshotDirectory)
        }

        wireSnapshotGallery(
            project = project,
            viewerDistDependency = viewerDistDependency,
            snapshot = snapshot,
            runTasks = runTasks,
            export = export,
        )

        report.configure { it.dependsOn(galleryTarget.kspTaskName) }
        doctor.configure { it.dependsOn(report) }

        configureDiagnostics(status, doctor) {
            it.hasIsolatedRunTask.set(true)
            it.notes.add("snapshot renderer → plugin-enabled '$ANDROID_HOST_TEST_SOURCE_SET' compilation")
        }
    }

    /**
     * Compose resource roots for the Android renderer, as classpath entries.
     *
     * Compose Multiplatform reaches Android resources through merged assets, staged by
     * its own `copy…ComposeResourcesToAndroidAssets` task. Under AGP's KMP library
     * plugin that task never receives an output directory — AGP wires one through the
     * variant-sources API, which this plugin does not participate in — so the assets
     * are never produced and every `stringResource` call fails at render time.
     *
     * Supplying the missing directory is enough, because the task already knows the
     * package-qualified `composeResources/<package>` placement the reader looks for,
     * and the Android reader falls back to `ClassLoader.getResourceAsStream` when the
     * asset manager comes up empty. A convention rather than a value, so a fixed AGP
     * or Compose release silently takes over.
     *
     * The task type is `internal` to Compose, so it is reached by name and its output
     * property reflectively — deliberately, because the alternative is re-deriving the
     * resource package here and getting it subtly wrong. If that access ever fails the
     * directory stays unset, which is exactly today's behaviour: previews using
     * resources fail with a named `MissingResourceException` in the manifest.
     *
     * Empty when Compose resources are not in use, which is the common case.
     */
    private fun androidComposeResources(project: Project): FileCollection {
        val roots = project.objects.fileCollection()
        project.pluginManager.withPlugin(COMPOSE_PLUGIN_ID) {
            val copies = project.tasks.matching { it.name.endsWith(COMPOSE_ANDROID_ASSETS_TASK_SUFFIX) }
            copies.configureEach { copy ->
                runCatching {
                    val outputDirectory = copy.javaClass
                        .getMethod("getOutputDirectory")
                        .invoke(copy) as DirectoryProperty
                    outputDirectory.convention(
                        project.layout.buildDirectory.dir("artboard/android-assets/${copy.name}"),
                    )
                }
            }
            roots.from(copies)
        }
        return roots
    }

    /**
     * Wires the headless snapshot renderer onto a consumer's existing `jvm` target.
     *
     * Mirrors the Wasm wiring — plugin-owned KSP discovery, an isolated `artboard`
     * compilation, and a generated entry point — but produces PNG files instead of a
     * browser bundle, so no Node toolchain or WasmGC browser is involved.
     */
    @Suppress("LongParameterList")
    private fun configureJvmTarget(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        target: KotlinJvmTarget,
        galleryTarget: GalleryTarget,
        generatedPackage: String,
        runtimeDependency: String,
        codegenDependency: String,
        viewerDistDependency: String,
        pluginVersion: String,
        generateHost: TaskProvider<GenerateArtboardHostTask>,
        report: TaskProvider<ArtboardReportTask>,
        status: TaskProvider<ArtboardDoctorTask>,
        doctor: TaskProvider<ArtboardDoctorTask>,
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
    ) {
        configureDiagnostics(status, doctor) {
            it.galleryTarget.set(galleryTarget.label)
            it.notes.add("${galleryTarget.mainSourceSetName} → Artboard runtime $pluginVersion")
            it.notes.add("${galleryTarget.kspConfigurationName} → Artboard preview discovery")
            it.notes.add("snapshot images → no wasmJs target required")
        }

        generateHost.configure { it.mode.set(GalleryMode.Snapshot) }

        // Processor dependency is registered early via [registerJvmKspProcessorEarly].
        kotlin.sourceSets.named(galleryTarget.mainSourceSetName).configure { sourceSet ->
            sourceSet.dependencies {
                implementation(runtimeDependency)
            }
        }

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val artboardCompilation = target.compilations.maybeCreate(ARTBOARD_COMPILATION_NAME)
        artboardCompilation.associateWith(mainCompilation)
        artboardCompilation.defaultSourceSet.kotlin.srcDir(
            generateHost.flatMap { it.outputDirectory.dir("kotlin") },
        )
        artboardCompilation.compileTaskProvider.configure { it.dependsOn(generateHost) }

        // Keep intermediate resource trees free of Finder/iCloud "file 2.ext" junk so
        // Compose resource lookup and the snapshot classpath do not see duplicates.
        listOf(mainCompilation, artboardCompilation).forEach { compilation ->
            project.tasks.named(compilation.processResourcesTaskName, Copy::class.java).configure { copy ->
                copy.exclude { details -> ConflictCopies.matches(details.file.name) }
                copy.doFirst {
                    ConflictCopies.purge(copy.destinationDir.toPath())
                }
            }
        }

        // Rasterizing offscreen needs the host-OS Skia natives. Scoped to the isolated
        // artboard source set so the consumer's own jvm artifacts never gain a
        // platform-specific dependency, and resolved from the consumer's own Compose
        // version rather than one pinned in Artboard's published POM.
        desktopRuntimeDependency(project)?.let { desktopRuntime ->
            kotlin.sourceSets.named(artboardCompilation.defaultSourceSet.name).configure { sourceSet ->
                sourceSet.dependencies {
                    implementation(desktopRuntime)
                }
            }
        }

        val snapshotDirectory = project.layout.buildDirectory.dir("artboard/snapshots")
        val snapshot = project.tasks.register("artboardSnapshot", JavaExec::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Renders every @Preview to a PNG plus a snapshot manifest"
            task.mainClass.set("$generatedPackage.host.${HostGenerator.SNAPSHOT_MAIN_CLASS}")
            task.classpath(
                artboardCompilation.output.allOutputs,
                mainCompilation.output.allOutputs,
                artboardCompilation.runtimeDependencyFiles,
            )
            task.systemProperty("java.awt.headless", "true")
            task.outputs.dir(snapshotDirectory)
            task.argumentProviders.add {
                listOf(snapshotDirectory.get().asFile.absolutePath)
            }
            task.dependsOn(artboardCompilation.compileTaskProvider)
            // iCloud can re-create "file 2.cvr" under build/ after processResources;
            // purge immediately before the headless renderer reads the classpath.
            // Capture DirectoryProperty (not Project) so configuration cache can serialize this.
            val buildDirectory = project.layout.buildDirectory
            task.doFirst {
                ConflictCopies.purge(buildDirectory.get().asFile.toPath())
            }
        }

        wireSnapshotGallery(
            project = project,
            viewerDistDependency = viewerDistDependency,
            snapshot = snapshot,
            runTasks = runTasks,
            export = export,
        )

        report.configure { it.dependsOn(galleryTarget.kspTaskName) }
        doctor.configure {
            it.dependsOn(report)
            it.dependsOn(artboardCompilation.compileTaskProvider)
        }

        configureDiagnostics(status, doctor) {
            it.hasIsolatedRunTask.set(true)
            it.notes.add("snapshot renderer → isolated '$ARTBOARD_COMPILATION_NAME' compilation")
        }
    }

    /**
     * Registers artboard-codegen on `kspJvm` early enough for KSP's onlyIf check.
     *
     * Mirrors [AndroidHostTest.enableEarly]: the processor must be visible during
     * evaluation, but we only attach it when JVM is the selected gallery target so
     * Wasm-first projects do not get a registry on jvmMain without the runtime.
     */
    private fun registerJvmKspProcessorEarly(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        codegenDependency: String,
    ) {
        kotlin.targets
            .withType(KotlinJvmTarget::class.java)
            .matching { it.name == GalleryTarget.Jvm.targetName }
            .configureEach {
                project.configurations
                    .named(GalleryTarget.Jvm.kspConfigurationName)
                    .configure { configuration ->
                        configuration.dependencies.addLater(
                            project.provider {
                                val selected = GalleryTarget.select(kotlin.targets.names.toSet())
                                if (selected == GalleryTarget.Jvm) {
                                    project.dependencies.create(codegenDependency)
                                } else {
                                    null
                                }
                            },
                        )
                    }
            }
    }

    /**
     * Assembles and serves the snapshot gallery, whichever renderer produced it.
     *
     * The prebuilt viewer is resolved as an ordinary artifact — the consumer never
     * compiles Wasm, they only download it.
     *
     * Task-graph note: a detached configuration + `provider { config.files }` does
     * **not** depend on the tasks that produce the jar. When the coordinate is
     * substituted for `:artboard-viewer-dist` (composite / includeBuild), that
     * left `artboardExport` serving a stale viewer after source changes — the
     * schemaVersion guard then failed at runtime. A resolvable configuration
     * with `dependsOn` + `inputs.files` rebuilds the jar (and its
     * `wasmJsBrowserDistribution` input) whenever the viewer sources change.
     */
    private fun wireSnapshotGallery(
        project: Project,
        viewerDistDependency: String,
        snapshot: TaskProvider<out org.gradle.api.Task>,
        runTasks: List<TaskProvider<ArtboardServeTask>>,
        export: TaskProvider<ArtboardExportTask>,
    ) {
        val viewerDist = project.configurations.maybeCreate(VIEWER_DIST_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            description = "Artboard prebuilt Wasm viewer distribution (snapshot gallery)"
            // wireSnapshotGallery runs once per project; still guard against double-add.
            if (dependencies.isEmpty()) {
                dependencies.add(project.dependencies.create(viewerDistDependency))
            }
        }
        val unpackViewer = project.tasks.register(
            "unpackArtboardViewer",
            Sync::class.java,
        ) { sync ->
            sync.group = ARTBOARD_GROUP
            sync.description = "Unpacks Artboard's prebuilt gallery"
            // Produces task edges to :artboard-viewer-dist:jar (and published jars).
            sync.dependsOn(viewerDist)
            sync.inputs.files(viewerDist)
            // Callable `from` keeps the zip trees tied to the configuration's files.
            sync.from({
                viewerDist.map { archive -> project.zipTree(archive) }
            }) { spec ->
                spec.include("$VIEWER_RESOURCE_PREFIX/**")
                spec.eachFile { details ->
                    details.relativePath = RelativePath(
                        true,
                        *details.relativePath.segments.drop(1).toTypedArray(),
                    )
                }
                spec.includeEmptyDirs = false
            }
            sync.into(project.layout.buildDirectory.dir("artboard/viewer"))
        }

        val runDirectory = project.layout.buildDirectory.dir("artboard/run")
        val assembleGallery = project.tasks.register(
            "syncArtboardSnapshotGallery",
            Sync::class.java,
        ) { sync ->
            sync.group = ARTBOARD_GROUP
            sync.description = "Assembles the snapshot gallery from the viewer and rendered images"
            sync.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            sync.from(unpackViewer)
            sync.from(snapshot)
            sync.into(runDirectory)
        }

        // The prebuilt bundle is self-contained, so no bare module specifiers exist and
        // the serve task's import-map injection is a no-op. This directory is only ever
        // consulted for /node_modules/ requests that snapshot mode never makes.
        val absentNodeModules = project.layout.buildDirectory.dir("artboard/no-node-modules")
        runTasks.forEach { run ->
            run.configure { task ->
                task.contentDirectory.set(runDirectory)
                task.nodeModulesDirectory.set(absentNodeModules)
                task.dependsOn(assembleGallery)
            }
        }
        export.configure { task ->
            // TaskProvider `from` tracks unpack outputs; explicit dependsOn keeps the
            // snapshot renderer and viewer unpack on the critical path for export.
            task.contentDirectories.from(unpackViewer)
            task.contentDirectories.from(snapshot)
            task.nodeModulesDirectory.set(absentNodeModules)
            task.dependsOn(unpackViewer, snapshot)
        }
    }

    /**
     * Resolves `compose.desktop.currentOs` from the consumer's Compose extension.
     *
     * Returns null when the Compose plugin is absent; `artboardDoctor` already
     * reports that as the real problem.
     */
    private fun desktopRuntimeDependency(project: Project): Any? = runCatching {
        project.extensions.getByType(ComposeExtension::class.java).dependencies.desktop.currentOs
    }.getOrNull()

    @Suppress("LongParameterList")
    private fun configureWasmTarget(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        target: KotlinJsIrTarget,
        galleryTarget: GalleryTarget,
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
            it.galleryTarget.set(galleryTarget.label)
            it.notes.add("${galleryTarget.mainSourceSetName} → Artboard runtime $pluginVersion")
            it.notes.add("${galleryTarget.kspConfigurationName} → Artboard preview discovery")
        }

        project.dependencies.add(galleryTarget.kspConfigurationName, codegenDependency)
        kotlin.sourceSets.named(galleryTarget.mainSourceSetName).configure { sourceSet ->
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

        report.configure { it.dependsOn(galleryTarget.kspTaskName) }
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

    private const val ARTBOARD_COMPILATION_NAME = "artboard"
    private const val ARTBOARD_GROUP = "artboard"

    /** Must match `viewerResourcePrefix` in `artboard-viewer-dist/build.gradle.kts`. */
    private const val VIEWER_RESOURCE_PREFIX = "artboard-viewer"

    /** Resolvable configuration for the prebuilt viewer jar (snapshot mode). */
    private const val VIEWER_DIST_CONFIGURATION = "artboardViewerDist"

    // Names AGP gives the host-test compilation that AndroidHostTest enables.
    private const val COMPOSE_PLUGIN_ID = "org.jetbrains.compose"

    /** Compose's per-variant task that stages `composeResources/<package>` for Android. */
    private const val COMPOSE_ANDROID_ASSETS_TASK_SUFFIX = "ComposeResourcesToAndroidAssets"

    private const val ANDROID_HOST_TEST_SOURCE_SET = "androidHostTest"
    private const val ANDROID_HOST_TEST_COMPILE_TASK = "compileAndroidHostTest"
    private const val ANDROID_HOST_TEST_TASK = "testAndroidHostTest"

    // Artboard's own Android rendering toolchain, pinned the same way the plugin pins
    // its codegen. Deliberately NOT derived from the consumer's Compose version —
    // nothing here is Compose-version-coupled. Versions come from the root version
    // catalog via ArtboardVersions so dependency-update tooling can see and bump them.
    private val JUNIT_DEPENDENCY = "junit:junit:${ArtboardVersions.JUNIT}"
    private val ROBOLECTRIC_DEPENDENCY = "org.robolectric:robolectric:${ArtboardVersions.ROBOLECTRIC}"
    private val ROBORAZZI_DEPENDENCY =
        "io.github.takahirom.roborazzi:roborazzi:${ArtboardVersions.ROBORAZZI}"
    private val ROBORAZZI_COMPOSE_DEPENDENCY =
        "io.github.takahirom.roborazzi:roborazzi-compose:${ArtboardVersions.ROBORAZZI}"
}
