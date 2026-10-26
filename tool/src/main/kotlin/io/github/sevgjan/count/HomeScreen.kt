package io.github.sevgjan.count

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.launch

class HomeViewModel(private val store: CounterStore) : LightViewModel<Unit>() {
    fun add(name: String, onAdded: (Counter) -> Unit) {
        viewModelScope.launch { onAdded(store.add(name)) }
    }
}

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, HomeViewModel>(sealedActivity) {
    private val store = CounterStore(lightContext.dataStore)

    override val viewModelClass: Class<HomeViewModel>
        get() = HomeViewModel::class.java

    override fun createViewModel() = HomeViewModel(store)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val counters by store.counters.collectAsState(initial = emptyList())

        fun addNew() {
            navigateTo(screenFactory = { TextEditorScreen(it, title = "Counter name") }) { name ->
                viewModel.add(name) { counter ->
                    navigateTo(screenFactory = { CounterScreen(it, store, counter.id) })
                }
            }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Count"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                if (counters.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LightText(text = "no counters yet", variant = LightTextVariant.Copy, lighten = true)
                    }
                } else {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        counters.forEach { counter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .lightClickable { navigateTo(screenFactory = { CounterScreen(it, store, counter.id) }) }
                                    .padding(vertical = 0.6f.gridUnitsAsDp()),
                            ) {
                                LightText(
                                    text = counter.name,
                                    variant = LightTextVariant.Copy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                LightText(
                                    text = CounterLogic.progress(counter),
                                    variant = LightTextVariant.Copy,
                                    lighten = true,
                                )
                            }
                        }
                    }
                }

                LightBottomBar(listOf(LightBarButton.Text(text = "ADD NEW", onClick = ::addNew)))
            }
        }
    }
}
