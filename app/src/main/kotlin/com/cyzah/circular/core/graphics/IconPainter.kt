package com.cyzah.circular.core.graphics
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.ContentPositionGeometry
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.core.graphics.geometry.RingGapPolicy
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.RingTrackStyle
import com.cyzah.circular.core.graphics.geometry.WifiEndpointGeometry
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.ContentOffsets
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StatusColors
import kotlin.math.max
import kotlin.math.min
class IconPainter {
    private var headerHeight = 0f
    private var headerWidth = 0f
    private var headerWifiScale = 0f
    private var headerY = 0f
    @JvmField var showPercent: Boolean = false
    @JvmField var swapContents: Boolean = false
    /** 双卡时下面四个点分两组显示（左组 = 卡一、右组 = 卡二），组间距比组内间距稍大  */
    private var groupedDots = false
    @JvmField var opacity: Float = 1.0f
    @JvmField var weight: Float = 1.0f
    @JvmField var wifiScale: Float = 1.0f
    @JvmField var technologyPercent: Int = 100
    private var lowBatteryThreshold = DisplayPolicy.DEFAULT_LOW_BATTERY_THRESHOLD
    @JvmField val wifiGlyphs: WifiGlyphRenderer = WifiGlyphRenderer()
    private val capsuleRenderer = CapsuleStyleRenderer()
    private val duoSignals = DuoSignalRenderer()
    private val duoRenderer: DuoStyleRenderer
    @JvmField val textPaint: StatusNumberPaint = StatusNumberPaint()
    @JvmField val networkPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    /** 制式文字的小号后缀（"5GA" 的 A、"5G+" 的 +），与 networkPaint 同色同字重  */
    @JvmField val suffixPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val technologyRenderer = TechnologyRenderer()
    private var compactTechnology = false
    @JvmField var technologyWeightPercent: Int = -1
    private var batteryWeightPercent = -1
    @JvmField var percentScale: Int = 100
    /** 控制中心展开进度（0=收起，1=完全展开），由绘制分支按帧写入  */
    private var motion = 0f
    private var contentOffsets = ContentOffsets.DEFAULT
    private var contentPixelUnit = 1.0f
    private val openingContentBounds = RectF()
    private val topOpeningBounds = RectF()
    private val bottomOpeningBounds = RectF()
    private val centerOpeningBounds = RectF()
    private val contentCutoutBounds = FloatArray(16)
    private val originalContentBounds = FloatArray(16)
    @JvmField val contentOpeningReference: RingGeometry = RingGeometry()
    private val positionedRing = RingPathAdapter()
    private val positionedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun setContentOffsets(offsets: ContentOffsets?) {
        this.contentOffsets = if (offsets == null) ContentOffsets.DEFAULT else offsets
    }
    fun setContentPixelUnit(unit: Float) {
        this.contentPixelUnit = if (java.lang.Float.isFinite(unit) && unit > 0) unit else 1.0f
    }
    fun contentPixelUnit(): Float {
        return contentPixelUnit
    }
    fun contentX(slot: Int): Float {
        return ContentPositionGeometry.halfPixelUnits(
            if (slot == 0)
                contentOffsets.topX
            else
                if (slot == 2) contentOffsets.bottomX else contentOffsets.centerX, contentPixelUnit
        )
    }
    fun contentY(slot: Int): Float {
        return ContentPositionGeometry.halfPixelUnits(
            if (slot == 0)
                contentOffsets.topY
            else
                if (slot == 2) contentOffsets.bottomY else contentOffsets.centerY, contentPixelUnit
        )
    }
    fun hasContentOffsets(): Boolean {
        return contentOffsets.topX != 0 || contentOffsets.topY != 0 || contentOffsets.centerX != 0 || contentOffsets.centerY != 0 || contentOffsets.bottomX != 0 || contentOffsets.bottomY != 0
    }
    @JvmOverloads
    fun applyContentCutouts(
        top: RectF?, center: RectF?, bottom: RectF?, extra: RectF? = null, tx: Float = contentX(0), ty: Float = contentY(0), cx: Float = contentX(1), cy: Float = contentY(1), bx: Float = contentX(2), by: Float = contentY(2), ex: Float = 0f, ey: Float = 0f
    ) {
        if (!hasContentOffsets()) {
            batteryRingGeometry.setContentCutouts(NO_CUTOUTS, 0f)
            return
        }
        putCutout(0, top, tx, ty)
        putCutout(4, center, cx, cy)
        putCutout(8, bottom, bx, by)
        putCutout(12, extra, ex, ey)
        batteryRingGeometry.setContentCutouts(
            contentCutoutBounds, originalContentBounds, batteryRingGeometry.strokeWidth / 2 + 2, contentOpeningReference
        )
    }
    private fun putCutout(at: Int, box: RectF?, dx: Float, dy: Float) {
        var box = box
        if (dx == 0f && dy == 0f) box = null
        contentCutoutBounds[at] = if (box == null) 0f else box.left
        contentCutoutBounds[at + 1] = if (box == null) 0f else box.top
        contentCutoutBounds[at + 2] = if (box == null) 0f else box.right
        contentCutoutBounds[at + 3] = if (box == null) 0f else box.bottom
        originalContentBounds[at] = if (box == null) 0f else box.left - dx
        originalContentBounds[at + 1] = if (box == null) 0f else box.top - dy
        originalContentBounds[at + 2] = if (box == null) 0f else box.right - dx
        originalContentBounds[at + 3] = if (box == null) 0f else box.bottom - dy
    }
    fun drawPositionedRing(canvas: Canvas, state: IconState, batteryTint: Int, statusTint: Int) {
        positionedRing.update(batteryRingGeometry)
        positionedRingPaint.setStyle(Paint.Style.STROKE)
        positionedRingPaint.setStrokeCap(Paint.Cap.ROUND)
        positionedRingPaint.setStrokeJoin(Join.ROUND)
        positionedRingPaint.setStrokeWidth(batteryRingGeometry.strokeWidth)
        val capsule = batteryRingGeometry.kind == RingGeometry.Kind.CAPSULE
        val track = RingTrackStyle.tint(
            capsule, state.battery, statusTint, batteryTint
        )
        positionedRingPaint.setColor(track)
        positionedRingPaint.setAlpha(
            DisplayPolicy.paintAlpha(
                track, RingTrackStyle.alpha(capsule), opacity
            )
        )
        positionedRing.drawTrack(canvas, positionedRingPaint)
        if (state.battery > 0) {
            positionedRingPaint.setColor(batteryTint)
            positionedRingPaint.setAlpha(DisplayPolicy.paintAlpha(batteryTint, 255, opacity))
            positionedRing.drawSuffix(canvas, Motion.clamp(state.battery / 100.0f), positionedRingPaint)
        }
    }
    private fun flatOpeningBounds(state: IconState) {
        topOpeningBounds.setEmpty()
        centerOpeningBounds.setEmpty()
        bottomOpeningBounds.setEmpty()
        batteryReadoutBounds(state, openingContentBounds)
        (if (this.swapContents) centerOpeningBounds else topOpeningBounds).set(openingContentBounds)
        val network = if (this.swapContents) topOpeningBounds else centerOpeningBounds
        if (this.swapContents) {
            prepareNetworkHeader(state)
            if (this.headerWidth > 0) network.set(
                60 - this.headerWidth / 2, this.headerY - this.headerHeight / 2, 60 + this.headerWidth / 2, this.headerY + this.headerHeight / 2
            )
        } else if (state.showWifi()) {
            val wifiY = if (state.dualSim())
                RingGapPolicy.wifiCentreY(currentDualStyle(), this.showPercent, normalWifiY())
            else
                normalWifiY()
            network.set(this.wifiGlyphs.bounds())
            network.set(
                60 + (network.left - 60) * wifiScale, wifiY + (network.top - 57) * wifiScale, 60 + (network.right - 60) * wifiScale, wifiY + (network.bottom - 57) * wifiScale
            )
        } else if (!state.centreTechnology().isEmpty()) {
            technologyBounds(
                state.centreTechnology(), AppearanceScale.technologyTextSize(technologyPercent), compactTechnology, network
            )
            network.offset(
                60 - measureTechnology(
                    state.centreTechnology(), AppearanceScale.technologyTextSize(technologyPercent), compactTechnology
                ) / 2, centreBaseline(state) - contentY(technologyContentSlot(state))
            )
        }
        if (!network.isEmpty()) network.offset(
            contentX(if (this.swapContents) 0 else 1), contentY(if (this.swapContents) 0 else 1)
        )
        val radius = 4.9f * weight
        for (dot in 0..3) {
            val px =
                if (state.dualSim() && !groupedDots) IconGeometry.outerDotX(dot, weight) else IconGeometry.singleDotX(
                    dot, weight
                )
            val py =
                if (state.dualSim() && !groupedDots) IconGeometry.outerDotY(dot, weight) else IconGeometry.singleDotY(
                    dot, weight
                )
            bottomOpeningBounds.union(
                px - radius + contentX(2), py - radius + contentY(2), px + radius + contentX(2), py + radius + contentY(2)
            )
        }
        if (state.dualSim() && !groupedDots) for (dot in 0..2) {
            val px = IconGeometry.innerDotX(dot, motion, weight) + contentX(2)
            val py = IconGeometry.innerDotY(dot, motion, weight) + contentY(2)
            bottomOpeningBounds.union(px - radius, py - radius, px + radius, py + radius)
        }
    }
    private fun prepareFlatRingGeometry(state: IconState, upper: Float, lower: Float) {
        contentOpeningReference.setCircular(
            currentDualStyle(), ring.left, ring.top, ring.right, ring.bottom, upper, lower, 7.5f * weight
        )
        flatOpeningBounds(state)
        val tx = contentX(0)
        val ty = contentY(0)
        val bx = contentX(2)
        val by = contentY(2)
        batteryRingGeometry.setCircularOpenings(
            currentDualStyle(), ring.left, ring.top, ring.right, ring.bottom, adaptedOpeningEdge(upper, true, true, topOpeningBounds, tx, ty), adaptedOpeningEdge(upper, true, false, topOpeningBounds, tx, ty), adaptedOpeningEdge(lower, false, true, bottomOpeningBounds, bx, by), adaptedOpeningEdge(lower, false, false, bottomOpeningBounds, bx, by), 7.5f * weight
        )
        applyContentCutouts(topOpeningBounds, centerOpeningBounds, bottomOpeningBounds)
    }
    fun wifiContentSlot(state: IconState): Int {
        return if (this.capsuleDual || (state.dualSim() && this.duoRenderer.enabled()))
            1
        else
            if (this.swapContents) 0 else 1
    }
    fun technologyContentSlot(state: IconState): Int {
        if (state.dualSim() && this.duoRenderer.enabled()) return 2
        return if (this.capsuleDual) 1 else if (this.swapContents) 0 else 1
    }
    fun technologyScalePercent(): Int {
        return technologyPercent
    }
    fun adaptedOpeningEdge(
        original: Float, top: Boolean, leftSide: Boolean, bounds: RectF, dx: Float, dy: Float
    ): Float {
        if (bounds.isEmpty() || (dx == 0f && dy == 0f)) return original
        val pad = 7.5f * weight / 2 + 2
        val oldEdge = ContentPositionGeometry.openingEdge(
            top, leftSide, ring.centerX(), ring.centerY(), ring.width() / 2, ring.height() / 2, bounds.left - dx, bounds.top - dy, bounds.right - dx, bounds.bottom - dy, pad
        )
        val newEdge = ContentPositionGeometry.openingEdge(
            top, leftSide, ring.centerX(), ring.centerY(), ring.width() / 2, ring.height() / 2, bounds.left, bounds.top, bounds.right, bounds.bottom, pad
        )
        return max(-179f, min(179f, original + newEdge - oldEdge))
    }
    fun adaptedHalfGap(
        original: Float, top: Boolean, left: Float, upper: Float, right: Float, bottom: Float
    ): Float {
        val slot = if (top) 0 else 2
        return adaptedHalfGap(original, top, left, upper, right, bottom, contentX(slot), contentY(slot))
    }
    fun adaptedHalfGap(
        original: Float, top: Boolean, left: Float, upper: Float, right: Float, bottom: Float, dx: Float, dy: Float
    ): Float {
        if (dx == 0f && dy == 0f) return original
        val cx = this.ring.centerX()
        val cy = this.ring.centerY()
        val rx = this.ring.width() / 2
        val ry = this.ring.height() / 2
        val pad = 7.5f * this.weight / 2 + 2
        val oldGap = ContentPositionGeometry.halfGap(
            top, cx, cy, rx, ry, left - dx, upper - dy, right - dx, bottom - dy, pad
        )
        val newGap = ContentPositionGeometry.halfGap(
            top, cx, cy, rx, ry, left, upper, right, bottom, pad
        )
        return max(0f, min(89f, original + newGap - oldGap))
    }
    private fun flatTopHalfGap(state: IconState?, original: Float): Float {
        if (this.swapContents) {
            if (this.headerWidth <= 0) return original
            val dx = contentX(0)
            val dy = contentY(0)
            return adaptedHalfGap(
                original, true, 60 - this.headerWidth / 2 + dx, this.headerY - this.headerHeight / 2 + dy, 60 + this.headerWidth / 2 + dx, this.headerY + this.headerHeight / 2 + dy
            )
        }
        batteryReadoutBounds(state, openingContentBounds)
        return if (openingContentBounds.isEmpty()) original else adaptedHalfGap(
            original, true, openingContentBounds.left, openingContentBounds.top, openingContentBounds.right, openingContentBounds.bottom
        )
    }
    private fun flatBottomHalfGap(state: IconState, original: Float): Float {
        val x = contentX(2)
        val y = contentY(2)
        val radius = this.weight * 4.9f
        var left = 200f
        var upper = 200f
        var right = -200f
        var bottom = -200f
        for (dot in 0..3) {
            val px = if (state.dualSim() && !this.groupedDots)
                IconGeometry.outerDotX(dot, weight)
            else
                IconGeometry.singleDotX(dot, weight)
            val py = if (state.dualSim() && !this.groupedDots)
                IconGeometry.outerDotY(dot, weight)
            else
                IconGeometry.singleDotY(dot, weight)
            left = min(left, px - radius)
            right = max(right, px + radius)
            upper = min(upper, py - radius)
            bottom = max(bottom, py + radius)
        }
        return adaptedHalfGap(original, false, left + x, upper + y, right + x, bottom + y)
    }
    private var chargeColor = StatusColors.CHARGING
    private var lowColor = StatusColors.LOW
    private var saverColor = StatusColors.SAVER
    private var performanceColor = StatusColors.PERFORMANCE
    @JvmField val numberBounds: Rect = Rect()
    private val batteryReadoutInk = Rect()
    @JvmField val networkTextBounds: Rect = Rect()
    private val pathBounds = RectF()
    @JvmField val textMetrics: Paint.FontMetrics = Paint.FontMetrics()
    @JvmField val p: Paint = Paint(1)
    @JvmField val ring: RectF = RectF(10.0f, 8.0f, 110.0f, 108.0f)
    @JvmField val bolt: Path = Path()
    @JvmField val batteryRingGeometry: RingGeometry = RingGeometry()
    /** 双卡「胶囊」样式开关  */
    @JvmField var capsuleDual: Boolean = false
    /** 双卡 DUO：圆环内双排四点，Wi-Fi 连接后两排下滑到制式缺口。  */
    /** 宿主实际可用区域的宽高比（1 = 正方形）：胶囊据此等比缩放，绝不被拉伸  */
    @JvmField var capsuleAspect: Float = CAPSULE_ASPECT
    /** 预览用：胶囊在图标框里居中画（真机状态栏里是右对齐到官方电池的右边缘）  */
    @JvmField var capsuleCentred: Boolean = false
    init {
        this.duoRenderer = DuoStyleRenderer(this, this.duoSignals)
        this.bolt.moveTo(8.0f, 0.0f)
        this.bolt.lineTo(0.0f, 13.0f)
        this.bolt.lineTo(6.0f, 13.0f)
        this.bolt.lineTo(3.0f, 23.0f)
        this.bolt.lineTo(15.0f, 8.0f)
        this.bolt.lineTo(8.0f, 8.0f)
        this.bolt.close()
        this.textPaint.setTextSize(BatteryHeader.textSize(100))
        this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(100))
        this.networkPaint.setTextAlign(Align.CENTER)
        this.networkPaint.setFontFeatureSettings("'tnum' 1")
        this.suffixPaint.setFontFeatureSettings(this.networkPaint.getFontFeatureSettings())
        setWeightPercent(100)
        setNetworkWeights(100, 100)
        setBatteryWeightPercent(100)
    }
    fun setWeightPercent(i: Int) {
        this.weight = AppearanceScale.ringWeight(i)
        this.ring.top = AppearanceScale.ringTop(i)
    }
    fun setNetworkWeights(i: Int, i2: Int) {
        val iWeight = DisplayPolicy.weight(i2)
        if (this.technologyWeightPercent != iWeight) {
            this.technologyWeightPercent = iWeight
            this.networkPaint.setTypeface(AppearanceScale.technologyTypeface(iWeight))
            this.suffixPaint.setTypeface(this.networkPaint.getTypeface())
        }
        this.wifiGlyphs.setWeightPercent(i)
    }
    fun setBatteryWeightPercent(i: Int) {
        val iWeight = BatteryHeader.weight(i)
        if (this.batteryWeightPercent == iWeight) {
            return
        }
        this.batteryWeightPercent = iWeight
        this.textPaint.setTypeface(StatusTypeface.forWeight(StatusFontWeight.battery(iWeight)))
        this.textPaint.setWeightPercent(iWeight)
    }
    fun batteryWeightPercent(): Int {
        return batteryWeightPercent
    }
    fun setSwapContents(z: Boolean) {
        this.swapContents = z
    }
    /**
     * 双卡信号点的第二种样式：外观沿用单卡那四点，只是把四个点分成两组
     * （左边两个 = 卡一、右边两个 = 卡二，每两点用亮度表示该卡信号强度），
     * 四点间距与单卡一致、中间不额外撑开。单卡不受影响。
     */
    fun setGroupedDots(z: Boolean) {
        this.groupedDots = z
    }
    /**
     * 双卡「胶囊」样式：整条胶囊就是电量环，上排四点 = 卡一、下排四点 = 卡二，
     * 左边槽位是 Wi-Fi 字形或网络制式文字，中间是充电闪电，右边是电量数字（强制显示）。
     */
    fun setCapsuleDual(z: Boolean) {
        this.capsuleDual = z
    }
    fun setDuoDual(z: Boolean) {
        this.duoRenderer.setEnabled(z)
    }
    fun setDuoWifiProgress(f: Float) {
        this.duoRenderer.setWifiProgress(f)
    }
    /**
     * 由宿主写入实际可用区域的宽高比（父容器限宽后的真实值）。
     * 胶囊按高度取固定比例、宽度由内容决定，再整体等比缩放进这个框里 ——
     * 所以宿主给多窄都只会整体变小，长宽比例永远不会被拉变形。
     */
    fun setCapsuleAspect(f: Float) {
        this.capsuleAspect = if (f >= 1.0f) f else CAPSULE_ASPECT
    }
    /**
     * 预览用：胶囊在图标框里居中画。
     * 真机状态栏里胶囊是右对齐到官方电池右边缘的，直接拿到预览框里会明显偏右。
     */
    fun setCapsuleCentred(z: Boolean) {
        this.capsuleCentred = z
    }
    /** 写入当前控制中心展开进度，驱动副卡三点的收放动效  */
    fun setMotion(f: Float) {
        this.motion = Motion.clamp(f)
    }
    fun updateBatteryRingGeometry(iconState: IconState) {
        this.compactTechnology = iconState.dualSim() && !this.capsuleDual
        if (this.capsuleDual) {
            this.capsuleRenderer.updateRingGeometry(this, iconState)
            return
        }
        if (iconState.dualSim() && this.duoRenderer.enabled()) {
            this.duoRenderer.updateRingGeometry(this, iconState)
            return
        }
        val digits: String = (if (!this.showPercent || iconState.battery < 0)
            ""
        else
            com.cyzah.circular.core.graphics.IconPainter.Companion.NUMBERS[kotlin.math.min(100, iconState.battery)])!!
        val showChargeBolt = iconState.charging
                && ChargeBoltPolicy.showBolt(currentDualStyle())
        val digitsWidth = prepareBatteryReadout(digits, showChargeBolt)
        val boltWidth = if (showChargeBolt) 15.0f else 0.0f
        val batteryContentWidth = (digitsWidth
                + boltWidth
                + (if (digitsWidth <= 0.0f || boltWidth <= 0.0f) 0.0f else 4.0f))
        if (this.swapContents) {
            prepareNetworkHeader(iconState)
        }
        val topHalfAngle = RingGapPolicy.topHalfAngle(
            !digits.isEmpty(), this.swapContents, batteryContentWidth, this.headerWidth
        )
        val bottomHalfAngle = if (iconState.dualSim())
            RingGapPolicy.lowerHalfAngle(currentDualStyle(), this.weight)
        else
            RingGapPolicy.singleHalfAngle(this.weight)
        prepareFlatRingGeometry(iconState, topHalfAngle, bottomHalfAngle)
    }
    /** Actual battery digits and charge symbol, in the icon's 120-unit frame.  */
    fun batteryReadoutBounds(state: IconState?, out: RectF) {
        batteryReadoutBounds(state, out, true)
    }
    fun batteryDigitsBounds(state: IconState?, out: RectF) {
        batteryReadoutBounds(state, out, false)
    }
    private fun batteryReadoutBounds(state: IconState?, out: RectF, includeCharge: Boolean) {
        out.setEmpty()
        if (state == null) {
            return
        }
        val savedSize = this.textPaint.getTextSize()
        val savedAlign = this.textPaint.getTextAlign()
        try {
            this.textPaint.setTextAlign(Align.LEFT)
            if (this.capsuleDual) {
                this.capsuleRenderer.batteryReadoutBounds(this, state, out)
                return
            }
            if (state.dualSim() && this.duoRenderer.enabled()) {
                this.duoRenderer.batteryReadoutBounds(state, out)
                return
            }
            val digits: String =
                (if (!this.showPercent || state.battery < 0) "" else com.cyzah.circular.core.graphics.IconPainter.Companion.NUMBERS[kotlin.math.min(
                    100, state.battery
                )])!!
            val charge = state.charging && ChargeBoltPolicy.showBolt(currentDualStyle())
            val digitsWidth = prepareBatteryReadout(digits, charge)
            val boltWidth = if (charge) 15.0f else 0.0f
            val totalWidth = (digitsWidth + boltWidth
                    + (if (digitsWidth > 0.0f && charge) 4.0f else 0.0f))
            var left = 60.0f - totalWidth / 2.0f
            if (charge) {
                val top = if (this.swapContents) BatteryHeader.centreY(true) - 11.5f else 2.5f
                val stroke = max(0.0f, this.weight - 1.0f)
                if (includeCharge) out.set(
                    left - stroke, top - stroke, left + 15.0f + stroke, top + 23.0f + stroke
                )
                left += boltWidth + (if (digitsWidth > 0.0f) 4.0f else 0.0f)
            }
            if (!digits.isEmpty() && digitsWidth > 0.0f) {
                this.textPaint.numberBounds(digits, this.batteryReadoutInk)
                left += this.textPaint.leftPadding()
                val baseline = batteryReadoutBaseline(state, this.batteryReadoutInk)
                out.union(
                    left + this.batteryReadoutInk.left, baseline + this.batteryReadoutInk.top, left + this.batteryReadoutInk.right, baseline + this.batteryReadoutInk.bottom
                )
            }
        } finally {
            this.textPaint.setTextSize(savedSize)
            this.textPaint.setTextAlign(savedAlign)
            if (!out.isEmpty() && !(this.capsuleDual || (state.dualSim() && this.duoRenderer.enabled()))) {
                out.offset(contentX(if (this.swapContents) 1 else 0), contentY(if (this.swapContents) 1 else 0))
            }
        }
    }
    private fun prepareBatteryReadout(digits: String, charge: Boolean): Float {
        this.textPaint.setTextSize(BatteryHeader.textSize(this.percentScale))
        var width = this.textPaint.measureNumber(digits)
        val boltWidth = if (charge) 15.0f else 0.0f
        val available = (BatteryHeader.readoutWidth(this.swapContents)
                - boltWidth - (if (charge) 4.0f else 0.0f))
        this.textPaint.numberBounds(digits, this.numberBounds)
        val fit = BatteryHeader.fit(width, this.numberBounds.height().toFloat(), available, 44.0f)
        if (fit < 1.0f) {
            this.textPaint.setTextSize(this.textPaint.getTextSize() * fit)
            width = this.textPaint.measureNumber(digits)
        }
        return width
    }
    private fun batteryReadoutBaseline(state: IconState, bounds: Rect): Float {
        val sink = ((if (state.dualSim() && !this.groupedDots) 0.0f else 2.0f)
                + (if (this.swapContents) 1.0f else 0.0f))
        return (sink + max(BatteryHeader.centreY(this.swapContents), bounds.height() / 2.0f + 2.0f)
                - (bounds.top + bounds.bottom) / 2.0f)
    }
    fun anchorY(i: Int, iconState: IconState): Float {
        return anchorYInternal(i, iconState) + (if (i == 1) contentY(wifiContentSlot(iconState)) else 0f)
    }
    fun technologyText(canvas: Canvas, str: String, f: Float, f2: Float, i: Int) {
        this.technologyRenderer.drawCentred(
            canvas, str, f, f2, i, this.opacity, this.networkPaint, this.suffixPaint, this.technologyWeightPercent, this.compactTechnology
        )
    }
    /** Wi-Fi 在环内的竖直位置：跟着圆弧的下边缘走，保证整块 Wi-Fi 不越出电量环  */
    private fun normalWifiY(): Float {
        return min(0.0f, 84.0f - (((this.wifiGlyphs.bounds().bottom - 57.0f) * this.wifiScale) + 57.0f)) + 57.0f
    }
    private fun anchorYInternal(i: Int, iconState: IconState): Float {
        if (i == 1 && this.capsuleDual && iconState.showWifi()) {
            // 胶囊样式里的 Wi-Fi 在左槽位、竖直居中于胶囊，也就是 120 坐标系的 60；
            // 这个值同时决定「原生 Wi-Fi 视图形变动画」的起点，指错就会在胶囊 Wi-Fi
            // 旁边多出一个原生 Wi-Fi 标
            return 60.0f
        }
        if (i == 1 && this.swapContents) {
            prepareNetworkHeader(iconState)
            return this.headerY
        }
        if (i == 1) {
            return if (iconState.showWifi()) normalWifiY() else 57.0f
        }
        return if (i == 2) 105.5f else 60.0f
    }
    fun setLowBatteryThreshold(i: Int) {
        this.lowBatteryThreshold = DisplayPolicy.lowBatteryThreshold(i)
    }
    fun setShowPercent(z: Boolean) {
        this.showPercent = z
    }
    fun setBatteryText(i: Int) {
        this.percentScale = BatteryHeader.scale(i)
    }
    fun setStatusColors(i: Int, i2: Int, i3: Int, i4: Int) {
        this.chargeColor = StatusColors.opaque(i)
        this.lowColor = StatusColors.opaque(i2)
        this.saverColor = StatusColors.opaque(i3)
        this.performanceColor = StatusColors.opaque(i4)
    }
    fun setCentreSizes(i: Int, i2: Int) {
        this.wifiScale = DisplayPolicy.centrePercent(i) / 100.0f
        val iCentrePercent = DisplayPolicy.centrePercent(i2)
        if (this.technologyPercent != iCentrePercent) {
            this.technologyPercent = iCentrePercent
            this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(iCentrePercent))
        }
    }
    private fun prepareNetworkHeader(iconState: IconState) {
        this.compactTechnology = iconState.dualSim() && !this.capsuleDual
        if (iconState.showWifi()) {
            // 表头 Wi-Fi 直接用「Wi-Fi 大小」比例，不做 fit 收缩
            this.headerWifiScale = this.wifiScale
            this.headerWidth = this.wifiGlyphs.bounds().width() * this.wifiScale
            this.headerHeight = this.wifiGlyphs.bounds().height() * this.wifiScale
        } else {
            val strCentreTechnology = iconState.centreTechnology()
            if (strCentreTechnology.isEmpty()) {
                // 既没有 WiFi 也没有可显示的制式：顶部不预留空间，电量环保持完整不截断
                this.headerWifiScale = 0.0f
                this.headerWidth = 0.0f
                this.headerHeight = 0.0f
            } else {
                this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(this.technologyPercent) * BatteryHeader.networkHeaderScale())
                // 后缀（5GA 的 A / 5G+ 的 +）是小号字，表头按实际绘制尺寸量，否则圆环开口会比文字大一圈
                val strBase = IconGeometry.technologyBase(strCentreTechnology)
                val strSuffix =
                    if (strCentreTechnology.length > strBase.length) strCentreTechnology.substring(strBase.length) else ""
                if (!strSuffix.isEmpty()) {
                    this.suffixPaint.setTextSize(this.networkPaint.getTextSize() * TECHNOLOGY_SUFFIX_SCALE)
                }
                var fTechnologyWidth = technologyWidth(strBase, strSuffix)
                this.networkPaint.getTextBounds(strBase, 0, strBase.length, this.networkTextBounds)
                val fFit = BatteryHeader.fit(fTechnologyWidth, this.networkTextBounds.height().toFloat(), 86.0f, 38.0f)
                if (fFit < 1.0f) {
                    this.networkPaint.setTextSize(this.networkPaint.getTextSize() * fFit)
                    if (!strSuffix.isEmpty()) {
                        this.suffixPaint.setTextSize(this.networkPaint.getTextSize() * TECHNOLOGY_SUFFIX_SCALE)
                    }
                    fTechnologyWidth = technologyWidth(strBase, strSuffix)
                    this.networkPaint.getTextBounds(strBase, 0, strBase.length, this.networkTextBounds)
                }
                this.headerWidth = fTechnologyWidth
                // 只有带小号后缀的（5GA / 5G+ 这种）才把表头抬到 Wi-Fi 高度、让文字下沉；
                // 普通 "5G"/"4G" 保持原来的垂直位置
                this.headerHeight = if (strSuffix.isEmpty()) this.networkTextBounds.height()
                    .toFloat() else max(this.networkTextBounds.height().toFloat(), 28.0f)
            }
        }
        this.headerY = BatteryHeader.networkHeaderY(this.headerHeight)
    }
    fun technologyWidth(str: String?, str2: String?): Float {
        return this.technologyRenderer.measure(
            str + str2, this.networkPaint.getTextSize(), this.networkPaint, this.compactTechnology
        )
    }
    private fun centreNetworkTop(iconState: IconState): Float {
        if (iconState.showWifi()) {
            return normalWifiY() + ((this.wifiGlyphs.bounds().top - 57.0f) * this.wifiScale)
        }
        val strCentreTechnology = iconState.centreTechnology()
        this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(this.technologyPercent))
        this.networkPaint.getFontMetrics(this.textMetrics)
        this.networkPaint.getTextBounds(strCentreTechnology, 0, strCentreTechnology.length, this.networkTextBounds)
        val centreY = if (iconState.dualSim())
            RingGapPolicy.technologyCentreY(currentDualStyle(), this.showPercent)
        else
            57.0f
        return (centreY - ((this.textMetrics.ascent + this.textMetrics.descent) / 2.0f)) + this.networkTextBounds.top
    }
    private fun battery(
        canvas: Canvas, iconState: IconState, i: Int, i2: Int, i3: Int, mask: Int
    ) {
        val f: Float
        val f2: Float
        val f3: Float
        var canvas2: Canvas
        var i4: Int
        var f4: Float
        val str: String =
            (if (!this.showPercent || iconState.battery < 0) "" else com.cyzah.circular.core.graphics.IconPainter.Companion.NUMBERS[kotlin.math.min(
                100, iconState.battery
            )])!!
        val showChargeBolt = iconState.charging && ChargeBoltPolicy.showBolt(currentDualStyle())
        val fMeasureText = prepareBatteryReadout(str, showChargeBolt)
        val f5 = (if (showChargeBolt) 15 else 0).toFloat()
        val f7 = fMeasureText + f5 + (if (fMeasureText <= 0.0f || f5 <= 0.0f) 0 else 4)
        if (this.swapContents) {
            prepareNetworkHeader(iconState)
        }
        val batteryNumberVisible = !str.isEmpty()
        val topHalfAngle = RingGapPolicy.topHalfAngle(
            batteryNumberVisible, this.swapContents, f7, this.headerWidth
        )
        val bottomHalfAngle = if (iconState.dualSim())
            RingGapPolicy.lowerHalfAngle(currentDualStyle(), this.weight)
        else
            RingGapPolicy.singleHalfAngle(this.weight)
        val sideSweep = max(0.0f, 180.0f - topHalfAngle - bottomHalfAngle)
        val totalSweep = sideSweep * 2.0f
        val fArcStart = 90.0f + bottomHalfAngle
        val rightArcStart = 270.0f + topHalfAngle
        val closedAtTop = topHalfAngle <= 0.0f
        prepareFlatRingGeometry(iconState, topHalfAngle, bottomHalfAngle)
        if (!BatteryTransferPolicy.drawsBatteryContent(mask)) {
            return
        }
        canvas2 = canvas
        i4 = 255
        f4 = 2.0f
        if (BatteryTransferPolicy.drawsBatteryRing(mask)) {
            if (hasContentOffsets()) {
                drawPositionedRing(canvas, iconState, i2, i)
            } else {
                stroke(if (iconState.battery == 0) i2 else i, 48, 7.5f)
                val rectF = this.ring
                if (closedAtTop) {
                    f = 7.5f
                    canvas.drawArc(rectF, fArcStart, totalSweep, false, this.p)
                    f3 = sideSweep
                    f2 = rightArcStart
                } else {
                    f = 7.5f
                    canvas.drawArc(rectF, fArcStart, sideSweep, false, this.p)
                    canvas.drawArc(this.ring, rightArcStart, sideSweep, false, this.p)
                    f2 = rightArcStart
                    f3 = sideSweep
                }
                if (iconState.battery <= 0) {
                    canvas2 = canvas
                    i4 = 255
                    f4 = 2.0f
                } else {
                    val fMin = (totalSweep * min(100, iconState.battery)) / 100.0f
                    stroke(i2, 255, f)
                    val rectF2 = this.ring
                    if (closedAtTop) {
                        i4 = 255
                        f4 = 2.0f
                        canvas2 = canvas
                        canvas2.drawArc(rectF2, fArcStart, fMin, false, this.p)
                    } else {
                        i4 = 255
                        f4 = 2.0f
                        val f11 = f3
                        canvas.drawArc(rectF2, fArcStart, min(f3, fMin), false, this.p)
                        if (fMin > f11) {
                            canvas2 = canvas
                            canvas2.drawArc(this.ring, f2, fMin - f11, false, this.p)
                        } else {
                            canvas2 = canvas
                        }
                    }
                }
            }
        }
        val contentSave = canvas2.save()
        try {
            canvas2.translate(contentX(if (this.swapContents) 1 else 0), contentY(if (this.swapContents) 1 else 0))
            // 水平居中：数字与充电闪电一起算（整块居中）
            var f12 = 60.0f - (f7 / f4)
            if (showChargeBolt) {
                val iSave = canvas2.save()
                canvas2.translate(f12, if (this.swapContents) BatteryHeader.centreY(true) - 11.5f else 2.5f)
                symbol(i3, i4, f4)
                canvas2.drawPath(this.bolt, this.p)
                canvas2.restoreToCount(iSave)
                f12 += f5 + (if (fMeasureText > 0.0f) 4 else 0)
            }
            if (fMeasureText > 0.0f) {
                val numberTint = StatusColors.batteryNumberTint(i2)
                this.textPaint.setColor(numberTint)
                this.textPaint.setAlpha(
                    DisplayPolicy.paintAlpha(
                        numberTint, i4, this.opacity
                    )
                )
                this.textPaint.numberBounds(str, this.numberBounds)
                canvas2.drawText(
                    str, f12 + this.textPaint.leftPadding(), batteryReadoutBaseline(iconState, this.numberBounds), this.textPaint
                )
            }
        } finally {
            canvas2.restoreToCount(contentSave)
        }
    }
    private fun currentDualStyle(): DualStyle {
        if (this.capsuleDual) {
            return DualStyle.CAPSULE
        }
        if (this.duoRenderer.enabled()) {
            return DualStyle.DUO
        }
        if (this.groupedDots) {
            return DualStyle.GROUPED
        }
        return DualStyle.LAYERED
    }
    fun stroke(i: Int, i2: Int, f: Float) {
        this.p.setColor(i)
        this.p.setAlpha(DisplayPolicy.paintAlpha(i, i2, this.opacity))
        this.p.setStyle(Paint.Style.STROKE)
        this.p.setStrokeWidth(f * this.weight)
        this.p.setStrokeCap(Paint.Cap.ROUND)
        this.p.setStrokeJoin(Join.ROUND)
    }
    fun fill(i: Int, i2: Int) {
        this.p.setColor(i)
        this.p.setAlpha(DisplayPolicy.paintAlpha(i, i2, this.opacity))
        this.p.setStyle(Paint.Style.FILL)
    }
    fun symbol(i: Int, i2: Int, f: Float) {
        fill(i, i2)
        if (this.weight > 1.0f) {
            this.p.setStyle(Paint.Style.FILL_AND_STROKE)
            this.p.setStrokeWidth(f * (this.weight - 1.0f))
            this.p.setStrokeJoin(Join.ROUND)
        }
    }
    fun draw(
        canvas: Canvas, f: Float, f2: Float, f3: Float, iconState: IconState, i: Int, z: Boolean
    ) {
        drawMask(canvas, f, f2, f3, iconState, i, z, 7, 1.0f)
    }
    fun drawPart(
        canvas: Canvas, f: Float, f2: Float, f3: Float, iconState: IconState, i: Int, z: Boolean, i2: Int, f4: Float
    ) {
        drawMask(canvas, f, f2, f3, iconState, i, z, 1 shl i2, f4)
    }
    /** 与 [.drawPart] 相同，但可以额外带上 [.MASK_HIDE_SIGNALS] / [.MASK_HIDE_CENTRE]  */
    fun drawPartMasked(
        canvas: Canvas, f: Float, f2: Float, f3: Float, iconState: IconState, i: Int, z: Boolean, iMask: Int, f4: Float
    ) {
        drawMask(canvas, f, f2, f3, iconState, i, z, iMask, f4)
    }
    /** Project the glyph size while retaining the content unit used to measure its source bounds.  */
    fun drawPartMaskedAtReferenceUnit(
        canvas: Canvas, x: Float, y: Float, size: Float, state: IconState, tint: Int, coloured: Boolean, mask: Int, alpha: Float, referenceUnit: Float
    ) {
        val previous = contentPixelUnit
        try {
            drawMask(canvas, x, y, size, state, tint, coloured, mask, alpha, referenceUnit)
        } finally {
            contentPixelUnit = previous
        }
    }
    fun centreBaseline(iconState: IconState): Float {
        this.compactTechnology = iconState.dualSim() && !this.capsuleDual
        if (this.swapContents) {
            prepareNetworkHeader(iconState)
            return (this.headerY - ((this.networkTextBounds.top + this.networkTextBounds.bottom) / 2.0f)
                    + contentY(technologyContentSlot(iconState)))
        }
        this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(this.technologyPercent))
        this.networkPaint.getFontMetrics(this.textMetrics)
        val centreY = if (iconState.dualSim())
            RingGapPolicy.technologyCentreY(currentDualStyle(), this.showPercent)
        else
            57.0f
        return (centreY - ((this.textMetrics.ascent + this.textMetrics.descent) / 2.0f)
                + contentY(technologyContentSlot(iconState)))
    }
    fun centreTextSize(): Float {
        return this.networkPaint.getTextSize()
    }
    fun duoTechnologyTextSize(state: IconState): Float {
        this.compactTechnology = state.dualSim() && !this.capsuleDual
        return this.duoRenderer.technologyTextSize(state)
    }
    /** 形变期间由模块绘制制式文本时使用：按「主体 + 0.75 倍后缀」量宽（画布坐标/画布字号）  */
    fun measureTechnology(str: String?, fSizePx: Float): Float {
        return measureTechnology(str, fSizePx, false)
    }
    fun measureTechnology(str: String?, fSizePx: Float, compact: Boolean): Float {
        return this.technologyRenderer.measure(
            str, fSizePx, this.networkPaint, compact
        )
    }
    fun technologyBounds(text: String?, size: Float, out: RectF) {
        technologyBounds(text, size, false, out)
    }
    fun technologyBounds(text: String?, size: Float, compact: Boolean, out: RectF) {
        this.technologyRenderer.bounds(
            text, size, this.networkPaint, this.technologyWeightPercent, compact, out
        )
    }
    /**
     * 形变期间制式文本的基线偏移（画布坐标）：用「真正绘制它的那套字体」量，
     * 使文字竖直方向居中在该行的中心线上。
     */
    fun technologyOffset(str: String?, fSizePx: Float): Float {
        return this.technologyRenderer.baselineOffset(fSizePx, this.networkPaint)
    }
    /** 形变期间由模块绘制制式文本：左对齐到 fLeft，基线与字号由调用方给出（画布坐标）  */
    fun drawTechnology(
        canvas: Canvas?, str: String?, fLeft: Float, fBaseline: Float, fSizePx: Float, fAlpha: Float, i: Int
    ) {
        drawTechnology(canvas, str, fLeft, fBaseline, fSizePx, fAlpha, i, false)
    }
    fun drawTechnology(
        canvas: Canvas?, str: String?, fLeft: Float, fBaseline: Float, fSizePx: Float, fAlpha: Float, i: Int, compact: Boolean
    ) {
        this.technologyRenderer.drawLeft(
            canvas, str, fLeft, fBaseline, fSizePx, fAlpha, i, this.networkPaint, this.technologyWeightPercent, compact
        )
    }
    /**
     * 双卡「胶囊」样式：整条胶囊 = 电量环。
     * 
     * 
     * 电量环从下排点左侧起算、逆时针绕一圈（左下段 → 左半圆 → 顶部两段 → 右半圆 → 右下段），
     * 没走到的那段就是「已消耗」。
     * 
     * 
     * 胶囊高度固定为 [.CAPSULE_H]，宽度由内容决定（最短 [.CAPSULE_MIN_ASPECT] 倍高，
     * 左边槽位没内容时缩到最短）；最后整体等比缩放进宿主可用框。上下两排点用同一组横坐标，
     * 正好面对面。
     */
    /**
     * 胶囊需要宿主预留的宽度（120 坐标系单位）。
     * 
     * 
     * = 胶囊本体 + 半个描边（右对齐是按路径贴边的，描边还会往外顶半个宽度）+ 固定的可见左边距。
     * 宿主拿它反推图标视图宽度，于是：胶囊宽度随内容变、**左边那个图标的间距永远固定**、
     * 胶囊右边缘仍然贴在预留框右边界（= 官方电池最右侧）。
     */
    fun capsuleReserveWidth(iconState: IconState): Float {
        return this.capsuleRenderer.reserveWidth(this, iconState)
    }
    /**
     * 「双卡双层」形变用：胶囊样式下第 `iDot` 颗点（`iRow` 0 = 上排卡一、1 = 下排卡二）
     * 在 120 坐标系里的 x。形变起点必须和胶囊实际画出来的位置完全一致，否则会跳。
     */
    fun capsuleDotX(iconState: IconState, iRow: Int, iDot: Int): Float {
        return this.capsuleRenderer.dotX(
            this, iconState, iRow, iDot
        )
    }
    /** 「双卡双层」形变用：胶囊样式下某一排点的 y（120 坐标系）  */
    fun capsuleDotY(iconState: IconState, iRow: Int): Float {
        return this.capsuleRenderer.dotY(this, iconState, iRow)
    }
    /** 胶囊里信号点的实际半径（已计入胶囊整体缩放）  */
    fun capsuleDotRadius(iconState: IconState): Float {
        return this.capsuleRenderer.dotRadius(this, iconState)
    }
    /** 双层 / 四点共用的环内 WiFi 起点，胶囊和 DUO 的形变也从这里出发。  */
    fun regularWifiY(): Float {
        return normalWifiY()
    }
    fun regularWifiScale(): Float {
        return this.wifiScale
    }
    fun wifiReadoutCentreX(state: IconState): Float {
        return if (this.capsuleDual)
            capsuleTextX(state)
        else
            60.0f + contentX(wifiContentSlot(state))
    }
    fun wifiReadoutCentreY(state: IconState): Float {
        if (this.swapContents) {
            prepareNetworkHeader(state)
        }
        return (WifiEndpointGeometry.centreY(
            currentDualStyle(), state.dualSim(), this.swapContents, this.showPercent, normalWifiY(), this.headerY, this.wifiGlyphs.bounds().centerY(), this.wifiScale
        )
                + contentY(wifiContentSlot(state)))
    }
    fun wifiReadoutScale(state: IconState): Float {
        return if (this.capsuleDual)
            capsuleWifiScale(state)
        else
            this.wifiScale
    }
    fun wifiGlyphLevel(state: IconState): Int {
        return this.duoRenderer.wifiLevelFor(state)
    }
    fun capsuleWifiScale(iconState: IconState): Float {
        return this.capsuleRenderer.drawWifiScale(this, iconState)
    }
    fun wifiHalfWidth(scale: Float): Float {
        return (this.wifiGlyphs.bounds().width() * scale) / 2.0f
    }
    fun wifiHalfHeight(scale: Float): Float {
        val bounds = this.wifiGlyphs.bounds()
        val centre = bounds.centerY()
        return max(centre - bounds.top, bounds.bottom - centre) * scale
    }
    fun wifiBottomOffset(scale: Float): Float {
        return (this.wifiGlyphs.bounds().bottom - this.wifiGlyphs.bounds().centerY()) * scale
    }
    /** 预览形变专用：只画原生 Wi-Fi 视图对应的字形，由调用方给出画布中心与缩放。  */
    fun drawWifiOnly(
        canvas: Canvas?, iconState: IconState, tint: Int, centreX: Float, centreY: Float, scale: Float, alpha: Float
    ) {
        this.wifiGlyphs.drawCentred(
            canvas, tint, this.duoRenderer.wifiLevelFor(iconState), centreX, centreY, scale, this.opacity * Motion.clamp(alpha)
        )
    }
    /** 「双卡双层」形变用：胶囊里那串制式文字的中心 x（120 坐标系，已计入内容横向压缩）  */
    fun capsuleTextX(iconState: IconState): Float {
        return this.capsuleRenderer.textX(this, iconState)
        // 内容是从左往右排的：先左槽位、再闪电、最后数字；这里要的是左槽位那串文字的中心
    }
    /**
     * 「双卡双层」形变用：胶囊里制式文字的字号（120 坐标系单位）。
     * 胶囊的字号是「0.40H × 制式大小」单独算的，跟圆环样式不是同一个值 ——
     * 形变起点必须用它，否则动画第一帧字号会突变。
     */
    fun capsuleTextSize(iconState: IconState): Float {
        return this.capsuleRenderer.textSize(this, iconState)
    }
    /**
     * 「双卡双层」形变用：胶囊里制式文字的**基线** y（120 坐标系单位）。
     * 
     * 
     * 形变那边拿到的 centreY 是基线（`drawTechnology` 直接当 baseline 用），
     * 不是文字的中心 —— 传中心会让整串字往下偏半个字高。
     */
    fun capsuleTextBaseline(iconState: IconState): Float {
        return this.capsuleRenderer.textBaseline(this, iconState)
        // 胶囊里内容竖直居中于 H/2，换算到 120 坐标系中心就是 60
    }
    /**
     * 形变期间只画胶囊左槽位那个 Wi-Fi 字形（part 1 专用）。
     * 
     * 
     * 画布已经被模块按「起点 → 终点」插值平移过，所以这里只负责把字形放到它在胶囊里
     * 原本的那个位置；至于它飞去哪儿，由模块逐帧给的平移量决定（终点就是控制中心那一行的 Wi-Fi）。
     */
    private fun capsuleWifiOnly(canvas: Canvas, iconState: IconState, iPlain: Int) {
        this.capsuleRenderer.capsuleWifiOnly(this, canvas, iconState, iPlain)
    }
    private fun capsule(canvas: Canvas, iconState: IconState, iTint: Int, iPlain: Int, iMask: Int) {
        this.capsuleRenderer.draw(this, canvas, iconState, iTint, iPlain, iMask)
    }
    /**
     * DUO 自成一套圆环排版：上方电量、中央双排信号/ Wi-Fi、下方制式缺口。
     * 连接 Wi-Fi 时制式淡出，信号行与 Wi-Fi 图标交叉过渡。
     */
    private fun duo(canvas: Canvas, iconState: IconState, iBatteryTint: Int, iStatusTint: Int, iMask: Int) {
        this.duoRenderer.draw(
            canvas, iconState, iBatteryTint, iStatusTint, iMask
        )
    }
    fun signalDotRemains(
        cross: Boolean, dotX: Float, dotY: Float, centreX: Float, centreY: Float, dotRadius: Float
    ): Boolean {
        return DuoSignalRenderer.Companion.dotRemains(
            cross, dotX, dotY, centreX, centreY, dotRadius, this.weight
        )
    }
    fun drawSignalCross(
        canvas: Canvas, tint: Int, centreX: Float, centreY: Float
    ) {
        this.duoSignals.drawCross(
            canvas, tint, centreX, centreY, this.weight, this.opacity
        )
    }
    private fun drawMask(
        canvas: Canvas, f: Float, f2: Float, f3: Float, iconState: IconState, i: Int, z: Boolean, i2: Int, f4: Float, referenceUnit: Float = Float.NaN
    ) {
        this.compactTechnology = iconState.dualSim() && !this.capsuleDual
        setContentPixelUnit(if (java.lang.Float.isFinite(referenceUnit) && referenceUnit > 0) referenceUnit else f3 / 120.0f)
        val i3: Int
        val canvas2 = canvas
        this.opacity = Motion.clamp(f4)
        if (i2 == 7) {
            // 整图标绘制（状态栏静止态、AOD、设置页预览）：不参与展开动效
            this.motion = 0.0f
        }
        val iSave = canvas2.save()
        var drawFailure: Throwable? = null
        try {
            canvas.translate(f, f2)
            val f5 = f3 / 120.0f
            canvas2.scale(f5, f5)
            val iBatteryTint = DisplayPolicy.batteryTint(
                iconState.battery, iconState.charging, iconState.powerSave, iconState.performanceMode, z, i, this.lowBatteryThreshold, this.chargeColor, this.lowColor, this.saverColor, this.performanceColor
            )
            if (this.capsuleDual) {
                // 胶囊样式自成一套排版（整条胶囊一次画完）。
                // Wi-Fi 不在这里单独飞：控制中心那一行的 Wi-Fi 是**原生视图**，
                // 模块自己有一套「原生 Wi-Fi 视图按展开进度插值」的动画（起点由 anchorY(1) 决定），
                // 胶囊只需要把 anchorY(1) 指到胶囊里 Wi-Fi 的高度即可，两边一起画反而会多出一个标
                if ((i2 and 1) != 0) {
                    capsule(canvas2, iconState, iBatteryTint, i, i2)
                }
                return
            }
            if (iconState.dualSim() && this.duoRenderer.enabled()) {
                if ((i2 and 1) != 0) {
                    duo(canvas2, iconState, iBatteryTint, i, i2)
                }
                return
            }
            if ((i2 and 1) != 0) {
                battery(
                    canvas2, iconState, i, iBatteryTint, DisplayPolicy.batteryTint(
                        iconState.battery, iconState.charging, false, z, i, this.lowBatteryThreshold, this.chargeColor, this.lowColor, this.saverColor
                    ), i2
                )
                i3 = i
            } else {
                i3 = i
            }
            val i4 = i2 and 2
            var i5 = 0
            val networkSave = canvas2.save()
            try {
                canvas2.translate(contentX(if (this.swapContents) 0 else 1), contentY(if (this.swapContents) 0 else 1))
                if (i4 != 0 && iconState.showWifi()
                    && (i2 and MASK_HIDE_WIFI) == 0 && (i2 and MASK_HIDE_CENTRE) == 0
                ) {
                    val iSave2 = canvas2.save()
                    if (this.swapContents) {
                        prepareNetworkHeader(iconState)
                        // 双卡时 Wi-Fi 图标上移 2 个单位
                        canvas2.translate(
                            60.0f - (this.wifiGlyphs.bounds().centerX() * this.headerWifiScale), (this.headerY - (if (iconState.dualSim()) 2.0f else 0.0f)) - (this.wifiGlyphs.bounds()
                                .centerY() * this.headerWifiScale)
                        )
                        canvas2.scale(this.headerWifiScale, this.headerWifiScale)
                    } else {
                        val wifiY = if (iconState.dualSim())
                            RingGapPolicy.wifiCentreY(currentDualStyle(), this.showPercent, normalWifiY())
                        else
                            normalWifiY()
                        canvas2.translate(0.0f, wifiY - 57.0f)
                        canvas2.scale(this.wifiScale, this.wifiScale, 60.0f, 57.0f)
                    }
                    this.wifiGlyphs.drawAtOrigin(
                        canvas2, i3, iconState.wifiLevel, this.opacity
                    )
                    canvas2.restoreToCount(iSave2)
                } else if (i4 != 0 && (i2 and MASK_HIDE_CENTRE) == 0 && (i2 and MASK_HIDE_TECHNOLOGY) == 0) {
                    val strCentreTechnology = iconState.centreTechnology()
                    // 没有 WiFi、也没有可显示的制式时中间留空：不画占位符，电量环也不留缺口
                    if (!strCentreTechnology.isEmpty()) {
                        if (this.swapContents) {
                            prepareNetworkHeader(iconState)
                            technologyText(
                                canvas2, strCentreTechnology, 60.0f, this.headerY - ((this.networkTextBounds.top + this.networkTextBounds.bottom) / 2.0f), i3
                            )
                        } else {
                            this.networkPaint.setTextSize(AppearanceScale.technologyTextSize(this.technologyPercent))
                            this.networkPaint.getFontMetrics(this.textMetrics)
                            val centreY = if (iconState.dualSim())
                                RingGapPolicy.technologyCentreY(currentDualStyle(), this.showPercent)
                            else
                                57.0f
                            technologyText(
                                canvas2, strCentreTechnology, 60.0f, centreY - ((this.textMetrics.ascent + this.textMetrics.descent) / 2.0f), i3
                            )
                        }
                    }
                }
            } finally {
                canvas2.restoreToCount(networkSave)
            }
            if ((i2 and 4) != 0 && (i2 and MASK_HIDE_SIGNALS) == 0 && !(iconState.dualSim() && this.groupedDots)) {
                // 双卡：外侧四点 = 卡二（卡槽 1），内侧三点 = 卡一（卡槽 0）
                // 双卡 = 两张卡都在（只有一张卡时，无论插在卡槽 0 还是 1，都按单卡绘制）
                val zDual = iconState.dualSim()
                val zCard2Ok = if (zDual)
                    (iconState.card2Present() && iconState.card2Known() && iconState.card2InService() && !iconState.airplane)
                else
                    (iconState.mobileKnown && iconState.simPresent && iconState.inService && !iconState.airplane)
                val singleCross = iconState.showMobileCross()
                val card2Cross = zDual && iconState.card2Cross()
                val dotRadius = this.weight * 4.9f
                val singleCrossY = IconGeometry.singleDotY(1, this.weight) + contentY(2)
                val card2CrossY = IconGeometry.outerDotY(1, this.weight) + contentY(2)
                val iCard2Level = if (zDual) iconState.card2Level() else iconState.mobileLevel
                while (i5 < 4) {
                    val x = (if (zDual) IconGeometry.outerDotX(i5, this.weight) else IconGeometry.singleDotX(
                        i5, this.weight
                    )) + contentX(2)
                    val y = (if (zDual) IconGeometry.outerDotY(i5, this.weight) else IconGeometry.singleDotY(
                        i5, this.weight
                    )) + contentY(2)
                    val cross = if (zDual) card2Cross else singleCross
                    val crossY = if (zDual) card2CrossY else singleCrossY
                    if (signalDotRemains(cross, x, y, 60.0f + contentX(2), crossY, dotRadius)) {
                        fill(i3, if (zCard2Ok && iCard2Level > i5) 255 else 48)
                        canvas2.drawCircle(x, y, dotRadius, this.p)
                    }
                    i5++
                }
                if (zDual) {
                    // 副卡只有三个点：0/1/2/3/4 格依次点亮 0/1/2/3/3 个点
                    // 展开动效：随控制中心展开把三点朝图标圆心（60,58）收拢并略快淡出，收起时再放回
                    val fSpread = 1.0f - (IconGeometry.MOTION_SPREAD * this.motion)
                    val fSecondaryAlpha = 1.0f - (IconGeometry.MOTION_FADE * this.motion)
                    val zCard1Ok =
                        iconState.card1Present() && iconState.card1Known() && iconState.card1InService() && !iconState.airplane
                    val zCard1Cross = iconState.card1Cross()
                    // 5 档信号映射到 3 个点（每点覆盖 4/3 档），用末点透明度体现半档，保证 0/1/2/3/4 各不相同
                    val iCard1Level = max(0, min(4, iconState.card1Level()))
                    val fSpan = 4.0f / 3.0f
                    val fCross1 = IconGeometry.innerDotY(1, this.motion, this.weight) + contentY(2)
                    for (i7 in 0..2) {
                        val x = IconGeometry.innerDotX(i7, this.motion, this.weight) + contentX(2)
                        val y = IconGeometry.innerDotY(i7, this.motion, this.weight) + contentY(2)
                        if (!signalDotRemains(zCard1Cross, x, y, 60.0f + contentX(2), fCross1, dotRadius)) {
                            continue
                        }
                        val fFill = if (zCard1Ok) Motion.clamp((iCard1Level - (i7 * fSpan)) / fSpan) else 0.0f
                        fill(i3, Math.round((48.0f + (fFill * 207.0f)) * fSecondaryAlpha))
                        canvas2.drawCircle(x, y, dotRadius, this.p)
                    }
                }
                if (zDual) {
                    // 哪张卡无服务，就在它自己那条圆弧中间画叉
                    if (iconState.card1Cross()) {
                        drawSignalCross(
                            canvas2, i3, 60.0f + contentX(2), IconGeometry.innerDotY(1, this.motion, this.weight) + contentY(2)
                        )
                    }
                    if (card2Cross) {
                        drawSignalCross(canvas2, i3, 60.0f + contentX(2), card2CrossY)
                    }
                } else if (singleCross) {
                    drawSignalCross(canvas2, i3, 60.0f + contentX(2), singleCrossY)
                }
            }
            if ((i2 and 4) != 0 && (i2 and MASK_HIDE_SIGNALS) == 0 && iconState.dualSim() && this.groupedDots) {
                val signalSave = canvas2.save()
                try {
                    canvas2.translate(contentX(2), contentY(2))
                    groupedDots(canvas2, iconState, i3)
                } finally {
                    canvas2.restoreToCount(signalSave)
                }
            }
        } catch (error: Throwable) {
            drawFailure = error
            throw error
        } finally {
            try {
                canvas2.restoreToCount(iSave)
            } catch (cleanup: Throwable) {
                if (drawFailure == null) {
                    throw cleanup
                }
                if (drawFailure !== cleanup) {
                    drawFailure.addSuppressed(cleanup)
                }
            }
        }
    }
    /**
     * 双卡「四点分组」样式：外观与单卡那四点相同，只是分成两组 ——
     * 左边两个 = 卡一、右边两个 = 卡二，每张卡用自己的两点亮度表示 0~4 格
     * （第 1 点覆盖 0~2 格、第 2 点覆盖 2~4 格，半格用亮度体现，末档不亮时 25%）。
     * 
     * 
     * 中间没有额外间距（[.GROUP_GAP] = 0，四点坐标与单卡完全一致），所以展开时也没有可撑开的量；
     * 副卡那一组仍随展开略微淡出，收起时原路回到静止外观 —— 这条动效只跟控制中心展开进度有关，
     * 用户没开「控制中心双排信号」时一样完整。
     */
    private fun groupedDots(canvas: Canvas, iconState: IconState, i: Int) {
        val fSecondary = IconGeometry.groupedSecondaryAlpha(this.motion)
        val zCard1Ok =
            iconState.card1Present() && iconState.card1Known() && iconState.card1InService() && !iconState.airplane
        val zCard2Ok =
            iconState.card2Present() && iconState.card2Known() && iconState.card2InService() && !iconState.airplane
        val card1Cross = iconState.card1Cross()
        val card2Cross = iconState.card2Cross()
        val iCard1 = max(0, min(4, iconState.card1Level()))
        val iCard2 = max(0, min(4, iconState.card2Level()))
        val crossY = (IconGeometry.groupedDotY(0, this.weight) + IconGeometry.groupedDotY(1, this.weight)) / 2.0f
        val card1CrossX = (IconGeometry.groupedDotX(0, this.motion, this.weight)
                + IconGeometry.groupedDotX(1, this.motion, this.weight)) / 2.0f
        val card2CrossX = (IconGeometry.groupedDotX(2, this.motion, this.weight)
                + IconGeometry.groupedDotX(3, this.motion, this.weight)) / 2.0f
        val dotRadius = this.weight * 4.9f
        for (iDot in XS.indices) {
            val zLeft = iDot < 2
            val iLevel = if (zLeft) iCard1 else iCard2
            val zOk = if (zLeft) zCard1Ok else zCard2Ok
            val x = IconGeometry.groupedDotX(iDot, this.motion, this.weight)
            val y = IconGeometry.groupedDotY(iDot, this.weight)
            val cross = if (zLeft) card1Cross else card2Cross
            val crossX = if (zLeft) card1CrossX else card2CrossX
            if (!signalDotRemains(cross, x, y, crossX, crossY, dotRadius)) {
                continue
            }
            // 组内格数偏移见 {@link #groupedDotRank}（左组方向与右组相反）
            val fFill = if (zOk) Motion.clamp((iLevel - IconGeometry.groupedDotRank(iDot)) / 2.0f) else 0.0f
            var fAlpha = (48.0f + (fFill * 207.0f)) / 255.0f
            if (!zLeft) {
                fAlpha *= fSecondary
            }
            fill(i, Math.round(fAlpha * 255.0f))
            canvas.drawCircle(x, y, dotRadius, this.p)
        }
        // 哪张卡无服务，就在它那一组上打个叉
        if (card1Cross) {
            drawSignalCross(canvas, i, card1CrossX, crossY)
        }
        if (card2Cross) {
            drawSignalCross(canvas, i, card2CrossX, crossY)
        }
    }
    companion object {
        @JvmField val NUMBERS: Array<String> = buildNumbers()

        private fun buildNumbers(): Array<String> {
            val numbers = arrayOfNulls<String>(101)
            var index = 0
            while (index < numbers.size) {
                numbers[index] = index.toString()
                index++
            }
            @Suppress("UNCHECKED_CAST")
            return numbers as Array<String>
        }
        /** 展开动效：副卡三点在最展开时朝圆心收拢的比例  */
        /** 展开动效：副卡三点随展开额外淡出的比例（主卡仍按原透明度）  */
        /** 展开动效：双卡「四点分组」样式下两组额外分开的比例（相对 [.GROUP_GAP]；GAP = 0 时没有可分开的间距）  */
        /** 双卡「四点分组」样式：两组之间的额外间距（120 坐标）。0 = 不额外撑开中间，四点用与单卡同一组坐标  */
        /** 制式文字后缀（"5GA" 的 A、"5G+" 的 +）相对主体的字号比例  */
        const val TECHNOLOGY_SUFFIX_SCALE: Float = 0.75f
        /**
         * drawMask 位（配合 [.drawPartMasked]）：双排↔三合一形变期间，
         * 信号点与中心制式文本由模块按形变进度绘制，这里把它们屏蔽掉，避免重影。
         */
        @JvmField val MASK_HIDE_CENTRE: Int = BatteryTransferPolicy.MASK_HIDE_CENTRE
        @JvmField val MASK_HIDE_SIGNALS: Int = BatteryTransferPolicy.MASK_HIDE_SIGNALS
        @JvmField val MASK_HIDE_TECHNOLOGY: Int = BatteryTransferPolicy.MASK_HIDE_TECHNOLOGY
        @JvmField val MASK_HIDE_BATTERY: Int = BatteryTransferPolicy.MASK_HIDE_BATTERY
        @JvmField val MASK_HIDE_BATTERY_RING: Int = BatteryTransferPolicy.MASK_HIDE_BATTERY_RING
        @JvmField val MASK_HIDE_WIFI: Int = BatteryTransferPolicy.MASK_HIDE_WIFI
        private val XS = floatArrayOf(34.0f, 51.0f, 69.0f, 86.0f)
        private val YS = floatArrayOf(102.0f, 109.0f, 109.0f, 102.0f)
        private val XS_DUAL = floatArrayOf(34.0f, 51.0f, 69.0f, 86.0f)
        private val YS_DUAL = floatArrayOf(103.0f, 110.0f, 110.0f, 103.0f)
        private val XS2 = floatArrayOf(42.5f, 60.0f, 77.5f)
        private val YS2 = floatArrayOf(89.7f, 94.0f, 89.7f)
        private val YS_GROUPED = floatArrayOf(101.0f, 108.0f, 108.0f, 101.0f)
        private const val SIGNAL_EDGE_GAP = 5.7f
        private const val SIGNAL_ROW_WIDTH = 84.0f
        private const val SIGNAL_ROW_GAP = 3.6f
        private const val SIGNAL_PAIR_CENTRE_Y = 98.8f
        private const val SIGNAL_OUTER_MEAN_Y = 106.5f
        private const val SIGNAL_INNER_MEAN_Y = 91.13333f
        private val NO_CUTOUTS = FloatArray(0)
        /* ================= 双卡「胶囊」样式 ================= *
     * 整条胶囊就是电量环，上排四点 = 卡一、下排四点 = 卡二，
     * 左边槽位是 Wi-Fi 字形或制式文字，中间是充电闪电，右边是电量数字。
     *
     * 描边、点距和留白按胶囊高度取固定比例，宽度由内容决定，再等比缩放进宿主可用框。
     */
        /** 胶囊在 120 坐标系里的高度（与电量环 100 的高度同档，24dp 图标框里约 20.8dp；整体随「尺寸」滑块缩放）  */
        const val CAPSULE_H: Float = 104.0f
        /** 信号点半径 4.1%H  */
        @JvmField val CAPSULE_DOT_R: Float = CAPSULE_H * 0.041f
        /** 每排信号点个数（上下排相同）  */
        const val CAPSULE_DOTS: Int = 3
        /** 上排信号点间距：**固定值**，不随胶囊宽度变（胶囊变宽时多出来的长度给电量环的直边）  */
        @JvmField val CAPSULE_DOT_GAP_TOP: Float = CAPSULE_H * 0.060f
        /** 下排信号点间距：比上排大一点  */
        @JvmField val CAPSULE_DOT_GAP_BOT: Float = CAPSULE_H * 0.120f
        /* ---- 胶囊专属展开动效（跟控制中心展开进度 motion 走，与「双卡四点分组」「双卡双层」同一套驱动） ---- */
        /** 两排点朝两端散开的比例：完全展开时点阵比静止态宽 25%  */
        /** 上排点随展开的淡出比例  */
        /** 下排点淡得更快：它要先把位置让给控制中心那两排信号  */
        /** 状态栏里胶囊整体相对「电池右边缘」再往左挪的量（120 坐标系单位）  */
        const val CAPSULE_SHIFT_X: Float = 8.0f
        /** 胶囊轮廓的描边宽度 6.8%H  */
        @JvmField val CAPSULE_STROKE: Float = CAPSULE_H * 0.068f
        /** 胶囊端部留白为高度的 30%，内容间距为高度的 10.5%。 */
        const val CAPSULE_PAD: Float = 0.30f
        const val CAPSULE_GAP: Float = 0.105f
        /** 胶囊本体的最小 / 最大宽高比：无连接缩到最短 1.95H，最宽 2.95H（三位数电量时最宽）  */
        const val CAPSULE_MIN_ASPECT: Float = 1.95f
        const val CAPSULE_MAX_ASPECT: Float = 2.95f
        /**
         * 宿主要求的图标视图宽高比：状态栏按 dp 正方形给空间，而胶囊是宽扁的，
         * 必须按这个比例加宽，否则会被正方形卡死、只能缩成很小一条。
         */
        const val CAPSULE_ASPECT: Float = 2.70f
        /** 未走到的电量段 / 未点亮的信号点透明度  */
        const val CAPSULE_TRACK_ALPHA: Int = 96
        const val CAPSULE_DOT_ALPHA: Int = 48
        /** 状态栏里胶囊右边那个固定间距（120 坐标系单位）：宿主按「胶囊外宽 + 它」预留宽度  */
        const val CAPSULE_SIDE_GAP: Float = 15.0f
        /**
         * Legacy fallback only: the solved host-6/host-5 row owns the final Wi-Fi
         * centre. This nudge remains for hosts without a valid solved snapshot.
         */
        const val CAPSULE_WIFI_END_DX: Float = 0.0f
        const val CAPSULE_WIFI_END_DY: Float = 0.0f
    }
}
