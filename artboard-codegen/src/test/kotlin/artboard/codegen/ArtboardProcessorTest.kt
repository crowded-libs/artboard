package artboard.codegen

import kotlin.test.Test
import kotlin.test.assertEquals

class ArtboardProcessorTest {
    @Test
    fun kindInferenceUsesWholeNameSegments() {
        assertEquals("Screen", inferKind("CheckoutScreenPreview", "Default"))
        assertEquals("Screen", inferKind("CheckoutPreview", "Empty Page"))
        assertEquals("Component", inferKind("ScreeningCardPreview", "Default"))
    }

    @Test
    fun generatedLiteralsEscapeKotlinAndJsonCorrectly() {
        val value = "quote \" slash \\ newline\n dollar $ control \u0001 form\u000C"
        assertEquals(
            "\"quote \\\" slash \\\\ newline\\n dollar \\$ control \\u0001 form\\u000c\"",
            value.kotlinQuote(),
        )
        assertEquals(
            "\"quote \\\" slash \\\\ newline\\n dollar $ control \\u0001 form\\f\"",
            value.jsonQuote(),
        )
    }

    @Test
    fun generatedCallableEscapesKotlinKeywordsAndUnusualNames() {
        assertEquals("com.example.`when`", kotlinCallableReference("com.example", "when"))
        assertEquals("com.`is`.`wide preview`", kotlinCallableReference("com.is", "wide preview"))
    }
}
