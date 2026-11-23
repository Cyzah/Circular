package com.cyzah.circular.data.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StyleProfile
import com.cyzah.circular.domain.settings.StyleSettings
import com.cyzah.circular.ui.preview.PreviewSettingsCache
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.HashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class SharedPreferencesSettingsStoreTest {
    @Test
    fun realAdapterPublishesCompleteTransactionAndReleasesPreviewListener() {
        val preferences = Preferences()
        val adapter =
            SharedPreferencesSettingsStore(preferences.proxy)
        val styles = StyleSettings(adapter)
        val cache = PreviewSettingsCache(adapter, object : Runnable {
            override fun run() {}
        })
        cache.attach()
        Assert.assertEquals(DualStyle.LAYERED, cache.settings().dualStyle())
        Assert.assertTrue(styles.select(DualStyle.DUO, true))
        Assert.assertEquals(1, preferences.applies.toLong())
        Assert.assertEquals(DualStyle.DUO, cache.settings().dualStyle())
        Assert.assertEquals(26, cache.settings().size().toLong())

        preferences.proxy.edit().putInt("size", 28).putInt("weight_v2", 150).apply()
        Assert.assertEquals(28, cache.settings().size().toLong())
        Assert.assertTrue(styles.ensureAvailable(false))
        Assert.assertEquals(DualStyle.LAYERED, cache.settings().dualStyle())
        Assert.assertEquals(24, cache.settings().size().toLong())
        Assert.assertEquals(28, StyleProfile.Companion.decode(adapter.getString("style_profile_duo", ""))!!.size.toLong())
        Assert.assertTrue(styles.select(DualStyle.DUO, true))
        Assert.assertEquals(28, cache.settings().size().toLong())
        Assert.assertEquals(150, cache.settings().weight().toLong())
        Assert.assertEquals(1, preferences.listeners.size.toLong())

        cache.detach()
        Assert.assertTrue(preferences.listeners.isEmpty())
        preferences.proxy.edit().putInt("size", 27).apply()
        cache.attach()
        Assert.assertEquals(27, cache.settings().size().toLong())
        Assert.assertEquals(1, preferences.listeners.size.toLong())
        cache.detach()
        Assert.assertTrue(preferences.listeners.isEmpty())
    }

    /** Minimal Android preference boundary fake; transactions run in production code.  */
    class Preferences : InvocationHandler {
        @JvmField val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        @JvmField val listeners: ArrayList<OnSharedPreferenceChangeListener> = ArrayList<OnSharedPreferenceChangeListener>()
        @JvmField val proxy: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.getClassLoader(), arrayOf<Class<*>>(SharedPreferences::class.java), this
        ) as SharedPreferences
        @JvmField var applies: Int = 0

        override fun invoke(ignored: Any?, method: Method, arguments: Array<Any?>?): Any? {
            val args = arguments.orEmpty()
            val name = method.getName()
            if (name == "contains") {
                return values.containsKey(args[0])
            }
            if (name == "getAll") {
                return HashMap<String?, Any?>(values)
            }
            if (name.startsWith("get")) {
                return if (values.containsKey(args[0])) values.get(args[0]) else args[1]
            }
            if (name == "registerOnSharedPreferenceChangeListener") {
                listeners.add((args[0] as OnSharedPreferenceChangeListener?)!!)
                return null
            }
            if (name == "unregisterOnSharedPreferenceChangeListener") {
                listeners.remove(args[0])
                return null
            }
            if (name == "edit") {
                val pending: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                return Proxy.newProxyInstance(
                    SharedPreferences.Editor::class.java.getClassLoader(),
                    arrayOf<Class<*>>(SharedPreferences.Editor::class.java), object : InvocationHandler {
                        override fun invoke(editor: Any?, editMethod: Method, arguments: Array<Any?>?): Any? {
                            val editArgs = arguments.orEmpty()
                            if (editMethod.getName().startsWith("put")) {
                                pending.put(editArgs[0] as String?, editArgs[1])
                                return editor
                            }
                            if (editMethod.getName() == "apply") {
                                values.putAll(pending)
                                applies++
                                for (key in pending.keys) {
                                    for (listener
                                    in ArrayList<OnSharedPreferenceChangeListener>(listeners)) {
                                        listener.onSharedPreferenceChanged(proxy, key)
                                    }
                                }
                                return null
                            }
                            throw AssertionError(editMethod.getName())
                        }
                    })
            }
            throw AssertionError(name)
        }
    }
}
