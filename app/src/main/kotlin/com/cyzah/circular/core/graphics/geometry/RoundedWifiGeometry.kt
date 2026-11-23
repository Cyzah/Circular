package com.cyzah.circular.core.graphics.geometry

import android.graphics.Path

/**
 * 圆润版 Wi-Fi 字形：上面两条圆弧 + 底部圆润透镜，三条都是闭合填充轮廓。
 * 矢量图逐点换算到 120 坐标系（外弧宽 49，居中于 wifiBounds），曲线原样保留。
 * 坐标系 120 x 120，返回顺序 {底部, 中间弧, 外弧}。
 */
object RoundedWifiGeometry {
    @JvmStatic fun create(): Array<Path?> {
        val outer = Path()
        outer.moveTo(58.50f, 38.92f)
        outer.cubicTo(66.15f, 38.64f, 74.85f, 41.07f, 81.30f, 45.15f)
        outer.cubicTo(82.68f, 46.02f, 84.11f, 46.96f, 84.44f, 48.70f)
        outer.cubicTo(84.95f, 51.45f, 82.28f, 53.91f, 79.58f, 53.23f)
        outer.cubicTo(78.76f, 53.02f, 78.09f, 52.42f, 77.38f, 51.98f)
        outer.cubicTo(73.09f, 49.33f, 68.03f, 47.47f, 62.99f, 46.92f)
        outer.cubicTo(56.17f, 46.17f, 49.75f, 47.97f, 43.93f, 51.41f)
        outer.cubicTo(41.92f, 52.60f, 40.33f, 54.25f, 37.80f, 53.13f)
        outer.cubicTo(35.70f, 52.20f, 34.88f, 49.70f, 36.01f, 47.70f)
        outer.cubicTo(36.70f, 46.47f, 38.71f, 45.30f, 39.92f, 44.58f)
        outer.cubicTo(45.35f, 41.38f, 52.17f, 39.15f, 58.50f, 38.92f)
        outer.close()
        val middle = Path()
        middle.moveTo(58.72f, 51.32f)
        middle.cubicTo(62.89f, 51.15f, 67.13f, 52.13f, 70.80f, 54.07f)
        middle.cubicTo(72.23f, 54.82f, 74.32f, 56.05f, 74.95f, 57.60f)
        middle.cubicTo(75.94f, 60.01f, 74.14f, 62.83f, 71.55f, 62.97f)
        middle.cubicTo(69.89f, 63.07f, 69.55f, 62.55f, 68.31f, 61.75f)
        middle.cubicTo(63.04f, 58.34f, 56.45f, 58.29f, 51.14f, 61.66f)
        middle.cubicTo(50.51f, 62.06f, 49.90f, 62.64f, 49.17f, 62.83f)
        middle.cubicTo(45.48f, 63.81f, 42.53f, 59.79f, 44.94f, 56.70f)
        middle.cubicTo(46.05f, 55.29f, 48.23f, 54.13f, 49.86f, 53.42f)
        middle.cubicTo(52.64f, 52.20f, 55.67f, 51.44f, 58.72f, 51.32f)
        middle.close()
        val dot = Path()
        dot.moveTo(59.05f, 63.97f)
        dot.cubicTo(61.34f, 63.86f, 64.49f, 64.55f, 66.23f, 66.14f)
        dot.cubicTo(67.77f, 67.55f, 66.91f, 69.05f, 65.81f, 70.33f)
        dot.cubicTo(64.88f, 71.41f, 63.83f, 72.54f, 62.85f, 73.58f)
        dot.cubicTo(62.06f, 74.42f, 61.31f, 75.36f, 60.07f, 75.45f)
        dot.cubicTo(59.10f, 75.51f, 58.35f, 75.27f, 57.65f, 74.61f)
        dot.cubicTo(56.52f, 73.53f, 55.31f, 72.15f, 54.25f, 70.98f)
        dot.cubicTo(53.85f, 70.54f, 53.33f, 70.01f, 52.99f, 69.54f)
        dot.cubicTo(50.45f, 66.01f, 56.51f, 64.10f, 59.05f, 63.97f)
        dot.close()
        return arrayOf(dot, middle, outer)
    }
}
