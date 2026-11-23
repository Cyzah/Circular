package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.ContentOffsets
import com.cyzah.circular.domain.settings.SettingsRowSerializer
import com.cyzah.circular.data.settings.SharedPreferencesSettingsStore
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ContentPositionControls(state: SettingsState) {
    var topOpen by rememberSaveable { mutableStateOf(true) }
    var centerOpen by rememberSaveable { mutableStateOf(false) }
    var bottomOpen by rememberSaveable { mutableStateOf(false) }
    ExpandableSection("上部开口内容", topOpen, state.darkTheme, { topOpen = !topOpen }) {
        ContentPositionSlot(state, "上部开口内容", "top_content_offset_x_half_px", "top_content_offset_y_half_px")
    }
    ExpandableSection("环内内容", centerOpen, state.darkTheme, { centerOpen = !centerOpen }) {
        ContentPositionSlot(state, "环内内容", "center_content_offset_x_half_px", "center_content_offset_y_half_px")
    }
    ExpandableSection("下部开口内容", bottomOpen, state.darkTheme, { bottomOpen = !bottomOpen }) {
        ContentPositionSlot(state, "下部开口内容", "bottom_content_offset_x_half_px", "bottom_content_offset_y_half_px")
    }
}

@Composable
private fun ContentPositionSlot(state: SettingsState, title: String, xKey: String, yKey: String) {
    val revision = state.revision
    val values = remember(state) { SharedPreferencesSettingsStore(state.prefs) }
    fun read(key: String) = SettingsRowSerializer.contentOffset(
        values, key, key.removeSuffix("_half_px") + "_px")
    val x = remember(revision, xKey) { read(xKey) }
    val y = remember(revision, yKey) { read(yKey) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(SettingsContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("X ${pixelValue(x)} · Y ${pixelValue(y)}", style = MiuixTheme.textStyles.body1,
            modifier = Modifier.weight(1f).semantics { heading() })
        TextButton(
            text = "复位",
            enabled = x != 0 || y != 0,
            modifier = Modifier.semantics { contentDescription = "复位$title" },
            onClick = {
                state.setInt(xKey, 0)
                state.setInt(yKey, 0)
                state.flushSettings()
            },
        )
    }
    ContentPositionAxis(state, title, "X 横向", xKey, x) { read(xKey) }
    ContentPositionAxis(state, title, "Y 纵向", yKey, y) { read(yKey) }
}

@Composable
private fun ContentPositionAxis(state: SettingsState, slot: String, axis: String, key: String, value: Int,
                                readCurrent: () -> Int) {
    val minimum = ContentOffsets.MIN_HALF_STEPS
    val maximum = ContentOffsets.MAX_HALF_STEPS
    IntSlider(
        title = axis,
        value = value,
        minimum = minimum,
        maximum = maximum,
        step = 1,
        suffix = " px",
        formatValue = ::pixelValue,
        onChangeFinished = state::flushSettings,
        onDiscreteChange = { state.setInt(key, it) },
        onChange = { state.setIntContinuously(key, it) },
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = SettingsContentPadding, end = SettingsContentPadding, bottom = SettingsContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(
            text = "− 0.5 px",
            enabled = value > minimum,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$slot $axis 减少 0.5 物理像素" },
            onClick = {
                val current = readCurrent()
                state.setInt(key, (current - 1).coerceAtLeast(minimum))
            },
        )
        TextButton(
            text = "+ 0.5 px",
            enabled = value < maximum,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$slot $axis 增加 0.5 物理像素" },
            onClick = {
                val current = readCurrent()
                state.setInt(key, (current + 1).coerceAtMost(maximum))
            },
        )
    }
}

private fun pixelValue(halfSteps: Int): String =
    if (halfSteps % 2 == 0) "${halfSteps / 2} px" else "${halfSteps / 2f} px"
