package com.cyzah.circular.ui.preview

import android.graphics.Canvas
import android.graphics.Path
import com.cyzah.circular.domain.settings.ObservableSettingsValues
import com.cyzah.circular.ui.preview.SettingsPreviewViewBoundaryTest
import com.cyzah.circular.ui.preview.SettingsPreviewViewBoundaryTest.FailureSource
import com.cyzah.circular.ui.preview.SettingsPreviewViewBoundaryTest.TestCanvas
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

/** Executes the real View boundary; no Android native drawing or pixel output is used.  */
class SettingsPreviewViewBoundaryTest {
    @Test
    @Throws(Exception::class)
    fun frameIsClippedBeforeSettingsReadAndRestoredAfterFailure() {
        val source = FailureSource(IllegalStateException("configuration failed"))
        val view: SettingsPreviewView = view(source)
        val canvas: TestCanvas = canvas()
        source.canvas = canvas
        view.onDraw(canvas)
        Assert.assertTrue("native drawing must be clipped before any renderer work", source.clippedAtRead)
        Assert.assertFalse("the caller's canvas must not retain the preview clip", canvas.previewClipped)
        Assert.assertEquals(1, canvas.depth.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun runtimeFailureRestoresWholeCanvasAndStopsAutomaticRetries() {
        val source = FailureSource(IllegalStateException("configuration failed"))
        val view: SettingsPreviewView = view(source)
        val canvas: TestCanvas = canvas()
        view.onDraw(canvas)
        Assert.assertEquals(1, canvas.depth.toLong())
        Assert.assertEquals(1, canvas.restores.toLong())
        Assert.assertEquals(1, source.reads.toLong())
        view.onDraw(canvas)
        Assert.assertEquals(1, source.reads.toLong())
        Assert.assertEquals(1, canvas.saves.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun restoreFailureIsSuppressedOnTheOriginalRuntimeFailure() {
        val original: RuntimeException = IllegalStateException("configuration failed")
        val source = FailureSource(original)
        val view: SettingsPreviewView = view(source)
        val canvas: TestCanvas = canvas()
        canvas.restoreFailure = IllegalArgumentException("restore failed")
        view.onDraw(canvas)
        Assert.assertArrayEquals(arrayOf<Throwable?>(canvas.restoreFailure), original.getSuppressed())
    }

    @Test
    @Throws(Exception::class)
    fun fatalFailurePropagatesAfterCanvasRestoration() {
        val original = AssertionError("fatal configuration")
        val view: SettingsPreviewView = view(FailureSource(original))
        val canvas: TestCanvas = canvas()
        try {
            view.onDraw(canvas)
            Assert.fail("fatal errors must not be swallowed")
        } catch (actual: AssertionError) {
            Assert.assertSame(original, actual)
        }
        Assert.assertEquals(1, canvas.depth.toLong())
        Assert.assertEquals(1, canvas.restores.toLong())
    }

    private class FailureSource(@JvmField val failure: Throwable) : ObservableSettingsValues {
        @JvmField var reads: Int = 0
        @JvmField var canvas: TestCanvas? = null
        @JvmField var clippedAtRead: Boolean = false
        override fun register(listener: ObservableSettingsValues.Listener) {}
        override fun unregister() {}
        override fun contains(key: String?): Boolean {
            reads++
            clippedAtRead = canvas != null && canvas!!.previewClipped
            if (failure is Error) throw failure
            throw failure as RuntimeException
        }

        override fun getInt(key: String?, fallback: Int): Int {
            throw AssertionError()
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            throw AssertionError()
        }

        override fun getString(key: String?, fallback: String?): String? {
            throw AssertionError()
        }
    }

    private class TestCanvas : Canvas() {
        @JvmField var depth: Int = 0
        @JvmField var saves: Int = 0
        @JvmField var restores: Int = 0
        @JvmField var clipDepth: Int = 0
        @JvmField var previewClipped: Boolean = false
        @JvmField var restoreFailure: RuntimeException? = null
        override fun save(): Int {
            saves++
            return depth++
        }

        override fun clipPath(path: Path): Boolean {
            Assert.assertNotNull("the native preview needs a real clipping boundary", path)
            previewClipped = true
            clipDepth = depth
            return true
        }

        override fun restoreToCount(count: Int) {
            restores++
            depth = count
            if (count < clipDepth) previewClipped = false
            restoreFailure?.let { throw it }
        }
    }

    companion object {
        @Throws(Exception::class)
        private fun view(source: FailureSource): SettingsPreviewView {
            val view: SettingsPreviewView = Companion.allocate(SettingsPreviewView::class.java)
            val field = SettingsPreviewView::class.java.getDeclaredField("settingsCache")
            field.setAccessible(true)
            field.set(view, PreviewSettingsCache(source, object : Runnable {
                override fun run() {}
            }))
            // The clip path is initialized by the real View constructor, bypassed by this fixture.
            for (member in SettingsPreviewView::class.java.getDeclaredFields()) {
                if (member.getType() == Path::class.java) {
                    member.setAccessible(true)
                    member.set(view, Companion.allocate(Path::class.java))
                }
            }
            return view
        }

        @Throws(Exception::class)
        private fun canvas(): TestCanvas {
            val canvas: TestCanvas = Companion.allocate(TestCanvas::class.java)
            canvas.depth = 1
            return canvas
        }

        @Throws(Exception::class)
        private fun <T : Any> allocate(type: Class<T>): T {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.setAccessible(true)
            val allocate = unsafeClass.getMethod("allocateInstance", Class::class.java)
            return checkNotNull(type.cast(allocate.invoke(field.get(null), type)))
        }
    }
}
