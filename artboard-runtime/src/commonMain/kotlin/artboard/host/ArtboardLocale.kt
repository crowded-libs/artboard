package artboard.host

/**
 * A language the gallery can preview, usually derived from the project's
 * `composeResources/values*` folders.
 *
 * @param tag BCP-47 language tag (`ar`, `en`, …). `null` means system/browser default.
 * @param label Short UI label for the locale control.
 */
data class ArtboardLocale(
    val tag: String?,
    val label: String,
) {
    companion object {
        val System: ArtboardLocale = ArtboardLocale(tag = null, label = "System")
    }
}

/**
 * Builds the gallery locale list: [ArtboardLocale.System] plus one entry per
 * discovered resource language tag (from `values-<lang>` folders).
 */
fun artboardLocalesFromResourceTags(
    resourceLanguageTags: Collection<String>,
    labelForTag: (String) -> String = { tag -> defaultLocaleLabel(tag) },
): List<ArtboardLocale> {
    val tags = resourceLanguageTags
        .map { it.replace('_', '-').substringBefore('-').lowercase() }
        .filter { it.isNotBlank() && it != "values" }
        .distinct()
        .sorted()
    return buildList {
        add(ArtboardLocale.System)
        tags.forEach { tag ->
            add(ArtboardLocale(tag = tag, label = labelForTag(tag)))
        }
    }
}

/** English display names for common gallery languages; falls back to the tag. */
fun defaultLocaleLabel(tag: String): String = when (tag.lowercase()) {
    "en" -> "English"
    "ar" -> "Arabic"
    "es" -> "Spanish"
    "fr" -> "French"
    "de" -> "German"
    "he", "iw" -> "Hebrew"
    "fa" -> "Persian"
    "ja" -> "Japanese"
    "ko" -> "Korean"
    "zh" -> "Chinese"
    "pt" -> "Portuguese"
    "ru" -> "Russian"
    "hi" -> "Hindi"
    else -> tag
}
