package com.cyzah.circular.ui.preview

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cyzah.circular.core.graphics.StatusFont
import com.cyzah.circular.data.settings.StatusFontFiles
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.StatusFontSelection
import com.cyzah.circular.ui.components.LocalCircularContentBackdrop
import com.cyzah.circular.ui.state.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassShape

/** Uses the production renderer and its existing motion timeline. */
@Composable
fun PreviewDock(state: SettingsState, modifier: Modifier = Modifier) {
    val target = if (state.page == 0) state.expansion else 0f
    val expansion by animateFloatAsState(targetValue = target,
        animationSpec = if (state.expansionDragging && state.page == 0) snap()
            else spring(dampingRatio = 1f, stiffness = 220f, visibilityThreshold = 0.001f),
        label = "preview expansion")
    var view by remember { mutableStateOf<SettingsPreviewView?>(null) }
    val sceneRevision = state.sceneRevision
    val revision = state.revision
    val app = LocalContext.current.applicationContext
    val fontSelection = remember(state, revision) {
        StatusFontSelection.decode(state.prefs.getString(SettingsSchema.STATUS_FONT_SOURCE, null))
    }
    var statusFont by remember(state) { mutableStateOf(StatusFont.DEFAULT) }
    LaunchedEffect(app, fontSelection) {
        statusFont = withContext(Dispatchers.IO) {
            StatusFont.resolve(fontSelection) { StatusFontFiles.load(app, it) }
        }
    }
    // Capture values in update so AndroidView refreshes when its Java state changes.
    val previewDark = remember(sceneRevision) { state.runtime.dark() }
    val sceneDescription = remember(sceneRevision) { state.scene.description() }
    val shape = remember { GlassShape(24.dp) }
    val backdrop = LocalCircularContentBackdrop.current
    val previewBackground = if (previewDark) Color(0xff141621) else Color(0xffeeeffc)
    // Match the floating navigation bar's sampling, material, shadow and shading recipe.
    Box(modifier.fillMaxWidth().height(116.dp).glassPanel(
        backdrop = backdrop,
        shape = shape,
        style = GlassDefaults.style(previewDark),
        material = GlassMaterials.puredThinGlass(previewDark),
        stroke = GlassEdgeStrokes.forTheme(previewDark),
        shadow = GlassShadows.Float,
        shading = false,
        fallback = Modifier.clip(shape).background(previewBackground),
    )) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SettingsPreviewView(context, state.scene, state.prefs, state.runtime,
                    { state.style() }, { state.previewDarkChanged() }).also { view = it }
            }, update = {
                it.setStatusFont(statusFont)
                it.setSceneRevision(sceneRevision)
                it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                it.contentDescription = "实时预览，点击切换深浅背景。模拟数据：$sceneDescription"
                it.invalidate()
            }, onRelease = { view = null; state.runtime.resetBatteryTransfer() })
    }
    LaunchedEffect(view, expansion, revision, sceneRevision, state.previewActive) {
        val preview = view ?: return@LaunchedEffect
        if (!state.previewActive) return@LaunchedEffect
        state.runtime.setExpansion(expansion)
        do {
            val configured = PreviewMorph.stackedConfigured(state.bool("stacked_shade", true),
                state.bool("keep_merged_shade", false), state.int("mode", 0), state.scene)
            state.runtime.updateMotion(configured, state.style(), state.scene, SystemClock.uptimeMillis())
            preview.invalidate()
            if (!state.runtime.needsFrame()) break
            withFrameNanos { }
        } while (true)
    }
    DisposableEffect(state) { onDispose { state.runtime.resetBatteryTransfer() } }
}
