package artboard.canvas

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FitRequestTrackerTest {
    @Test
    fun consumesOnlyNewReadyFitRequests() {
        val tracker = FitRequestTracker(initialToken = 0)

        assertFalse(tracker.consumeIfReady(token = 0, ready = true))
        assertFalse(tracker.consumeIfReady(token = 1, ready = false))
        assertTrue(tracker.consumeIfReady(token = 1, ready = true))
        assertFalse(tracker.consumeIfReady(token = 1, ready = true))
        assertTrue(tracker.consumeIfReady(token = 2, ready = true))
    }

    @Test
    fun recreationTreatsCurrentTokenAsAlreadyConsumed() {
        val tracker = FitRequestTracker(initialToken = 7)

        assertFalse(tracker.consumeIfReady(token = 7, ready = true))
        assertTrue(tracker.consumeIfReady(token = 8, ready = true))
    }
}
