package artboard.gradle

/**
 * How a consumer's gallery is produced.
 *
 * Public because it is an input of [GenerateArtboardHostTask].
 */
enum class GalleryMode {
    /** Interactive Wasm gallery compiled from the consumer's own `wasmJs` target. */
    Live,

    /**
     * Pre-rendered snapshot images browsed in Artboard's prebuilt viewer.
     *
     * Trades interactivity for not requiring a `wasmJs` target in the consumer.
     * Rendered offscreen with Skia.
     */
    Snapshot,

    /**
     * Snapshot images rendered on Android, by Robolectric on the JVM.
     *
     * Same output as [Snapshot]; a separate mode because Android has no
     * `ImageComposeScene`, so the entry point is a JUnit test rather than a `main`.
     */
    AndroidSnapshot,
}

/**
 * A consumer-declared target Artboard can build a gallery from.
 *
 * Artboard never adds a target. It binds to whatever the consumer already declares
 * and reports the choice through `artboardDoctor`, so opting into a heavier target
 * stays the consumer's decision.
 *
 * Declaration order is precedence order: an interactive gallery wins whenever the
 * consumer already pays for a target that can host one.
 */
internal enum class GalleryTarget(
    val targetName: String,
    val kspConfigurationName: String,
    val mainSourceSetName: String,
    val kspTaskName: String,
    val mode: GalleryMode,
    val label: String,
) {
    Wasm(
        targetName = "wasmJs",
        kspConfigurationName = "kspWasmJs",
        mainSourceSetName = "wasmJsMain",
        kspTaskName = "kspKotlinWasmJs",
        mode = GalleryMode.Live,
        label = "wasmJs (live gallery)",
    ),
    Jvm(
        targetName = "jvm",
        kspConfigurationName = "kspJvm",
        mainSourceSetName = "jvmMain",
        kspTaskName = "kspKotlinJvm",
        mode = GalleryMode.Snapshot,
        label = "jvm (snapshot gallery)",
    ),
    Android(
        targetName = "android",
        kspConfigurationName = "kspAndroid",
        mainSourceSetName = "androidMain",
        kspTaskName = "kspAndroidMain",
        mode = GalleryMode.AndroidSnapshot,
        label = "android (snapshot gallery)",
    ),
    ;

    companion object {
        /** Picks the gallery target from the target names a consumer declared. */
        fun select(declaredTargetNames: Set<String>): GalleryTarget? =
            entries.firstOrNull { it.targetName in declaredTargetNames }
    }
}
