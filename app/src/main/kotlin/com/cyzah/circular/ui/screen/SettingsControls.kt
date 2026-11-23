package com.cyzah.circular.ui.screen

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.StatusColors
import com.cyzah.circular.ui.components.LocalCircularPageBackdrop
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.material.GlassColorLayer
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

internal val SettingsContentPadding = 20.dp
internal val SettingsRowInsets = PaddingValues(SettingsContentPadding)

@Composable
internal fun Section(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title, Modifier.padding(horizontal = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            if (description != null) Note(description)
            content()
        }
    }
}

@Composable
internal fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MiuixTheme.textStyles.title4, modifier = modifier.semantics { heading() })
}

@Composable
internal fun Note(text: String) {
    Text(text, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(SettingsContentPadding))
}

@Composable
internal fun Toggle(title: String, checked: Boolean, summary: String? = null, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    SwitchPreference(title = title, summary = summary, checked = checked, enabled = enabled,
        insideMargin = SettingsRowInsets, onCheckedChange = onChange)
}

@Composable
internal fun IntSlider(title: String, value: Int, minimum: Int, maximum: Int, step: Int = 1,
                       suffix: String = "%", enabled: Boolean = true,
                       onChangeFinished: (() -> Unit)? = null,
                       onDiscreteChange: ((Int) -> Unit)? = null,
                       formatValue: ((Int) -> String)? = null,
                       contentPadding: PaddingValues = SettingsRowInsets, onChange: (Int) -> Unit) {
    // draggable reports cancellation through its stopped callback; disposal covers page/lifecycle removal.
    DisposableEffect(onChangeFinished) { onDispose { onChangeFinished?.invoke() } }
    val displayValue = formatValue?.invoke(value) ?: "$value$suffix"
    Column(Modifier.padding(contentPadding)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MiuixTheme.textStyles.body1, modifier = Modifier.weight(1f))
            Text(displayValue, style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
        }
        Slider(value = value.coerceIn(minimum, maximum).toFloat(), enabled = enabled,
            height = 23.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).semantics {
                contentDescription = title
                stateDescription = displayValue
                if (onChangeFinished != null || onDiscreteChange != null) {
                    setProgress { target ->
                        val normalized = (minimum + ((target - minimum) / step).roundToInt() * step).coerceIn(minimum, maximum)
                        (onDiscreteChange ?: onChange)(normalized)
                        onChangeFinished?.invoke()
                        true
                    }
                }
            },
            valueRange = minimum.toFloat()..maximum.toFloat(), steps = ((maximum - minimum) / step - 1).coerceAtLeast(0),
            onValueChangeFinished = onChangeFinished,
            onValueChange = { onChange((minimum + ((it - minimum) / step).roundToInt() * step).coerceIn(minimum, maximum)) })
    }
}

@Composable
internal fun Action(text: String, enabled: Boolean = true, primary: Boolean = false, onClick: () -> Unit) {
    TextButton(text = text, onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(SettingsContentPadding),
        colors = if (primary) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors())
}

@Composable
internal fun ChoicePreference(title: String, options: List<String>, selected: Int,
                              darkTheme: Boolean, summary: String? = null, enabled: Boolean = true,
                              onSelect: (Int) -> Unit) {
    val defaultMaterial = GlassPopupDefaults.material()
    val menuMaterial = remember(defaultMaterial) {
        defaultMaterial.copy(
            blurRadius = 10.dp,
            first = defaultMaterial.first.withLighterTint(),
            second = defaultMaterial.second?.withLighterTint(),
            third = defaultMaterial.third?.withLighterTint(),
        )
    }
    GlassDropdownPreference(
        title = title,
        summary = summary,
        items = options,
        selectedIndex = selected,
        onSelectedIndexChange = onSelect,
        enabled = enabled,
        insideMargin = SettingsRowInsets,
        backdrop = LocalCircularPageBackdrop.current,
        visuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(darkTheme), material = menuMaterial),
    )
}

private fun GlassColorLayer.withLighterTint(): GlassColorLayer =
    copy(color = color.copy(alpha = color.alpha * 0.70f))

@Composable
internal fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    WindowDialog(show = true, title = title, onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Note(message)
            Action("明白", primary = true, onClick = onDismiss)
        }
    }
}

@Composable
internal fun ColorDialog(title: String, initial: Int, default: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val initialHsv = remember(initial) { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    var hex by rememberSaveable { mutableStateOf(StatusColors.hex(initial)) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    fun color() = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    fun updateHex() { hex = StatusColors.hex(color()); invalid = false }
    fun setColor(value: Int) {
        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(value, it) }
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
        updateHex()
    }
    WindowDialog(show = true, title = title, onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("滑动色板调整深浅，滑动色条选择颜色", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Canvas(Modifier.fillMaxWidth().height(150.dp)
                .pointerInput(hue) {
                    detectTapGestures { point ->
                        saturation = (point.x / size.width).coerceIn(0f, 1f)
                        brightness = (1f - point.y / size.height).coerceIn(0f, 1f)
                        updateHex()
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        brightness = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        updateHex()
                    }
                }) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                drawCircle(Color.White, 7.dp.toPx(), Offset(saturation * size.width, (1f - brightness) * size.height), style = Stroke(2.dp.toPx()))
            }
            Text("色相")
            Slider(value = hue, valueRange = 0f..360f, height = 23.dp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics { contentDescription = "色相" },
                onValueChange = { hue = it; updateHex() })
            Box(Modifier.fillMaxWidth().height(30.dp).background(Color(color())))
            TextField(value = hex, onValueChange = { value ->
                hex = value
                val parsed = StatusColors.parse(value)
                invalid = parsed == null
                if (parsed != null) {
                    val hsv = FloatArray(3).also { AndroidColor.colorToHSV(parsed, it) }
                    hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                }
            }, label = "十六进制色号", singleLine = true)
            if (invalid) Text("请输入 3 位或 6 位色号，例如 #1CB753")
            Action("默认颜色") { setColor(default) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f))
                TextButton(text = "保存", enabled = !invalid, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(), onClick = {
                        val parsed = StatusColors.parse(hex)
                        if (parsed != null) { onSave(parsed); onDismiss() } else invalid = true
                    })
            }
        }
    }
}
