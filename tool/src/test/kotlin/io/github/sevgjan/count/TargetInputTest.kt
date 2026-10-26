package io.github.sevgjan.count

import kotlin.test.Test
import kotlin.test.assertEquals

class TargetInputTest {
    private fun type(keys: String, start: String = "") = keys.fold(start, TargetInput::press)

    @Test
    fun typesDigits() = assertEquals("120", type("120"))

    @Test
    fun noLeadingZero() = assertEquals("5", type("005"))

    @Test
    fun backspaceRemovesLastDigit() = assertEquals("1", type("12<"))

    @Test
    fun backspaceOnEmptyStaysEmpty() = assertEquals("", type("<<"))

    @Test
    fun capsAtMaxDigits() = assertEquals("123456", type("12345678"))

    @Test
    fun editsExistingTarget() = assertEquals("45", type("<5", start = "40"))
}
