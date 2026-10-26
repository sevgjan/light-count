package io.github.sevgjan.count

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

private const val BACKSPACE = '<'
private val KEYS = listOf("123", "456", "789", " 0$BACKSPACE")

/**
 * Number pad for a counter's target. Returns the digits typed ("" for no target), or nothing
 * when the user backs out.
 */
class TargetScreen(
    sealedActivity: SealedLightActivity,
    private val initialValue: String = "",
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var digits by remember { mutableStateOf(initialValue) }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Target"),
                )

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    LightText(
                        text = digits.ifEmpty { "none" },
                        variant = LightTextVariant.Title,
                        lighten = digits.isEmpty(),
                        align = TextAlign.Center,
                        maxLines = 1,
                    )
                }

                NumberPad { key -> digits = TargetInput.press(digits, key) }

                LightBottomBar(
                    listOf(
                        LightBarButton.Text(text = "NONE", onClick = { goBack("") }),
                        LightBarButton.Text(text = "SET", onClick = { goBack(digits) }),
                    ),
                )
            }
        }
    }
}

/** What a key press does to the digits typed so far. */
internal object TargetInput {
    /** Targets up to 999 999 fit the counter screen. */
    const val MAX_DIGITS = 6

    fun press(digits: String, key: Char): String = when {
        key == BACKSPACE -> digits.dropLast(1)
        !key.isDigit() -> digits
        key == '0' && digits.isEmpty() -> digits // no leading zero
        digits.length >= MAX_DIGITS -> digits
        else -> digits + key
    }
}

@Composable
private fun NumberPad(onKey: (Char) -> Unit) {
    // A clear gap above NONE / SET, so reaching for ⌫ or 0 doesn't land on them.
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2f.gridUnitsAsDp())) {
        KEYS.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.8f.gridUnitsAsDp())
                            .then(if (key == ' ') Modifier else Modifier.lightClickable { onKey(key) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key != ' ') {
                            LightText(
                                text = if (key == BACKSPACE) "⌫" else key.toString(),
                                variant = LightTextVariant.Subtitle,
                            )
                        }
                    }
                }
            }
        }
    }
}
