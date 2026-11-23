package com.cyzah.circular.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cyzah.circular.ui.components.CircularIcons
import com.cyzah.circular.ui.components.LocalCircularPageBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ExpandableSection(
    title: String,
    expanded: Boolean,
    darkTheme: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(260), label = "section chevron")
    val surface = MiuixTheme.colorScheme.surface
    // Record only the heading beneath the button; sampling the page would include itself.
    val headerBackdrop = if (LocalCircularPageBackdrop.current != null) {
        rememberLayerBackdrop { drawRect(surface); drawContent() }
    } else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .then(if (headerBackdrop != null) Modifier.layerBackdrop(headerBackdrop) else Modifier)
                    .padding(start = 8.dp, end = 64.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                SectionTitle(title)
            }
            GlassIconButton(
                onClick = onToggle,
                modifier = Modifier.align(Alignment.CenterEnd).padding(4.dp).semantics {
                    role = Role.Button
                    contentDescription = if (expanded) "收起$title" else "展开$title"
                    stateDescription = if (expanded) "已展开" else "已收起"
                },
                inheritTopBarMaterial = false,
                backdrop = headerBackdrop,
                style = GlassDefaults.style(darkTheme),
                size = 40.dp,
            ) {
                Icon(CircularIcons.ChevronDown, contentDescription = null,
                    modifier = Modifier.size(20.dp).rotate(rotation), tint = MiuixTheme.colorScheme.onSurface)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(320, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
        ) {
            Card(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}
