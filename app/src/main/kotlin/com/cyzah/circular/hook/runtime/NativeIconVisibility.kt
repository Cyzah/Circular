package com.cyzah.circular.hook.runtime

import android.view.View

/** Shares the native neighbour's fade without counting their common parent twice.  */
object NativeIconVisibility {
    private val NODES: NativeIconVisibilityPolicy.Nodes<View?> = object : NativeIconVisibilityPolicy.Nodes<View?> {
        override fun parent(view: View?): View? {
            val parent = view!!.getParent()
            return if (parent is View) parent as View else null
        }

        override fun alpha(view: View?): Float {
            return view!!.getAlpha()
        }

        override fun visible(view: View?): Boolean {
            return view!!.getVisibility() == View.VISIBLE
        }
    }

    @JvmStatic fun alpha(battery: View?, icons: View?): Float {
        return NativeIconVisibilityPolicy.alpha<View?>(battery, icons, NODES)
    }
}
