package artboard.snapshot

import java.util.Locale

/**
 * Runs [block] with the platform default locale forced to [localeTag].
 *
 * Compose Multiplatform resources resolve `stringResource` through an environment
 * built from `androidx.compose.ui.text.intl.Locale.current`, which on both JVM and
 * Android reads `java.util.Locale.getDefault()` on every access. Its own override
 * seam (`LocalComposeEnvironment`) is `internal`, so the platform locale is the only
 * supported way in — and it has to be in place *before* the scene composes, since a
 * composition-scoped override lands after the first read.
 *
 * Without this a right-to-left locale still flips layout direction (Artboard derives
 * that from the tag itself) while every string stays in the default language, which
 * reads as a working translation until you look closely.
 *
 * A null or blank tag means "follow the platform", so the default is left untouched.
 * Restoration is unconditional: snapshot rendering is single-threaded by contract,
 * but the default is process-global and must not leak into the rest of the build.
 */
fun <T> withSnapshotLocale(localeTag: String?, block: () -> T): T {
    val replacement = localeTag
        ?.replace('_', '-')
        ?.takeIf(String::isNotBlank)
        ?.let(Locale::forLanguageTag)
        ?: return block()

    val previous = Locale.getDefault()
    Locale.setDefault(replacement)
    return try {
        block()
    } finally {
        Locale.setDefault(previous)
    }
}
