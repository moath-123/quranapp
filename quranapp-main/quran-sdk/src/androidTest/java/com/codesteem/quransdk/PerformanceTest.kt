package com.codesteem.quransdk

import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codesteem.quransdk.internal.data.QuranImporter
import com.codesteem.quransdk.ui.QuranPageView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rough numbers for the readiness report. Results are printed as `QURAN_PERF key=value`
 * in the instrumentation output; bounds are deliberately generous (emulators are slow).
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    private fun report(key: String, value: Any) {
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("stream", "QURAN_PERF $key=$value\n")
        })
    }

    @Test
    fun firstLaunchImportTime() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("quransdk_meta", Context.MODE_PRIVATE).edit().clear().commit()
        val api = QuranSdk.create(context)
        val start = SystemClock.elapsedRealtime()
        api.initialize()
        val importMs = SystemClock.elapsedRealtime() - start

        val warm = SystemClock.elapsedRealtime()
        api.initialize()
        val warmMs = SystemClock.elapsedRealtime() - warm

        val s = SystemClock.elapsedRealtime()
        api.search("الرحمن")
        val firstSearchMs = SystemClock.elapsedRealtime() - s
        val s2 = SystemClock.elapsedRealtime()
        api.search("الله")
        val searchMs = SystemClock.elapsedRealtime() - s2

        report("import_first_launch_ms", importMs)
        report("initialize_warm_ms", warmMs)
        report("search_first_ms", firstSearchMs)
        report("search_cached_ms", searchMs)
        report("data_version", QuranImporter.DATA_VERSION)
        assertTrue("import took $importMs ms", importMs < 30_000)
        assertTrue("warm initialize took $warmMs ms", warmMs < 1_000)
    }

    @Test
    fun memoryWhilePagingThroughFiftyPages() {
        val scenario = ActivityScenario.launch(TestHostActivity::class.java)
        lateinit var view: QuranPageView
        var ready = false
        scenario.onActivity { activity ->
            view = QuranPageView(activity).apply { id = View.generateViewId() }
            activity.setContentView(view)
            val api = QuranSdk.create(activity)
            activity.lifecycleScope.launch {
                api.initialize()
                view.bind(api)
                view.goToPage(1)
                ready = true
            }
        }
        waitUntil { ready }
        Thread.sleep(1_000)

        fun usedMb(): Long {
            Runtime.getRuntime().gc()
            Thread.sleep(300)
            val rt = Runtime.getRuntime()
            return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        }

        val before = usedMb()
        val nativeBefore = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val start = SystemClock.elapsedRealtime()
        for (page in 2..51) {
            scenario.onActivity { view.goToPage(page) }
            Thread.sleep(120)
        }
        Thread.sleep(1_000)
        val pagingMs = SystemClock.elapsedRealtime() - start
        val after = usedMb()
        val nativeAfter = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        scenario.close()

        report("java_heap_before_mb", before)
        report("java_heap_after_50_pages_mb", after)
        report("native_heap_before_mb", nativeBefore)
        report("native_heap_after_50_pages_mb", nativeAfter)
        report("paging_50_pages_ms", pagingMs)
        assertTrue("java heap grew ${after - before} MB", after - before < 64)
    }

    private fun waitUntil(timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (!condition()) {
            if (SystemClock.uptimeMillis() > end) throw AssertionError("timeout")
            Thread.sleep(100)
        }
    }
}
