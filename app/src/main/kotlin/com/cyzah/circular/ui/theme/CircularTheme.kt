package com.cyzah.circular.ui.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles

@Composable
fun CircularTheme(darkTheme: Boolean, settings: AppThemeSettings, content: @Composable () -> Unit) {
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            window.isNavigationBarContrastEnforced = false
        }
    }
    // Use the wallpaper's seed, so app palette and specification choices still take effect.
    val seed = if (settings.customColor && settings.dynamicColor) {
        colorResource(android.R.color.system_accent1_500)
    } else Color(settings.keyColor)
    val controller = remember(darkTheme, settings, seed) {
        ThemeController(
            colorSchemeMode = if (settings.customColor) {
                if (darkTheme) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
            } else if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
            keyColor = seed,
            colorSpec = settings.colorSpec,
            paletteStyle = settings.paletteStyle,
            isDark = darkTheme,
        )
    }
    // Match the original settings hierarchy: page 28, section 18, row 15, detail 13.
    val textStyles = remember {
        val defaults = defaultTextStyles()
        defaults.copy(
            main = defaults.main.copy(fontSize = 15.sp, lineHeight = 22.sp),
            paragraph = defaults.paragraph.copy(fontSize = 15.sp, lineHeight = 22.sp),
            body1 = defaults.body1.copy(fontSize = 14.sp, lineHeight = 20.sp),
            body2 = defaults.body2.copy(fontSize = 13.sp, lineHeight = 19.sp),
            button = defaults.button.copy(fontSize = 14.sp, lineHeight = 20.sp),
            footnote1 = defaults.footnote1.copy(fontSize = 12.sp, lineHeight = 17.sp),
            headline1 = defaults.headline1.copy(fontSize = 15.sp, lineHeight = 22.sp),
            headline2 = defaults.headline2.copy(fontSize = 14.sp, lineHeight = 20.sp),
            title1 = defaults.title1.copy(fontSize = 28.sp, lineHeight = 36.sp),
            title3 = defaults.title3.copy(fontSize = 28.sp, lineHeight = 36.sp),
            title4 = defaults.title4.copy(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium),
        )
    }
    MiuixTheme(controller = controller, textStyles = textStyles) {
        val raw = MiuixTheme.colorScheme
        val target = if (settings.amoled && darkTheme) {
            // OS4 uses surface for the page, and surfaceContainer for cards.
            raw.copy(surface = Color.Black)
        } else raw
        // Keep one content subtree alive while all semantic colors transition together.
        MiuixTheme(colors = target.animated(), textStyles = textStyles, content = content)
    }
}

@Composable
private fun animateThemeColor(value: Color): Color =
    animateColorAsState(value, animationSpec = tween(300), label = "appThemeColor").value

@Composable
private fun Colors.animated(): Colors = copy(
    primary = animateThemeColor(primary),
    onPrimary = animateThemeColor(onPrimary),
    primaryVariant = animateThemeColor(primaryVariant),
    onPrimaryVariant = animateThemeColor(onPrimaryVariant),
    error = animateThemeColor(error),
    onError = animateThemeColor(onError),
    errorContainer = animateThemeColor(errorContainer),
    onErrorContainer = animateThemeColor(onErrorContainer),
    disabledPrimary = animateThemeColor(disabledPrimary),
    disabledOnPrimary = animateThemeColor(disabledOnPrimary),
    disabledPrimaryButton = animateThemeColor(disabledPrimaryButton),
    disabledOnPrimaryButton = animateThemeColor(disabledOnPrimaryButton),
    disabledPrimarySlider = animateThemeColor(disabledPrimarySlider),
    primaryContainer = animateThemeColor(primaryContainer),
    onPrimaryContainer = animateThemeColor(onPrimaryContainer),
    secondary = animateThemeColor(secondary),
    onSecondary = animateThemeColor(onSecondary),
    secondaryVariant = animateThemeColor(secondaryVariant),
    onSecondaryVariant = animateThemeColor(onSecondaryVariant),
    disabledSecondary = animateThemeColor(disabledSecondary),
    disabledOnSecondary = animateThemeColor(disabledOnSecondary),
    disabledSecondaryVariant = animateThemeColor(disabledSecondaryVariant),
    disabledOnSecondaryVariant = animateThemeColor(disabledOnSecondaryVariant),
    secondaryContainer = animateThemeColor(secondaryContainer),
    onSecondaryContainer = animateThemeColor(onSecondaryContainer),
    secondaryContainerVariant = animateThemeColor(secondaryContainerVariant),
    onSecondaryContainerVariant = animateThemeColor(onSecondaryContainerVariant),
    tertiaryContainer = animateThemeColor(tertiaryContainer),
    onTertiaryContainer = animateThemeColor(onTertiaryContainer),
    tertiaryContainerVariant = animateThemeColor(tertiaryContainerVariant),
    background = animateThemeColor(background),
    onBackground = animateThemeColor(onBackground),
    onBackgroundVariant = animateThemeColor(onBackgroundVariant),
    surface = animateThemeColor(surface),
    onSurface = animateThemeColor(onSurface),
    surfaceVariant = animateThemeColor(surfaceVariant),
    onSurfaceSecondary = animateThemeColor(onSurfaceSecondary),
    onSurfaceVariantSummary = animateThemeColor(onSurfaceVariantSummary),
    onSurfaceVariantActions = animateThemeColor(onSurfaceVariantActions),
    disabledOnSurface = animateThemeColor(disabledOnSurface),
    surfaceContainer = animateThemeColor(surfaceContainer),
    onSurfaceContainer = animateThemeColor(onSurfaceContainer),
    onSurfaceContainerVariant = animateThemeColor(onSurfaceContainerVariant),
    surfaceContainerHigh = animateThemeColor(surfaceContainerHigh),
    onSurfaceContainerHigh = animateThemeColor(onSurfaceContainerHigh),
    surfaceContainerHighest = animateThemeColor(surfaceContainerHighest),
    onSurfaceContainerHighest = animateThemeColor(onSurfaceContainerHighest),
    outline = animateThemeColor(outline),
    dividerLine = animateThemeColor(dividerLine),
    windowDimming = animateThemeColor(windowDimming),
    sliderKeyPoint = animateThemeColor(sliderKeyPoint),
    sliderKeyPointForeground = animateThemeColor(sliderKeyPointForeground),
    sliderBackground = animateThemeColor(sliderBackground),
)
