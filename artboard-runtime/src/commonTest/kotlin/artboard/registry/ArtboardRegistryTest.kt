package artboard.registry

import artboard.model.PreviewFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtboardRegistryTest {
    @Test
    fun resolvesCanonicalAndUnambiguousLegacyIds() {
        val registry = registry(
            frame("demo.FirstPreview::Light", "demo.FirstPreview"),
            frame("demo.SecondPreview::Default", "demo.SecondPreview"),
        )

        assertEquals("demo.FirstPreview::Light", registry.resolveFrameId("demo.FirstPreview::Light"))
        assertEquals("demo.SecondPreview::Default", registry.resolveFrameId("demo.SecondPreview"))
        assertNull(registry.resolveFrameId("demo.MissingPreview"))
    }

    @Test
    fun rejectsAmbiguousLegacyIds() {
        val registry = registry(
            frame("demo.RepeatedPreview::Light", "demo.RepeatedPreview"),
            frame("demo.RepeatedPreview::Dark", "demo.RepeatedPreview"),
        )

        assertNull(registry.resolveFrameId("demo.RepeatedPreview"))
    }

    private fun registry(vararg frames: PreviewFrame): ArtboardRegistry = object : ArtboardRegistry {
        override val frames: List<PreviewFrame> = frames.toList()
    }

    private fun frame(id: String, source: String) = PreviewFrame(
        id = id,
        name = id.substringAfter("::"),
        sourceFqName = source,
        content = {},
    )
}
