package com.xnotes.ui

import androidx.compose.runtime.MonotonicFrameClock
import com.xnotes.core.workspace.NoteTabs
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DocumentTabsSelectionStateTest {
    private class Frames : MonotonicFrameClock {
        private val waiting = mutableListOf<(Long) -> Unit>()
        private var now = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = suspendCancellableCoroutine { continuation ->
            val resume: (Long) -> Unit = { time ->
                if (continuation.isActive) {
                    try { continuation.resume(onFrame(time)) } catch (e: Throwable) { continuation.resumeWithException(e) }
                }
            }
            waiting.add(resume)
            continuation.invokeOnCancellation { waiting.remove(resume) }
        }
        suspend fun tick(count: Int = 1) {
            repeat(count) {
                repeat(4) { yield() }
                now += 16_000_000L
                val frame = waiting.toList(); waiting.clear()
                frame.forEach { it(now) }
                repeat(4) { yield() }
            }
        }
    }
    private val tabs = NoteTabs().open("a", "First").open("b", "Second").open("c", "Third")

    @Test fun selectionInterpolatesPositionAndTintInWorkspaceOwnedState() {
        val frames = Frames()
        runBlocking(frames) {
            val motion = DocumentTabsSelectionState(tabs, "a")
            val job = launch { motion.select(tabs, "b") }
            frames.tick(4)
            assertTrue(motion.position.value > 0f && motion.position.value < 1f)
            assertTrue(motion.weight("b") > 0f && motion.weight("b") < 1f)
            assertTrue(motion.weight("a") > 0f && motion.weight("a") < 1f)
            // A new host receives this same state; it must observe the in-flight position.
            val newHost = motion
            assertEquals(motion.position.value, newHost.position.value, 0f)
            frames.tick(50)
            assertTrue(job.isCompleted)
            assertEquals(1f, newHost.position.value, .001f)
            assertEquals(1f, newHost.weight("b"), .001f)
        }
    }

    @Test fun retargetingDoesNotResetTheIndicatorAndClosedTabsReleaseTintState() {
        val frames = Frames()
        runBlocking(frames) {
            val motion = DocumentTabsSelectionState(tabs, "a")
            val first = launch { motion.select(tabs, "b") }
            frames.tick(3)
            val inFlight = motion.position.value
            first.cancelAndJoin()
            val second = launch { motion.select(tabs, "c") }
            assertEquals(inFlight, motion.position.value, 0f)
            frames.tick(50)
            assertTrue(second.isCompleted)
            assertEquals(2f, motion.position.value, .001f)
            val closing = launch { motion.select(tabs.close("a"), "c") }
            frames.tick(50)
            assertTrue(closing.isCompleted)
            assertEquals(0f, motion.weight("a"), 0f)
            assertEquals(1f, motion.position.value, .001f)
        }
    }
}
