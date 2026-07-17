package artboard.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.security.MessageDigest

/** Gradle integration for Artboard's source-free Compose preview gallery. */
class ArtboardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("artboard", ArtboardExtension::class.java)
        extension.title.convention(project.name)

        val generatedPackage = generatedPackage(project)
        val resourceRoots = listOf(
            project.layout.projectDirectory.dir("src/commonMain/composeResources"),
            project.layout.projectDirectory.dir("src/composeResources"),
        )
        val composeResourceFiles = project.fileTree(project.layout.projectDirectory.dir("src")) {
            it.include("**/composeResources/**")
        }

        val generateHost = project.tasks.register(
            "generateArtboardHost",
            GenerateArtboardHostTask::class.java,
        ) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Generates the isolated Artboard browser host"
            task.registryPackage.set(generatedPackage)
            task.hostPackage.set("$generatedPackage.host")
            task.title.set(extension.title)
            task.entryScript.convention("artboard.mjs")
            task.composeResourceRoots.from(resourceRoots)
            task.outputDirectory.set(project.layout.buildDirectory.dir("generated/artboard/host"))
        }

        val report = project.tasks.register("artboardReport", ArtboardReportTask::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Generates the machine-readable Artboard preview report"
            task.projectDirectory.set(project.layout.projectDirectory)
            task.generatedReports.from(
                project.layout.buildDirectory.dir("generated/ksp").map { directory ->
                    directory.asFileTree.matching { it.include("**/artboard-previews.json") }
                },
            )
            task.reportFile.set(project.layout.buildDirectory.file("reports/artboard/previews.json"))
        }

        val status = diagnosticTask(
            project = project,
            name = "artboardStatus",
            description = "Prints Artboard plugin status without failing",
            generatedPackage = generatedPackage,
            composeResourceFiles = composeResourceFiles,
            failWhenNotReady = false,
        )
        val doctor = diagnosticTask(
            project = project,
            name = "artboardDoctor",
            description = "Verifies that the isolated Artboard gallery can compile and run",
            generatedPackage = generatedPackage,
            composeResourceFiles = composeResourceFiles,
            failWhenNotReady = true,
        )
        val run = project.tasks.register("artboardRun", ArtboardServeTask::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Runs the isolated Artboard Wasm browser gallery"
            task.dependsOn(doctor)
        }
        val runLan = project.tasks.register("artboardRunLan", ArtboardServeTask::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Runs the Artboard Wasm gallery for devices on the local network"
            task.bindAddress.set(ALL_INTERFACES_ADDRESS)
            task.dependsOn(doctor)
        }
        val export = project.tasks.register("artboardExport", ArtboardExportTask::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = "Exports an optimized Artboard gallery for static hosting"
            task.outputDirectory.set(project.layout.buildDirectory.dir("artboard/export"))
            task.dependsOn(doctor)
        }

        project.pluginManager.withPlugin(COMPOSE_ID) {
            configureDiagnostics(status, doctor) { it.hasCompose.set(true) }
        }
        project.pluginManager.withPlugin(KOTLIN_MPP_ID) {
            configureDiagnostics(status, doctor) { it.hasKotlinMultiplatform.set(true) }
            project.pluginManager.apply(KSP_ID)
            configureDiagnostics(status, doctor) { it.hasKsp.set(true) }
            project.extensions.configure(KspExtension::class.java) { ksp ->
                ksp.arg("artboard.package", generatedPackage)
                ksp.arg("artboard.projectPath", project.path)
            }

            ArtboardKotlinIntegration.configure(
                project = project,
                generatedPackage = generatedPackage,
                runtimeDependency = artboardDependency("artboard-runtime"),
                codegenDependency = artboardDependency("artboard-codegen"),
                pluginVersion = pluginVersion(),
                generateHost = generateHost,
                report = report,
                status = status,
                doctor = doctor,
                runTasks = listOf(run, runLan),
                export = export,
            )
        }
    }

    private fun diagnosticTask(
        project: Project,
        name: String,
        description: String,
        generatedPackage: String,
        composeResourceFiles: Any,
        failWhenNotReady: Boolean,
    ): TaskProvider<ArtboardDoctorTask> =
        project.tasks.register(name, ArtboardDoctorTask::class.java) { task ->
            task.group = ARTBOARD_GROUP
            task.description = description
            task.projectDirectory.set(project.layout.projectDirectory)
            task.projectPath.set(project.path)
            task.generatedPackage.set(generatedPackage)
            task.composeResources.from(composeResourceFiles)
            task.failWhenNotReady.set(failWhenNotReady)
        }

    private fun configureDiagnostics(
        first: TaskProvider<ArtboardDoctorTask>,
        second: TaskProvider<ArtboardDoctorTask>,
        configure: (ArtboardDoctorTask) -> Unit,
    ) {
        first.configure(configure)
        second.configure(configure)
    }

    private fun artboardDependency(module: String): String =
        "$ARTBOARD_GROUP_ID:$module:${pluginVersion()}"

    private fun pluginVersion(): String =
        ArtboardPlugin::class.java.`package`.implementationVersion
            ?.takeIf(String::isNotBlank)
            ?: FALLBACK_VERSION

    private fun generatedPackage(project: Project): String {
        val input = "${project.rootProject.name}:${project.path}"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(12)
        return "artboard.generated.p$hash"
    }

    private companion object {
        const val ARTBOARD_GROUP = "artboard"
        const val ARTBOARD_GROUP_ID = "io.github.crowded-libs.artboard"
        const val FALLBACK_VERSION = "0.1.2"
        const val KOTLIN_MPP_ID = "org.jetbrains.kotlin.multiplatform"
        const val COMPOSE_ID = "org.jetbrains.compose"
        const val KSP_ID = "com.google.devtools.ksp"
    }
}
