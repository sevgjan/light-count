package io.github.sevgjan.count

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CounterLogicTest {
    private val counter = Counter(id = 1, name = "Rows")

    @Test
    fun incrementAndDecrement() {
        val up = CounterLogic.apply(counter, CounterAction.Increment).counter
        assertEquals(1, up.value)
        assertEquals(0, CounterLogic.apply(up, CounterAction.Decrement).counter.value)
    }

    @Test
    fun neverBelowZero() {
        assertEquals(0, CounterLogic.apply(counter, CounterAction.Decrement).counter.value)
    }

    @Test
    fun reachingTargetIsReportedOnce() {
        val nearly = counter.copy(value = 9, target = 10)
        assertTrue(CounterLogic.apply(nearly, CounterAction.Increment).reachedTarget)
        val past = counter.copy(value = 10, target = 10)
        assertFalse(CounterLogic.apply(past, CounterAction.Increment).reachedTarget)
        assertFalse(CounterLogic.apply(nearly.copy(value = 11), CounterAction.Decrement).reachedTarget)
    }

    @Test
    fun targetParsing() {
        assertEquals(40, CounterLogic.parseTarget(" 40 "))
        assertNull(CounterLogic.parseTarget(""))
        assertNull(CounterLogic.parseTarget("0"))
        assertNull(CounterLogic.parseTarget("abc"))
        assertNull(CounterLogic.apply(counter.copy(target = 5), CounterAction.SetTarget(0)).counter.target)
    }

    @Test
    fun progressText() {
        assertEquals("3", CounterLogic.progress(counter.copy(value = 3)))
        assertEquals("3 / 40", CounterLogic.progress(counter.copy(value = 3, target = 40)))
    }

    @Test
    fun resetKeepsTarget() {
        val reset = CounterLogic.apply(counter.copy(value = 7, target = 9), CounterAction.Reset).counter
        assertEquals(0, reset.value)
        assertEquals(9, reset.target)
    }
}
