package com.cyzah.circular.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyzah.circular.ui.theme.CircularTheme
import com.cyzah.circular.ui.theme.AppThemeSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Popups may sample the complete page, but never their own rendered overlay. */
val LocalCircularPageBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Floating surfaces share the navigation bar's scene, excluding bars and fixed overlays. */
val LocalCircularContentBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

private val commonTabs = listOf(
    GlassNavigationItem(CircularIcons.Preview, "预览", "预览"),
    GlassNavigationItem(CircularIcons.Appearance, "外观", "外观"),
    GlassNavigationItem(CircularIcons.Display, "显示", "显示"),
)

/** The caller applies [content]'s padding to its scrolling content, allowing it beneath the bars. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircularShell(
    title: String,
    selectedPage: Int,
    lastTab: GlassNavigationItem,
    onPageSelected: (Int) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    themeSettings: AppThemeSettings,
    onNavigateBack: (() -> Unit)? = null,
    overlay: @Composable (PaddingValues) -> Unit = {},
    content: @Composable (PaddingValues, (Boolean) -> Unit) -> Unit,
) {
    val destinations = remember(lastTab) { commonTabs + lastTab }
    CircularTheme(darkTheme, themeSettings) {
        val useGlass = isRuntimeShaderSupported() && isRenderEffectSupported() &&
            LocalView.current.isHardwareAccelerated
        val contentBackdrop = rememberBackdrop(useGlass)
        val pageBackdrop = rememberBackdrop(useGlass)
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        // Navigation remains anchored to system bars, independently of the keyboard.
        val layoutInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .union(WindowInsets.navigationBarsIgnoringVisibility)
        val safeInsets = layoutInsets.asPaddingValues()
        val imeVisible = WindowInsets.isImeVisible
        val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        var topHeight by remember { mutableIntStateOf(0) }
        var bottomHeight by remember { mutableIntStateOf(0) }
        var contentScrolled by remember { mutableStateOf(false) }
        val padding = PaddingValues(
            start = safeInsets.calculateStartPadding(direction),
            top = if (topHeight > 0) with(density) { topHeight.toDp() }
                else safeInsets.calculateTopPadding() + 52.dp,
            end = safeInsets.calculateEndPadding(direction),
            bottom = if (imeVisible) imeBottom
                else if (bottomHeight > 0) with(density) { bottomHeight.toDp() }
                else safeInsets.calculateBottomPadding() + 90.dp,
        )
        // The sole Scaffold owns the fixed popup host outside both recorded layers.
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MiuixTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0),
        ) {
            CompositionLocalProvider(
                LocalCircularPageBackdrop provides pageBackdrop,
                LocalCircularContentBackdrop provides contentBackdrop,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().recordBackdrop(pageBackdrop)) {
                        // Record the fixed viewport; overscroll remains inside this boundary.
                        Box(Modifier.fillMaxSize().recordBackdrop(contentBackdrop)) {
                            content(padding) { contentScrolled = it }
                        }
                        val headerModifier = Modifier.fillMaxWidth().onSizeChanged { topHeight = it.height }
                        if (useGlass) {
                            GlassTopAppBar(
                                title = title,
                                modifier = headerModifier,
                                expandable = false,
                                isContentScrolled = contentScrolled,
                                progressiveBlur = true,
                                backdrop = contentBackdrop,
                                style = GlassDefaults.style(darkTheme),
                                navigationIcon = {
                                    onNavigateBack?.let { back ->
                                        GlassIconButton(onClick = back) {
                                            BackIcon()
                                        }
                                    }
                                },
                                actions = {
                                    GlassIconButton(onClick = onToggleTheme) {
                                        ThemeIcon(darkTheme)
                                    }
                                },
                            )
                        } else {
                            TopAppBar(
                                title = title,
                                modifier = headerModifier,
                                expandable = false,
                                navigationIcon = {
                                    onNavigateBack?.let { back ->
                                        IconButton(onClick = back, minWidth = GlassTopAppBarDefaults.ButtonSize,
                                            minHeight = GlassTopAppBarDefaults.ButtonSize) {
                                            BackIcon()
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(onClick = onToggleTheme, minWidth = GlassTopAppBarDefaults.ButtonSize,
                                        minHeight = GlassTopAppBarDefaults.ButtonSize) {
                                        ThemeIcon(darkTheme)
                                    }
                                },
                            )
                        }
                    }
                    // Keep fixed overlays in the page scene but outside the bar's content
                    // sampler. Draw after the bar so the preview is never covered by its blur.
                    overlay(padding)
                    if (!imeVisible) Box(
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .onSizeChanged { bottomHeight = it.height }
                            .windowInsetsPadding(layoutInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // A null backdrop uses miuix's opaque capsule fallback. Both paths retain
                        // its accessible selection and measured large-font/two-line label layout.
                        GlassNavigationBar(
                            items = destinations,
                            selectedIndex = selectedPage.coerceIn(destinations.indices),
                            onSelect = onPageSelected,
                            backdrop = contentBackdrop,
                            style = GlassDefaults.style(darkTheme),
                            defaultWindowInsetsPadding = false,
                            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackIcon() {
    Icon(NativeGlassIcons.Back, contentDescription = "返回",
        modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
}

@Composable
private fun ThemeIcon(darkTheme: Boolean) {
    val rotation by animateFloatAsState(if (darkTheme) 45f else 0f,
        tween(320, easing = FastOutSlowInEasing), label = "theme icon rotation")
    AnimatedContent(
        targetState = darkTheme,
        modifier = Modifier.size(24.dp).rotate(rotation).semantics {
            contentDescription = if (darkTheme) "切换浅色主题" else "切换深色主题"
        },
        contentAlignment = Alignment.Center,
        transitionSpec = {
            (fadeIn(tween(180)) + scaleIn(tween(260), initialScale = 0.65f)) togetherWith
                (fadeOut(tween(120)) + scaleOut(tween(180), targetScale = 0.65f))
        },
        label = "theme icon",
    ) { dark ->
        Icon(if (dark) CircularIcons.Moon else CircularIcons.Sun, contentDescription = null,
            modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun rememberBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled) return null
    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}

private fun Modifier.recordBackdrop(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop)
