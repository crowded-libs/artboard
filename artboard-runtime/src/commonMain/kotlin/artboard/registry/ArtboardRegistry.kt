package artboard.registry

import artboard.model.PreviewFrame

/**
 * Catalog of preview frames. Codegen will emit implementations; hand-written
 * registries are fine for samples and early development.
 */
interface ArtboardRegistry {
    /** Stable, immutable preview frames exposed by this registry. */
    val frames: List<PreviewFrame>
}

/** Empty registry used by the seed host until discovery is wired. */
object EmptyArtboardRegistry : ArtboardRegistry {
    override val frames: List<PreviewFrame> = emptyList()
}

/** Combines multiple registries (e.g. multi-module aggregation later). */
class CompositeArtboardRegistry(
    private val registries: List<ArtboardRegistry>,
) : ArtboardRegistry {
    constructor(vararg registries: ArtboardRegistry) : this(registries.toList())

    override val frames: List<PreviewFrame> = registries.flatMap { it.frames }
}

/** Resolves a canonical frame ID or an unambiguous legacy source FQCN. */
fun ArtboardRegistry.resolveFrameId(candidate: String?): String? {
    val value = candidate?.takeIf(String::isNotBlank) ?: return null
    frames.firstOrNull { it.id == value }?.let { return it.id }
    val legacyMatches = frames.filter { it.sourceFqName == value }
    return legacyMatches.singleOrNull()?.id
}
