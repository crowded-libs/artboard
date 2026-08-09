package artboard.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Name AGP gives the host-test compilation when one already exists. */
private const val HOST_TEST_COMPILATION_NAME = "hostTest"

/**
 * Turns on a host-test compilation for a consumer's Android target.
 *
 * Android previews are rendered by Robolectric on the JVM, which needs a host-test
 * compilation. AGP's KMP library plugin does not create one unless the build asks for
 * it via `android { withHostTestBuilder { … } }` — consumer-side DSL that Artboard's
 * contract says consumers should never have to write. Calling the same public API from
 * the plugin keeps snapshot mode zero-config.
 *
 * AGP allows only one host-test builder per target. Enabling it reactively as soon as
 * the Android target is registered races a consumer who already configures their own
 * host tests (e.g. `withHostTest { … }` for real unit tests): Artboard would claim the
 * slot first, and the *consumer's* subsequent script line would throw. Defer Artboard's
 * own call to [KotlinMultiplatformAndroidComponentsExtension.finalizeDsl] — after every
 * build script has finished configuring the target, but still early enough for AGP's
 * variant computation (plain Gradle `afterEvaluate` is too late for this DSL mutation).
 *
 * If a host-test compilation already exists by then, Artboard reuses it instead of
 * fighting over the single allowed slot.
 *
 * If AGP ever stops allowing any of this, [enabled] stays false and `artboardDoctor`
 * falls back to telling the consumer to add the one-line opt-in themselves.
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
     * Wires the KSP registration as soon as the Android target appears, but defers the
     * host-test-builder call to [KotlinMultiplatformAndroidComponentsExtension.finalizeDsl]
     * (see the class doc).
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

            // Deferred past the consumer's own androidLibrary { } block so their
            // withHostTest / withHostTestBuilder (if any) runs first.
            runCatching {
                project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
            }.onFailure { error ->
                enabled = false
                failureReason = error.message ?: error::class.java.simpleName
            }.onSuccess { components ->
                components.finalizeDsl {
                    if (target.compilations.findByName(HOST_TEST_COMPILATION_NAME) != null) {
                        // Consumer already has host tests — reuse rather than fight over
                        // the one-per-target slot. We cannot retroactively force
                        // isReturnDefaultValues on their builder, so an unstubbed
                        // framework call may fail the render instead of degrading quietly.
                        enabled = true
                        failureReason = null
                        return@finalizeDsl
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
    }
}
