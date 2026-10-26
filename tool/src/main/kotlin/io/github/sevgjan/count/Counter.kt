package io.github.sevgjan.count

import android.view.KeyEvent
import kotlinx.serialization.Serializable

@Serializable
data class Counter(
    val id: Long,
    val name: String,
    val value: Int = 0,
    val target: Int? = null,
)

sealed interface CounterAction {
    data object Increment : CounterAction
    data object Decrement : CounterAction
    data object Reset : CounterAction
    data class SetTarget(val target: Int?) : CounterAction
}

data class CounterUpdate(val counter: Counter, val reachedTarget: Boolean)

object CounterLogic {
    fun apply(counter: Counter, action: CounterAction): CounterUpdate {
        val next = when (action) {
            CounterAction.Increment -> counter.copy(value = counter.value + 1)
            CounterAction.Decrement -> counter.copy(value = (counter.value - 1).coerceAtLeast(0))
            CounterAction.Reset -> counter.copy(value = 0)
            is CounterAction.SetTarget -> counter.copy(target = action.target?.takeIf { it > 0 })
        }
        val reached = action == CounterAction.Increment && next.target != null && next.value == next.target
        return CounterUpdate(next, reached)
    }

    /** "12" or "12 / 40" */
    fun progress(counter: Counter): String =
        counter.target?.let { "${counter.value} / $it" } ?: counter.value.toString()

    fun parseTarget(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }
}

/**
 * Hardware controls that count. Volume keys and the camera button exist everywhere; the LP3
 * scroll wheel uses LightOS-only key labels, resolved at runtime so other builds simply skip them.
 */
object CountKeys {
    private val wheelUp by lazy { keyCode("WHEEL_CCW") }
    private val wheelDown by lazy { keyCode("WHEEL_CW") }

    fun actionFor(keyCode: Int): CounterAction? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_CAMERA -> CounterAction.Increment
        KeyEvent.KEYCODE_VOLUME_DOWN -> CounterAction.Decrement
        wheelDown -> CounterAction.Increment
        wheelUp -> CounterAction.Decrement
        else -> null
    }

    private fun keyCode(label: String): Int =
        runCatching { KeyEvent.keyCodeFromString(label) }.getOrDefault(KeyEvent.KEYCODE_UNKNOWN)
            .takeIf { it != KeyEvent.KEYCODE_UNKNOWN } ?: Int.MIN_VALUE
}
