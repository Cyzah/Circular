package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.cyzah.circular.ui.components.CircularIcons
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun RestartSystemUiAction(state: SettingsState) {
    var confirmingRestart by rememberSaveable { mutableStateOf(false) }
    SettingsActionRow(
        title = if (state.restartBusy) "正在重启系统界面…" else "重启系统界面",
        summary = if (state.restartBusy) "正在等待 Root 授权或执行重载，请勿重复操作。"
            else "需要 Root 授权。重载状态栏与控制中心，不会重启手机。",
        busy = state.restartBusy,
        onClick = { confirmingRestart = true },
    )
    RestartDialog(
        show = confirmingRestart,
        onDismiss = { confirmingRestart = false },
        onConfirm = {
            confirmingRestart = false
            state.restartSystemUi()
        },
    )
}

@Composable
internal fun SettingsActionRow(title: String, summary: String, busy: Boolean = false, onClick: () -> Unit) {
    BasicComponent(
        title = title,
        summary = summary,
        insideMargin = SettingsRowInsets,
        enabled = !busy,
        role = Role.Button,
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Button
            if (busy) {
                disabled()
                liveRegion = LiveRegionMode.Polite
            }
        },
        endActions = {
            if (busy) InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary, size = 20.dp)
            else Icon(CircularIcons.ChevronBack, contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(180f),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        },
    )
}

@Composable
private fun RestartDialog(show: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    WindowDialog(show = show, title = "重启系统界面", onDismissRequest = onDismiss,
        insideMargin = DpSize(SettingsContentPadding, SettingsContentPadding)) {
        Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("请在 KernelSU 或其他 Root 管理器中授权 Circular。状态栏和控制中心会短暂消失并重新加载，可能返回锁屏；不会重启整台手机。",
                style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                TextButton("取消", onClick = onDismiss, modifier = Modifier.weight(1f))
                TextButton("授权并重启", onClick = onConfirm, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}
