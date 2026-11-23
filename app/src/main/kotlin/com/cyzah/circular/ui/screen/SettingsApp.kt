package com.cyzah.circular.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.AppNotice
import com.cyzah.circular.ui.components.CircularShell
import com.cyzah.circular.ui.preview.PreviewDock
import com.cyzah.circular.ui.state.SettingsState
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun SettingsApp(
    state: SettingsState,
    lastTab: GlassNavigationItem,
    lastPage: @Composable (openCredits: () -> Unit) -> Unit,
) {
    val savedPages = rememberSaveableStateHolder()
    var subpage by rememberSaveable { mutableStateOf<SettingsSubpage?>(null) }
    val destination = subpage?.destination ?: state.page
    val closeSubpage = { state.flushSettings(); subpage = null }
    BackHandler(enabled = subpage != null, onBack = closeSubpage)
    CircularShell(title = subpage?.title ?: "Circular", selectedPage = state.page, lastTab = lastTab,
        onPageSelected = { closeSubpage(); state.page = it },
        darkTheme = state.darkTheme, onToggleTheme = state::toggleTheme,
        themeSettings = state.themeSettings,
        onNavigateBack = if (subpage != null) closeSubpage else null,
        overlay = { padding ->
            AnimatedVisibility(
                visible = destination < 2 || subpage?.showsPreview == true,
                enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(180)),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding())) {
                    PreviewDock(state, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
            }
        }) { padding, onContentScrolled ->
        // Keep the viewport behind the glass bars; apply safe areas to scroll content.
        // The preview remains fixed while settings can scroll beneath both bars.
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { direction * it / 12 }) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -direction * it / 12 })
                },
                label = "settings page",
            ) { page ->
                savedPages.SaveableStateProvider(page) {
                    val pageSubpage = SettingsSubpage.entries.firstOrNull { it.destination == page }
                    val scrollState = rememberScrollState()
                    LaunchedEffect(page, destination, scrollState) {
                        if (page == destination) {
                            snapshotFlow { scrollState.value > 0 }
                                .distinctUntilChanged()
                                .collect(onContentScrolled)
                        }
                    }
                    Column(
                        Modifier.fillMaxSize()
                            // Keep the spring active even when the page has no scroll range.
                            .overScrollVertical()
                            .verticalScroll(scrollState, overscrollEffect = null)
                            .padding(padding).padding(horizontal = 20.dp)
                            .padding(top = if (page < 2 || pageSubpage?.showsPreview == true) 140.dp else 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        when (page) {
                            0 -> PreviewScreen(state)
                            1 -> AppearanceScreen(state) { target ->
                                state.flushSettings()
                                subpage = target
                            }
                            2 -> DisplayScreen(state) {
                                subpage = SettingsSubpage.THEME_SETTINGS
                            }
                            3 -> lastPage {
                                subpage = SettingsSubpage.PROJECT_CREDITS
                            }
                            else -> when (pageSubpage) {
                                SettingsSubpage.THEME_SETTINGS -> ThemeSettingsScreen(state)
                                SettingsSubpage.CONTENT_POSITIONS -> ContentPositionControls(state)
                                SettingsSubpage.ICON_SCALE -> IconScaleScreen(state)
                                SettingsSubpage.BATTERY_LAYOUT -> BatteryLayoutScreen(state)
                                SettingsSubpage.STATUS_COLORS -> StatusColorsScreen(state)
                                SettingsSubpage.PROJECT_CREDITS -> ProjectCreditsScreen { url ->
                                    state.info = "无法打开链接" to url
                                }
                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
        state.info?.let { (title, message) -> MessageDialog(title, message) { state.info = null } }
        if (state.showNotice) {
            var neverAgain by rememberSaveable { mutableStateOf(false) }
            WindowDialog(show = true, title = AppNotice.TITLE, onDismissRequest = { state.dismissNotice(false) }) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Note(AppNotice.MESSAGE)
                    Toggle("以后不再弹出", neverAgain) { neverAgain = it }
                    Action("明白", primary = true) { state.dismissNotice(neverAgain) }
                }
            }
        }
    }
}
