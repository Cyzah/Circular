package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.RectF
import android.graphics.Typeface
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.max
import kotlin.math.min

/**
 * 控制中心双排信号，以及「双排 ↔ 三合一」的逐元素形变。
 * 
 * 
 * 条带几何：每排 4 格，整体等比放大至双排总高 13dp，
 * 第一排逐格升高，第二排等高小方块，未点亮格 25% 透明度。
 * 制式文本使用 7dp，条带与文字的间距保持 3dp。
 * 
 * 
 * 两排间距随条带放大至 1.625dp（[.DEFAULT_GAP]），整个块自动垂直居中；
 * 每个元素都能按形变进度在「双排的条」与「三合一的点」之间插值（位置、大小、亮度），
 * 见 [.drawElements]。
 */
class StackedSignals {
    /**
     * 形变目标：三合一那一侧每个元素的画布坐标 / 半径 / 亮度，以及中心制式文本的位置字号。
     * 元素下标 0..3 = 第 1 排（卡一，对应三合一内侧三点），4..7 = 第 2 排（卡二，对应外侧四点）。
     */
    class Target {
        @JvmField val x: FloatArray = FloatArray(ELEMENTS)
        @JvmField val y: FloatArray = FloatArray(ELEMENTS)
        @JvmField val radius: FloatArray = FloatArray(ELEMENTS)
        @JvmField val alpha: FloatArray = FloatArray(ELEMENTS)
        @JvmField val rowAnchorX: FloatArray = FloatArray(ROWS)
        @JvmField val rowAnchorY: FloatArray = FloatArray(ROWS)
        @JvmField var centreX: Float = 0f
        @JvmField var centreY: Float = 0f
        @JvmField var centreSize: Float = 0f

        /** 形变到中心的那一排（0/1）；-1 表示两排文本都只做淡出  */
        @JvmField var centreRow: Int = -1

        /** 形变过程中要并到同组前一个元素上并淡出的元素（下标 0..7）  */
        @JvmField val mergeFade: BooleanArray = BooleanArray(ELEMENTS)
        @JvmField var hasBatteryObstacle: Boolean = false
        @JvmField var smoothBatteryAvoidance: Boolean = false
        @JvmField var batteryLeft: Float = 0f
        @JvmField var batteryTop: Float = 0f
        @JvmField var batteryRight: Float = 0f
        @JvmField var batteryBottom: Float = 0f
        @JvmField var batteryAnchorX: Float = 0f
        @JvmField var openingBattery: OpeningBatteryMorph? = null
        @JvmField var solidBatteryLeft: Float = 0f
        @JvmField var solidBatteryTop: Float = 0f
        @JvmField var solidBatteryRight: Float = 0f
        @JvmField var solidBatteryBottom: Float = 0f
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val metrics = Paint.FontMetrics()
    private var technology = true
    private var textOffset = 0f

    /** 当前排数：控制中心双卡时 2 排，单卡时 1 排  */
    private var rows: Int = ROWS

    init {
        this.textPaint.setTypeface(Typeface.DEFAULT_BOLD)
        this.textPaint.setTextSize(TEXT_SIZE)
        this.textPaint.setTextAlign(Align.LEFT)
        centerText()
    }

    /** 是否在每排右侧显示该卡的网络制式文本  */
    fun setTechnology(z: Boolean) {
        this.technology = z
    }

    /** 排数：2 = 控制中心双卡（默认），1 = 控制中心单卡  */
    fun setRows(i: Int) {
        this.rows = max(1, min(ROWS, i))
    }

    private fun centerText() {
        this.textPaint.getFontMetrics(this.metrics)
        this.textOffset = (-(this.metrics.ascent + this.metrics.descent)) / 2.0f
    }

    /** 第一排基线（dp；块内 0 = 两排整体的垂直中心）  */
    private fun topBaseline(): Float {
        if (this.rows == 1) {
            // 单排：四条把最高的那条竖直居中
            return SINGLE_ROW_TOP / 2.0f
        }
        return (TOP_ROW_TOP - DEFAULT_GAP - BOTTOM_ROW_HEIGHT) / 2.0f
    }

    /**
     * 第 row 排文本行的垂直中心相对块中心的位置（dp）。
     * 
     * 
     * 上排（条）与下排（点）的高度不同，若各自居中在自身条带里，
     * 两个文本合起来会整体偏下；这里把两行文本**作为一组**整体上移，使它们的中心落在块中心上。
     */
    fun rowLabelCenter(row: Int): Float {
        val fTop: Float = topBaseline() - (TOP_ROW_TOP / 2.0f)
        if (this.rows == 1) {
            return 0.0f
        }
        val fBottom: Float = topBaseline() + DEFAULT_GAP + (BOTTOM_ROW_HEIGHT / 2.0f)
        val fPair = (fTop + fBottom) / 2.0f
        return (if (row == 0) fTop else fBottom) - fPair
    }

    /**
     * 画 8 个元素（单卡时 4 个）。
     * 
     * @param rowLeft 块左边缘（画布坐标）
     * @param centerY 块中心线（画布坐标，通常取图标行的垂直中心）
     * @param scale   dp → px
     * @param morph   0 = 元素落在 `target` 上（三合一形态），1 = 双排形态
     * @param xProgress X 轴专用形变进度；右外数字模式会比 `morph` 更慢
     * @param levels  两排点亮格数（0..4）
     * @param rtl     镜像布局（条带翻到右边、文本在左侧）
     * @param target  三合一那一侧的目标（morph &lt; 1 时必须已填好）
     */
    fun drawElements(
        canvas: Canvas,
        rowLeft: Float,
        centerY: Float,
        scale: Float,
        morph: Float,
        xProgress: Float,
        tint: Int,
        levels: IntArray?,
        rtl: Boolean,
        target: Target
    ) {
        if (scale <= 0.0f) {
            return
        }
        val fMix = Motion.clamp(morph)
        val fXProgress = Motion.clamp(xProgress)
        val trim = SignalMotionPolicy.trimProgress(fMix)
        val move = SignalMotionPolicy.moveProgress(fMix)
        val rowTravel = SignalMotionPolicy.rowProgress(fMix)
        val barTravel = SignalMotionPolicy.barProgress(fMix)
        val xTrim = SignalMotionPolicy.trimProgress(fXProgress)
        val xMove = SignalMotionPolicy.moveProgress(fXProgress)
        val xRowTravel = SignalMotionPolicy.rowProgress(fXProgress)
        this.barPaint.setColor(tint)
        val fBaseline = topBaseline()
        for (row in 0..<this.rows) {
            val sourceAnchorX: Float = (rowLeft
                    + ((if (rtl)
                (BAR_SPAN
                        - (((BAR_INSET_DP + BAR_EDGE_DP) / 2.0f))
                        - LEFT_PAD_DP)
            else
                (((BAR_INSET_DP + BAR_EDGE_DP) / 2.0f))
                        + LEFT_PAD_DP) * scale))
            val sourceAnchorY: Float = (centerY
                    + ((if (row == 0)
                fBaseline - ((if (this.rows == 1) SINGLE_ROW_TOP else TOP_ROW_TOP) / 2.0f)
            else
                (fBaseline + DEFAULT_GAP
                        + (BOTTOM_ROW_HEIGHT / 2.0f))) * scale))
            val iLevel = if (levels == null || row >= levels.size) 0 else max(0, min(BARS, levels[row]))
            for (i in 0..<BARS) {
                val iElement: Int = (row * BARS) + i
                val fBarHeight: Float = if (row == 0) {
                    if (this.rows == 1) singleRowBarHeight(i) else TOP_ROW_BASE + (i * TOP_ROW_RISE)
                } else {
                    BOTTOM_ROW_HEIGHT
                }
                var fBarX: Float = BAR_FIRST_X + (i * BAR_STEP)
                if (rtl) {
                    fBarX = BAR_SPAN - fBarX
                }
                // 左侧（RTL 为右侧）再让出 LEFT_PAD_DP，邻居就不会贴上来
                val fSrcX: Float = rowLeft + ((fBarX + (if (rtl) -LEFT_PAD_DP else LEFT_PAD_DP)) * scale)
                val fSrcY =
                    centerY + ((if (row == 0) fBaseline - (fBarHeight / 2.0f) else fBaseline + DEFAULT_GAP + (BOTTOM_ROW_HEIGHT / 2.0f)) * scale)
                val fAlpha = if (i < iLevel) 1.0f else DIMMED_ROW
                val fTargetRadius = target.radius[iElement]
                val localTrim = SignalMotionPolicy.dotTrim(i, trim)
                val localXTrim = SignalMotionPolicy.dotTrim(i, xTrim)
                var fX = SignalMotionPolicy.coordinate(
                    fSrcX,
                    sourceAnchorX,
                    target.rowAnchorX[row],
                    target.x[iElement],
                    localXTrim,
                    xMove,
                    xRowTravel
                )
                val fY = SignalMotionPolicy.coordinate(
                    fSrcY,
                    sourceAnchorY,
                    target.rowAnchorY[row],
                    target.y[iElement],
                    localTrim,
                    move,
                    barTravel
                )
                val localScale = SignalMotionPolicy.dotScale(
                    i,
                    localTrim,
                    rowTravel
                )
                val targetDiameter = fTargetRadius * 2.0f
                val sourceWidth: Float = BAR_WIDTH * scale
                val sourceHeight = fBarHeight * scale
                val maximumDimension = 16.0f * scale
                val fWidth = min(
                    maximumDimension,
                    max(
                        0.0f,
                        (sourceWidth
                                + ((targetDiameter - sourceWidth)
                                * trim)) * localScale
                    )
                )
                val fHeight = min(
                    maximumDimension,
                    max(
                        0.0f,
                        (sourceHeight
                                + ((targetDiameter - sourceHeight)
                                * trim)) * localScale
                    )
                )
                val fAlphaMix = SignalMotionPolicy.elementAlpha(
                    fAlpha,
                    target.alpha[iElement],
                    barTravel,
                    target.mergeFade[iElement]
                )
                this.barPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, fAlphaMix))
                if (target.hasBatteryObstacle) {
                    val preferLeft = target.x[iElement] <= target.batteryAnchorX
                    val openingBattery = target.openingBattery
                    if (openingBattery != null) {
                        fX = SignalMotionPolicy.clearOpeningBatteryX(
                            fX, fY, fWidth / 2, fHeight / 2,
                            openingBattery, target.solidBatteryLeft, target.solidBatteryTop,
                            target.solidBatteryRight, target.solidBatteryBottom, preferLeft
                        )
                    } else {
                        fX = SignalMotionPolicy.clearBatteryX(
                            fX, fY, fWidth / 2, fHeight / 2,
                            target.batteryLeft, target.batteryTop, target.batteryRight, target.batteryBottom,
                            preferLeft, target.smoothBatteryAvoidance
                        )
                    }
                }
                this.rect.set(fX - (fWidth / 2.0f), fY - (fHeight / 2.0f), fX + (fWidth / 2.0f), fY + (fHeight / 2.0f))
                val fRadius = min(fWidth, fHeight) / 2.0f
                canvas.drawRoundRect(this.rect, fRadius, fRadius, this.barPaint)
            }
        }
    }

    companion object {
        const val ROWS: Int = 2
        const val BARS: Int = 4

        /** 元素总数：第 1 排 4 个（卡一）+ 第 2 排 4 个（卡二）  */
        @JvmField val ELEMENTS: Int = ROWS * BARS

        private val EXPANDED_SCALE = 13.0f / 12.0f
        private val BAR_WIDTH: Float = 2.6f * EXPANDED_SCALE
        private val BAR_FIRST_X: Float = 3.3f * EXPANDED_SCALE
        private val BAR_STEP: Float = 4.0f * EXPANDED_SCALE
        private val TOP_ROW_BASE: Float = 3.0f * EXPANDED_SCALE
        private val TOP_ROW_RISE: Float = 1.5f * EXPANDED_SCALE
        private val TOP_ROW_TOP: Float = TOP_ROW_BASE + (TOP_ROW_RISE * (BARS - 1))
        private const val SINGLE_ROW_BASE = 4.6f
        private const val SINGLE_ROW_RISE = 2.3f
        private const val SINGLE_ROW_TOP = 11.5f
        private const val SINGLE_LABEL_GAP = 1.5f
        private val BOTTOM_ROW_HEIGHT: Float = 3.0f * EXPANDED_SCALE
        private const val TEXT_SIZE = 7.0f

        /** 放大后的条带横向跨度（镜像布局用）。  */
        private val BAR_SPAN: Float = 18.6f * EXPANDED_SCALE

        /** 整个条带（不带文本）的宽度：20.15dp。  */
        @JvmField val BLOCK_SPAN_DP: Float = BAR_SPAN

        /** 条带最左格左缘相对块左边的距离（镜像后左右两端相同）。  */
        @JvmField val BAR_INSET_DP: Float = BAR_FIRST_X - (BAR_WIDTH / 2.0f)

        /** 条带最右格右缘相对块左边的距离。  */
        @JvmField val BAR_EDGE_DP: Float = BAR_SPAN - BAR_INSET_DP
        private val TEXT_X: Float = BAR_EDGE_DP + 3.0f

        /** 条带与制式文本之间保持 3dp 的间距。  */
        @JvmField val TEXT_GAP_DP: Float = TEXT_X - BAR_EDGE_DP
        private const val DIMMED_ROW = 0.25f
        private const val TEXT_LIMIT = 6

        /** 两排之间的间距（dp）：随条带等比放大，固定不可调。  */
        @JvmField val DEFAULT_GAP: Float = 1.5f * EXPANDED_SCALE

        /**
         * 条带相对块左缘（RTL 时为右缘）再让出的间距（dp）：给左边的免打扰 / NFC 图标留口气。
         * 块宽度同时加这么多，所以邻居位置不变、只有条带往内缩，左右两侧的总间距各多 1.5dp。
         */
        const val LEFT_PAD_DP: Float = 3.5f

        /** 文本长度上限：过长的制式串直接截断，避免撑出图标区  */
        @JvmStatic fun clip(str: String?): String? {
            if (str == null) {
                return null
            }
            val strTrim = str.trim { it <= ' ' }
            if (strTrim.isEmpty()) {
                return null
            }
            return if (strTrim.length > TEXT_LIMIT) strTrim.substring(0, TEXT_LIMIT) else strTrim
        }

        /** 文本字号（dp）  */
        @JvmStatic fun labelSize(): Float {
            return TEXT_SIZE
        }

        @JvmStatic fun labelSize(percent: Int): Float {
            return TEXT_SIZE * DisplayPolicy.centrePercent(percent) / 100.0f
        }

        @JvmStatic fun singleRowBarHeight(index: Int): Float {
            return SINGLE_ROW_BASE + (max(0, min(BARS - 1, index)) * SINGLE_ROW_RISE)
        }

        @JvmStatic fun singleRowHeight(): Float {
            return SINGLE_ROW_TOP
        }

        @JvmStatic fun singleRowLabelCentreOffset(textSizePx: Float, density: Float): Float {
            val shortestBarTop = (SINGLE_ROW_TOP / 2.0f) - SINGLE_ROW_BASE
            return ((shortestBarTop - SINGLE_LABEL_GAP) * density) - (textSizePx / 2.0f)
        }

        @JvmStatic fun singleRowShortestBarCentreX(
            rowLeft: Float,
            density: Float,
            rtl: Boolean
        ): Float {
            val offset = if (rtl)
                BAR_SPAN - BAR_FIRST_X - LEFT_PAD_DP
            else
                BAR_FIRST_X + LEFT_PAD_DP
            return rowLeft + (offset * density)
        }

        @JvmStatic fun singleRowLabelLeft(
            rowLeft: Float,
            density: Float,
            rtl: Boolean,
            textWidth: Float
        ): Float {
            return singleRowShortestBarCentreX(rowLeft, density, rtl) - (textWidth / 2.0f)
        }

        @JvmStatic fun singleRowBarLeft(
            left: Float,
            right: Float,
            density: Float,
            rtl: Boolean,
            textWidth: Float
        ): Float {
            val shortBarOffset = if (rtl)
                BAR_SPAN - BAR_FIRST_X - LEFT_PAD_DP
            else
                BAR_FIRST_X + LEFT_PAD_DP
            val targetCentre = if (rtl)
                right - textWidth / 2.0f
            else
                left + textWidth / 2.0f
            return targetCentre - (shortBarOffset * density)
        }

        @JvmStatic fun singleRowFillWidthPx(textWidth: Float, density: Float): Float {
            if (!java.lang.Float.isFinite(density) || density <= 0.0f) {
                return 0.0f
            }
            if (!java.lang.Float.isFinite(textWidth) || textWidth <= 0.0f) {
                return (BLOCK_SPAN_DP + LEFT_PAD_DP) * density
            }
            val signalReachFromShortestBar = (3.0f * BAR_STEP) + (BAR_WIDTH / 2.0f)
            val textLeftFromLabelCentre = textWidth / 2.0f
            val signalLeftFromLabelCentre = textLeftFromLabelCentre - (BAR_WIDTH * density / 2.0f)
            val signalRightFromLabelCentre = textLeftFromLabelCentre + (signalReachFromShortestBar * density)
            val left = min(0.0f, signalLeftFromLabelCentre)
            val right = max(textWidth, signalRightFromLabelCentre)
            return right - left
        }

        /**
         * 「铺满」布局下整条排真正需要的宽度（px）：条带跨度 + 条带与文本的间距 + 文本宽。
         * 
         * 
         * 调用方用这个宽度去**占位**（而不是用原生两张卡槽位的宽度），块与邻居之间就只剩
         * 原生图标间距，不会出现条带左边、文本右边各空一块的情况。无文本时等于条带自身宽度。
         */
        @JvmStatic @JvmOverloads
        fun fillWidthPx(
            fTextPx: Float,
            fDensity: Float,
            placement: BatteryNumberPlacement? = BatteryNumberPlacement.HIDDEN
        ): Float {
            if (fTextPx <= 0.0f) {
                return (BLOCK_SPAN_DP + LEFT_PAD_DP) * fDensity
            }
            return ((((BAR_EDGE_DP - BAR_INSET_DP)
                    + TEXT_GAP_DP
                    + LEFT_PAD_DP)) * fDensity) + fTextPx
        }

        /** 无制式文本时，展开态信号条相对信号槽中心的可见左边界偏移。  */
        @JvmStatic @JvmOverloads
        fun sourceVisualLeftOffset(
            scale: Float,
            placement: BatteryNumberPlacement? = BatteryNumberPlacement.HIDDEN
        ): Float {
            return ((((-((BLOCK_SPAN_DP + LEFT_PAD_DP) / 2.0f))
                    - BAR_INSET_DP) + BAR_FIRST_X + LEFT_PAD_DP
                    - (BAR_WIDTH / 2.0f)) * scale)
        }

        /** 无制式文本时，展开态信号条相对信号槽中心的可见右边界偏移。  */
        @JvmStatic @JvmOverloads
        fun sourceVisualRightOffset(
            scale: Float,
            placement: BatteryNumberPlacement? = BatteryNumberPlacement.HIDDEN
        ): Float {
            return (((((-((BLOCK_SPAN_DP + LEFT_PAD_DP) / 2.0f))
                    - BAR_INSET_DP) + BAR_FIRST_X + (3.0f * BAR_STEP)
                    + LEFT_PAD_DP + (BAR_WIDTH / 2.0f))) * scale)
        }

        /** 当前排数下，展开态信号条相对垂直中心的可见底边偏移。  */
        @JvmStatic fun sourceVisualBottomOffset(rows: Int, scale: Float): Float {
            return ((if (rows <= 1)
                SINGLE_ROW_TOP / 2.0f
            else
                (TOP_ROW_TOP + DEFAULT_GAP + BOTTOM_ROW_HEIGHT) / 2.0f)
                    * scale)
        }
    }
}
