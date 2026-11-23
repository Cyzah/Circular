package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

/** Packs the surviving slots in their native order and aligns their two copies.  */
class NativeIconPacking<T>(nodes: Nodes<T?>) {
    interface Nodes<T> {
        fun slot(node: T?): String?
        fun visible(node: T?): Boolean
        fun width(node: T?): Float
        fun place(node: T?, right: Float)
        fun opacity(node: T?, value: Float)
        fun release(node: T?)
    }

    private val nodes: Nodes<T?>
    private val active = ArrayList<T?>()
    private val next = ArrayList<T?>()
    private val rowTargets = ArrayList<T?>()
    private val rowSources = ArrayList<T?>()

    init {
        requireNotNull(nodes) { "nodes" }
        this.nodes = nodes
    }

    @JvmOverloads
    fun apply(
        target: List<T?>, source: List<T?>, right: Float, spacing: Float,
        extraSpace: Float = 0.0f, extraAlpha: Float = 0.0f
    ) {
        if (!java.lang.Float.isFinite(right) || !java.lang.Float.isFinite(spacing) || !java.lang.Float.isFinite(extraSpace) || !java.lang.Float.isFinite(
                extraAlpha
            )
        ) {
            release()
            return
        }
        next.clear()
        merge(target, source)
        val space = max(0.0f, min(1.0f, extraSpace))
        // A native icon keeps its full size. Reveal it only after its slot is full.
        val alpha = if (space == 1.0f) max(0.0f, min(1.0f, extraAlpha)) else
            0.0f
        val gap = max(0.0f, spacing)
        var cursor = right
        for (index in rowTargets.indices.reversed()) {
            val node = rowTargets.get(index)
            val copy = rowSources.get(index)
            if (node == null) {
                place(copy, cursor, alpha)
                cursor -= (nodes.width(copy) + gap) * space
            } else if (copy == null) {
                // A slot absent from the expanded native row must also give
                // back its space before the final native handoff. Fade first:
                // moving a neighbour into a still-visible glyph would overlap.
                val retainedAlpha = 1.0f - Motion.ease(space / 0.2f)
                val retainedSpace = 1.0f - Motion.ease((space - 0.2f) / 0.8f)
                place(node, cursor, retainedAlpha)
                cursor -= (nodes.width(node) + gap) * retainedSpace
            } else {
                var width = nodes.width(node)
                if (copy != null) width = max(width, nodes.width(copy))
                place(node, cursor, 1.0f)
                if (copy != null) place(copy, cursor, 1.0f)
                cursor -= width + gap
            }
        }
        for (previous in active) {
            if (!containsIdentity(next, previous)) nodes.release(previous)
        }
        active.clear()
        active.addAll(next)
        rowTargets.clear()
        rowSources.clear()
    }

    fun release() {
        for (node in active) nodes.release(node)
        active.clear()
        next.clear()
        rowTargets.clear()
        rowSources.clear()
    }

    private fun merge(target: List<T?>, source: List<T?>) {
        rowTargets.clear()
        rowSources.clear()
        var sourceCursor = 0
        for (node in target) {
            if (!eligible(node)) continue
            val matchingIndex = matching(source, nodes.slot(node))
            if (matchingIndex >= sourceCursor) {
                appendExtras(target, source, sourceCursor, matchingIndex)
                sourceCursor = matchingIndex + 1
            }
            rowTargets.add(node)
            rowSources.add(if (matchingIndex < 0) null else source.get(matchingIndex))
        }
        appendExtras(target, source, sourceCursor, source.size)
    }

    private fun appendExtras(target: List<T?>, source: List<T?>, start: Int, end: Int) {
        for (index in start..<end) {
            val node = source.get(index)
            if (eligible(node) && matching(target, nodes.slot(node)) < 0) {
                rowTargets.add(null)
                rowSources.add(node)
            }
        }
    }

    private fun place(node: T?, right: Float, alpha: Float) {
        nodes.opacity(node, alpha)
        nodes.place(node, right)
        if (!containsIdentity(next, node)) next.add(node)
    }

    private fun matching(source: List<T?>, slot: String?): Int {
        if (slot == null) return -1
        for (index in source.indices) {
            val node = source.get(index)
            if (eligible(node) && slot == nodes.slot(node)) return index
        }
        return -1
    }

    private fun containsIdentity(values: List<T?>, candidate: T?): Boolean {
        for (value in values) {
            if (value === candidate) return true
        }
        return false
    }

    private fun eligible(node: T?): Boolean {
        if (node == null || !nodes.visible(node)) return false
        val width = nodes.width(node)
        return java.lang.Float.isFinite(width) && width > 0.0f
    }
}
