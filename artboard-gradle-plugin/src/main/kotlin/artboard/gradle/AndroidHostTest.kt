package artboard.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Turns on a host-test compilation for a consumer's Android target.
 *
 * Android previews are rendered by Robolectric on the JVM, which needs a host-test
 * compilation. AGP's KMP library plugin does not create one unless the build asks for
 * it via `android { withHostTestBuilder { … } }` — consumer-side DSL that Artboard's
 * contract says consumers should never have to write. Calling the same public API from
 * the plugin keeps snapshot mode zero-config.
 *
 * If AGP ever stops allowing this, [enabled] stays false and `artboardDoctor` falls
 * back to telling the consumer to add the one-line opt-in themselves.
 */
internal object AndroidHostTest {

    /** Whether the plugin managed to enable the host-test compilation. */
    @Volatile
    var enabled: Boolean = false
        private set

    /** Reason enabling failed, for the doctor's remedy text. */
    @Volatile
    var failureReason: String? = null
        private set

    /**
     * Registers the host-test compilation as soon as the Android target appears.
     *
     * Must not be deferred to `afterEvaluate`: AGP finalizes its variants during
     * evaluation, and a builder registered afterwards is ignored.
     */
    fun enableEarly(project: Project, kotlin: KotlinMultiplatformExtension, codegenDependency: String) {
        val targets = runCatching {
            kotlin.targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        }.getOrElse {
            // AGP's KMP library plugin is not on the classpath; nothing to enable.
            return
        }

        targets.configureEach { target ->
            if (target.name != GalleryTarget.Android.targetName) return@configureEach

            // KSP gates its Android task on a processor classpath captured during
            // evaluation, so registering the processor from afterEvaluate leaves the
            // task permanently skipped. But whether Android is the gallery target is
            // only knowable once every target is declared — adding it unconditionally
            // would emit a registry into androidMain for projects where Wasm wins,
            // which then fails to compile because the runtime is not on that classpath.
            // addLater keeps the registration early while deferring the decision.
            project.configurations
                .named(GalleryTarget.Android.kspConfigurationName)
                .configure { configuration ->
                    configuration.dependencies.addLater(
                        project.provider {
                            val selected = GalleryTarget.select(kotlin.targets.names.toSet())
                            if (selected == GalleryTarget.Android) {
                                project.dependencies.create(codegenDependency)
                            } else {
                                null
                            }
                        },
                    )
                }

            runCatching {
                target.withHostTestBuilder { }.configure {
                    // Robolectric needs real resources; returning defaults instead of
                    // throwing keeps unimplemented framework calls from killing a render.
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
            }.fold(
                onSuccess = {
                    enabled = true
                    failureReason = null
                },
                onFailure = { error ->
                    enabled = false
                    failureReason = error.message ?: error::class.java.simpleName
                    project.logger.info(
                        "Artboard could not enable the Android host-test compilation: $failureReason",
                    )
                },
            )
        }
    }
}
