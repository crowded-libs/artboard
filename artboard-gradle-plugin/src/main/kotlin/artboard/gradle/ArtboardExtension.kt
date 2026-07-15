package artboard.gradle

import org.gradle.api.provider.Property

/**
 * Consumer DSL:
 *
 * ```
 * artboard { title.set("Design system") }
 * ```
 */
abstract class ArtboardExtension {
    /** Gallery window title. Defaults to the Gradle project name. */
    abstract val title: Property<String>
}
