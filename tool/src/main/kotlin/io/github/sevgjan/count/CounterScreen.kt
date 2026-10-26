package io.github.sevgjan.count

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightVibrationUsage
import com.thelightphone.sdk.LightVibrationWaveform
import com.thelightphone.sdk.LightVibrator
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.vibrationAmplitudesOf
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TARGET_PATTERN = LightVibrationWaveform(
    durations = listOf(200, 120, 200, 120, 450).map { it.milliseconds },
    amplitudes = vibrationAmplitudesOf(255, 0, 255, 0, 255),
)

class CounterViewModel(
    private val store: CounterStore,
    private val id: Long,
    private val vibrator: LightVibrator,
) : LightViewModel<Unit>() {
    val counter: StateFlow<Counter?> = store.counter(id).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val resetArmed = MutableStateFlow(false)
    private var disarmJob: Job? = null

    fun perform(action: CounterAction, fromKey: Boolean = false) {
        viewModelScope.launch {
            val current = store.counter(id).first() ?: return@launch
            val update = CounterLogic.apply(current, action)
            store.replace(update.counter)
            when {
                update.reachedTarget -> vibrator.vibrate(TARGET_PATTERN, LightVibrationUsage.Alarm)
                fromKey -> vibrator.click()
            }
        }
    }

    /** First tap arms, second tap within 3 s resets. */
    fun reset() {
        if (resetArmed.value) {
            disarmJob?.cancel()
            resetArmed.value = false
            perform(CounterAction.Reset)
            return
        }
        resetArmed.value = true
        disarmJob = viewModelScope.launch {
            delay(3.seconds)
            resetArmed.value = false
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            store.delete(id)
            onDeleted()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val action = CountKeys.actionFor(keyCode) ?: return false
        if (event.repeatCount == 0) perform(action, fromKey = true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = CountKeys.actionFor(keyCode) != null
}

class CounterScreen(
    private val sealedActivity: SealedLightActivity,
    private val store: CounterStore,
    private val id: Long,
) : LightScreen<Unit, CounterViewModel>(sealedActivity) {

    override val viewModelClass: Class<CounterViewModel>
        get() = CounterViewModel::class.java

    override fun createViewModel() = CounterViewModel(store, id, sealedActivity.vibrator)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val counter by viewModel.counter.collectAsState()
        val resetArmed by viewModel.resetArmed.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(counter?.name ?: ""),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.TRASH,
                        onClick = { viewModel.delete { goBack() } },
                    ),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .lightClickable { viewModel.perform(CounterAction.Increment) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LightText(
                            text = counter?.value?.toString() ?: "",
                            variant = LightTextVariant.Title,
                            align = TextAlign.Center,
                        )
                        counter?.target?.let { target ->
                            LightText(
                                text = if (counter!!.value >= target) "target $target reached" else "of $target",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                align = TextAlign.Center,
                                modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                            )
                        }
                        LightText(
                            text = "tap or press volume up",
                            variant = LightTextVariant.Fine,
                            lighten = true,
                            modifier = Modifier.padding(top = 2f.gridUnitsAsDp()),
                        )
                    }
                }

                LightBottomBar(
                    listOf(
                        LightBarButton.Text(text = if (resetArmed) "SURE?" else "RESET", onClick = viewModel::reset),
                        LightBarButton.Text(text = "−1", onClick = { viewModel.perform(CounterAction.Decrement) }),
                        LightBarButton.Text(text = "TARGET", onClick = ::editTarget),
                    ),
                )
            }
        }
    }

    private fun editTarget() {
        val current = viewModel.counter.value?.target?.toString() ?: ""
        navigateTo(
            screenFactory = { TargetScreen(it, initialValue = current) },
            resultCallback = { text -> viewModel.perform(CounterAction.SetTarget(CounterLogic.parseTarget(text))) },
        )
    }
}
