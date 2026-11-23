package com.cyzah.circular.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Only the icons this app uses; no bundled icon catalogue. */
internal object CircularIcons {
    val ChevronBack = icon("ChevronBack") {
        moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
    }
    val ChevronDown = icon("ChevronDown") {
        moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
    }
    val Preview = icon("Preview") {
        moveTo(2f, 12f)
        curveTo(7f, 4f, 17f, 4f, 22f, 12f)
        curveTo(17f, 20f, 7f, 20f, 2f, 12f)
        close()
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, true, true, 9f, 12f)
        arcTo(3f, 3f, 0f, true, true, 15f, 12f)
        close()
    }
    val Appearance = icon("Appearance") {
        moveTo(4f, 7f); lineTo(10f, 7f)
        moveTo(16f, 7f); lineTo(20f, 7f)
        moveTo(4f, 17f); lineTo(8f, 17f)
        moveTo(14f, 17f); lineTo(20f, 17f)
        moveTo(13f, 4f); lineTo(13f, 10f)
        moveTo(11f, 14f); lineTo(11f, 20f)
    }
    val Display = icon("Display") {
        moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 17f)
        lineTo(3f, 17f); close()
        moveTo(12f, 17f); lineTo(12f, 21f)
        moveTo(8f, 21f); lineTo(16f, 21f)
    }
    val Tools = icon("Tools") {
        moveTo(14f, 3f)
        curveTo(18f, 1f, 23f, 6f, 21f, 10f)
        lineTo(17f, 8f); lineTo(14f, 11f); lineTo(16f, 15f)
        curveTo(14f, 16f, 12f, 15f, 11f, 14f)
        lineTo(5f, 21f); lineTo(2f, 18f); lineTo(9f, 12f)
        curveTo(7f, 8f, 9f, 4f, 12f, 3f)
        lineTo(11f, 7f); lineTo(14f, 9f); lineTo(17f, 6f)
        close()
    }
    val About = icon("About") {
        moveTo(21f, 12f)
        arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        close()
        moveTo(12f, 8f)
        lineTo(12f, 8.2f)
        moveTo(12f, 11.5f)
        lineTo(12f, 16f)
    }
    val Sun = icon("Sun") {
        moveTo(16f, 12f)
        arcTo(4f, 4f, 0f, true, true, 8f, 12f)
        arcTo(4f, 4f, 0f, true, true, 16f, 12f)
        close()
        moveTo(12f, 2f); lineTo(12f, 4f)
        moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(4f, 12f)
        moveTo(20f, 12f); lineTo(22f, 12f)
        moveTo(4.9f, 4.9f); lineTo(6.3f, 6.3f)
        moveTo(17.7f, 17.7f); lineTo(19.1f, 19.1f)
        moveTo(4.9f, 19.1f); lineTo(6.3f, 17.7f)
        moveTo(17.7f, 6.3f); lineTo(19.1f, 4.9f)
    }
    val Moon = icon("Moon") {
        moveTo(21f, 13f)
        curveTo(19.8f, 13.6f, 18.4f, 13.8f, 17f, 13.6f)
        curveTo(12.5f, 13f, 9.5f, 8.9f, 10.1f, 4.5f)
        curveTo(10.2f, 4f, 10.3f, 3.5f, 10.5f, 3f)
        curveTo(6.2f, 3.8f, 3f, 7.6f, 3f, 12.2f)
        curveTo(3f, 17.3f, 7.1f, 21.4f, 12.2f, 21.4f)
        curveTo(16.9f, 21.4f, 20.6f, 17.8f, 21f, 13f)
        close()
    }
}

private fun icon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        )
    }.build()
