package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion

object NativeIconVisibilityPolicy {
    private const val MAX_DEPTH = 32
    @JvmStatic fun <T> alpha(battery: T?, icons: T?, nodes: Nodes<T?>?): Float {
        if (battery == null || icons == null || battery === icons || nodes == null) return 1.0f
        try {
            val common = commonAncestor<T?>(battery, icons, nodes)
            if (common == null) return 1.0f
            return gain(branchAlpha<T?>(battery, common, nodes), branchAlpha<T?>(icons, common, nodes))
        } catch (unavailable: RuntimeException) {
            return 1.0f
        } catch (unavailable: LinkageError) {
            return 1.0f
        }
    }

    private fun <T> commonAncestor(battery: T?, icons: T?, nodes: Nodes<T?>): T? {
        var ancestor = battery
        var depth = 0
        while (ancestor != null && depth < MAX_DEPTH) {
            var candidate = icons
            var otherDepth = 0
            while (candidate != null && otherDepth < MAX_DEPTH) {
                if (ancestor === candidate) return ancestor
                candidate = nodes.parent(candidate)
                otherDepth++
            }
            ancestor = nodes.parent(ancestor)
            depth++
        }
        return null
    }

    private fun <T> branchAlpha(start: T?, common: T?, nodes: Nodes<T?>): Float {
        var node = start
        var alpha = 1.0f
        var depth = 0
        while (node !== common && depth < MAX_DEPTH) {
            if (node == null) return Float.NaN
            if (!nodes.visible(node)) return 0.0f
            val value = nodes.alpha(node)
            if (!java.lang.Float.isFinite(value)) return Float.NaN
            alpha *= Motion.clamp(value)
            node = nodes.parent(node)
            depth++
        }
        return if (node === common) alpha else Float.NaN
    }

    @JvmStatic fun gain(batteryAlpha: Float, iconAlpha: Float): Float {
        if (!java.lang.Float.isFinite(batteryAlpha) || !java.lang.Float.isFinite(iconAlpha)) return 1.0f
        if (batteryAlpha <= 0.0f || iconAlpha <= 0.0f) return 0.0f
        return Motion.clamp(iconAlpha / batteryAlpha)
    }

    interface Nodes<T> {
        fun parent(node: T?): T?
        fun alpha(node: T?): Float
        fun visible(node: T?): Boolean
    }
}
