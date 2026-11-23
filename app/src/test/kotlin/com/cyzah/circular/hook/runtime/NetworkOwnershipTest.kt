package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.network.SecondarySubscriptionBinding
import com.cyzah.circular.hook.platform.ObserverRegistration
import com.cyzah.circular.hook.runtime.NetworkOwnershipTest.ObserverPlatform
import com.cyzah.circular.hook.runtime.NetworkOwnershipTest.SecondaryPlatform
import java.util.ArrayList
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class NetworkOwnershipTest {
    @Test
    fun staleNativeSampleCannotOverridePlatformChange() {
        val labels = NativeTechnologyState()
        labels.sample(11, "5G")
        Assert.assertEquals("5G", labels.label(11))
        labels.invalidate(11)
        labels.sample(11, "5G") // Upstream StateFlow has not caught up yet.
        Assert.assertNull(labels.label(11))
        labels.sample(11, "4G")
        Assert.assertEquals("4G", labels.label(11))
    }

    @Test
    fun emptyNativeLabelClearsAndStableNativeLabelsRemainValid() {
        val labels = NativeTechnologyState()
        labels.sample(11, "5G")
        Assert.assertEquals("5G", labels.label(11))
        labels.sample(11, "")
        Assert.assertNull(labels.label(11))
        labels.sample(11, "4G")
        Assert.assertEquals("4G", labels.label(11))
        labels.emitted(11, "4G")
        Assert.assertEquals("4G", labels.label(11))
    }

    @Test
    fun authoritativeSameLabelEmissionRestoresInvalidatedSample() {
        val labels = NativeTechnologyState()
        labels.sample(11, "5GA")
        labels.invalidate(11)
        labels.emitted(11, "5GA")
        Assert.assertEquals("5GA", labels.label(11))
    }

    @Test
    fun nativeEventBeforePlatformEventKeepsCurrentCarrierLabel() {
        val labels = NativeTechnologyState()
        labels.sample(11, "5G")
        labels.emitted(11, "Carrier LTE")
        labels.invalidate(11)
        Assert.assertNull(labels.label(11))
        // Coalesced current read has authority only with the verified sink installed.
        labels.emitted(11, "Carrier LTE")
        Assert.assertEquals("Carrier LTE", labels.label(11))
        // A later platform transition without new native evidence must fall back.
        labels.invalidate(11)
        Assert.assertNull(labels.label(11))
    }

    @Test
    fun successfulObserverStartDoesNotDuplicateRegistrations() {
        val platform = ObserverPlatform()
        val registration = ObserverRegistration(platform)
        registration.start()
        registration.start()
        Assert.assertEquals(5, platform.active.toLong())
        Assert.assertEquals(5, platform.registerCalls.toLong())
        registration.stop()
        Assert.assertEquals(0, platform.active.toLong())
    }

    @Test
    fun removedSubscriptionsCannotResurrectOldSamples() {
        val labels = NativeTechnologyState()
        labels.sample(11, "5G")
        Assert.assertEquals("5G", labels.label(11))
        labels.retain(intArrayOf(22))
        Assert.assertNull(labels.label(11))
        labels.emitted(11, "5G") // Delayed callback from a retired view.
        Assert.assertNull(labels.label(11))
    }

    @Test
    fun partialObserverRegistrationRollsBackAtEveryPosition() {
        for (fail in 0..4) {
            val platform = ObserverPlatform()
            platform.failAt = fail
            val registration = ObserverRegistration(platform)
            registration.start()
            Assert.assertEquals((fail + 1).toLong(), platform.registerCalls.toLong())
            Assert.assertEquals(0, platform.active.toLong())
            Assert.assertFalse(registration.ownsRegistration())
        }
    }

    @Test
    fun observerCallThatRegistersThenThrowsIsAlsoRolledBack() {
        val platform = ObserverPlatform()
        platform.failAt = 0
        platform.failAfterSideEffect = true
        val registration = ObserverRegistration(platform)
        registration.start()
        Assert.assertEquals(0, platform.active.toLong())
        Assert.assertFalse(registration.ownsRegistration())
    }

    @Test
    fun failedObserverRollbackRetainsOwnershipUntilCleanupSucceeds() {
        val platform = ObserverPlatform()
        platform.failAt = 2
        platform.failCleanup = true
        val registration = ObserverRegistration(platform)
        registration.start()
        Assert.assertTrue(registration.ownsRegistration())
        val attempts = platform.registerCalls
        registration.start()
        Assert.assertEquals(attempts.toLong(), platform.registerCalls.toLong())
        platform.failCleanup = false
        registration.stop()
        Assert.assertEquals(0, platform.active.toLong())
        Assert.assertFalse(registration.ownsRegistration())
    }

    @Test
    fun secondaryRecoversWithoutRestartingPrimaryAndCancelsOnStop() {
        val scheduler = Queue()
        val platform = SecondaryPlatform()
        platform.failuresLeft = 1
        val binding = SecondarySubscriptionBinding<Resource>(platform, scheduler)
        binding.refresh()
        Assert.assertEquals(0, platform.active.toLong())
        Assert.assertEquals(1, scheduler.tasks.size.toLong())
        scheduler.fire()
        Assert.assertEquals(1, platform.active.toLong())
        Assert.assertTrue(binding.matches(platform.last, 22))
        binding.stop()
        Assert.assertEquals(0, platform.active.toLong())
        Assert.assertTrue(scheduler.tasks.isEmpty())
    }

    @Test
    fun alreadyDispatchedSecondaryRetryCannotRebindAfterStop() {
        val scheduler = Queue()
        val platform = SecondaryPlatform()
        platform.failuresLeft = 1
        val binding = SecondarySubscriptionBinding<Resource>(platform, scheduler)
        binding.refresh()
        val dispatched = scheduler.tasks.get(0)
        binding.stop()
        dispatched.run()
        Assert.assertEquals(0, platform.active.toLong())
        Assert.assertTrue(scheduler.tasks.isEmpty())
    }

    @Test
    fun secondarySubscriptionChangeCancelsOldRetryAndOldCallbacks() {
        val scheduler = Queue()
        val platform = SecondaryPlatform()
        val binding = SecondarySubscriptionBinding<Resource>(platform, scheduler)
        binding.refresh()
        Assert.assertEquals(1, platform.active.toLong())
        val old = platform.last
        platform.subscription = 33
        platform.failuresLeft = 1
        binding.refresh()
        Assert.assertFalse(binding.matches(old, 22))
        binding.stop()
        Assert.assertTrue(scheduler.tasks.isEmpty())
        Assert.assertEquals(0, platform.active.toLong())
    }

    @Test
    fun repeatedFailuresHaveOneRetryWithBoundedIncreasingDelay() {
        val scheduler = Queue()
        val platform = SecondaryPlatform()
        platform.failuresLeft = 100
        val binding = SecondarySubscriptionBinding<Resource>(platform, scheduler)
        binding.refresh()
        Assert.assertEquals(2000, scheduler.delay)
        scheduler.fire()
        Assert.assertEquals(4000, scheduler.delay)
        for (i in 0..9) scheduler.fire()
        Assert.assertEquals(60000, scheduler.delay)
        Assert.assertEquals(1, scheduler.tasks.size.toLong())
        binding.stop()
        Assert.assertEquals(0, platform.active.toLong())
    }

    private class ObserverPlatform : ObserverRegistration.Platform {
        @JvmField var failAt: Int = -1
        @JvmField var active: Int = 0
        @JvmField var registerCalls: Int = 0
        @JvmField var failCleanup: Boolean = false
        @JvmField var failAfterSideEffect: Boolean = false
        override fun register(index: Int) {
            registerCalls++
            check(!(index == failAt && !failAfterSideEffect)) { "register" }
            active++
            check(index != failAt) { "register after side effect" }
        }

        override fun unregister() {
            check(!failCleanup) { "cleanup" }
            active = 0
        }

        override fun failed(phase: String?, error: Throwable?) {}
    }

    private class Queue : SecondarySubscriptionBinding.Scheduler {
        @JvmField val tasks: MutableList<Runnable> = ArrayList<Runnable>()
        @JvmField var delay: Long = 0
        override fun post(task: Runnable?, delay: Long) {
            tasks.add(task!!)
            this.delay = delay
        }

        override fun remove(task: Runnable?) {
            tasks.remove(task)
        }

        fun fire() {
            tasks.removeAt(0).run()
        }
    }

    private class Resource {
        @JvmField var registered: Boolean = false
    }

    private class SecondaryPlatform : SecondarySubscriptionBinding.Platform<Resource> {
        @JvmField var subscription: Int = 22
        @JvmField var failuresLeft: Int = 0
        @JvmField var active: Int = 0
        @JvmField var last: Resource? = null
        override fun subscription(): Int {
            return subscription
        }

        override fun create(id: Int): Resource {
            return Resource().also { last = it }
        }

        override fun register(resource: Resource) {
            resource.registered = true
            active++
        }

        override fun refresh(resource: Resource) {
            check(failuresLeft-- <= 0) { "service unavailable" }
        }

        override fun close(resource: Resource) {
            if (resource.registered) {
                active--
                resource.registered = false
            }
        }

        override fun unavailable(id: Int) {}
        override fun failed(error: Throwable?) {}
    }
}
