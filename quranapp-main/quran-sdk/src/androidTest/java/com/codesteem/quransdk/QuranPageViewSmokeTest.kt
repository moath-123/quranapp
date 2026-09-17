package com.codesteem.quransdk

import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.codesteem.quransdk.api.model.Ayah
import com.codesteem.quransdk.api.QuranPageListener
import com.codesteem.quransdk.ui.QuranPageView
import kotlinx.coroutines.launch
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class QuranPageViewSmokeTest {

    private lateinit var scenario: ActivityScenario<TestHostActivity>
    private lateinit var pageView: QuranPageView
    private val taps = CopyOnWriteArrayList<Ayah>()
    private val longPresses = CopyOnWriteArrayList<Ayah>()

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        scenario.onActivity { activity ->
            pageView = QuranPageView(activity).apply { id = View.generateViewId() }
            activity.setContentView(pageView)
            val api = QuranSdk.create(activity)
            activity.lifecycleScope.launch {
                api.initialize()
                pageView.setPageListener(object : QuranPageListener {
                    override fun onAyahTapped(ayah: Ayah) { taps += ayah }
                    override fun onAyahLongPressed(ayah: Ayah, screenX: Float, screenY: Float) { longPresses += ayah }
                })
                pageView.bind(api)
                // Called right after bind(), before the adapter exists — this used to be ignored.
                pageView.goToPage(50)
            }
        }
    }

    @After
    fun tearDown() = scenario.close()

    @Test
    fun opensRequestedPageAndReportsTapsOnThatPage() {
        waitForDisplayedPage(50)
        onView(allOf(withId(R.id.txtPageText), isDisplayed())).perform(click())
        waitUntil({ "tap callback" }) { taps.isNotEmpty() }
        assertEquals(50, taps.first().pageNumber)
        assertTrue(longPresses.isEmpty())
    }

    @Test
    fun swipingRightGoesToTheNextPageInRtl() {
        waitForDisplayedPage(50)
        onView(withId(pageView.id)).perform(swipeRight())
        waitForDisplayedPage(51)
        onView(withId(pageView.id)).perform(swipeLeft())
        waitForDisplayedPage(50)
    }

    private fun waitForDisplayedPage(page: Int) {
        var state = ""
        waitUntil({ "page $page displayed ($state)" }) {
            var ok = false
            scenario.onActivity {
                val header = pageView.findDisplayed(R.id.txtPageNo)?.text?.toString()
                val body = pageView.findDisplayed(R.id.txtPageText)?.text?.toString().orEmpty()
                // currentItem changes when the settle animation starts; wait for it to finish too.
                val idle = pageView.findPager()?.scrollState == ViewPager2.SCROLL_STATE_IDLE
                state = "currentPage=${pageView.currentPage}, header=$header, bodyLength=${body.length}, idle=$idle"
                ok = idle && pageView.currentPage == page && header == page.toString() && body.length > 20
            }
            ok
        }
    }

    private fun View.findPager(): ViewPager2? = when (this) {
        is ViewPager2 -> this
        is android.view.ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findPager() }
        else -> null
    }

    /** The visible page's view with [id] (ViewPager2 keeps neighbours attached off-screen). */
    private fun View.findDisplayed(id: Int): TextView? {
        val out = CopyOnWriteArrayList<TextView>()
        fun walk(v: View) {
            if (v.id == id && v is TextView) out += v
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        val screen = android.graphics.Rect()
        return out.firstOrNull { it.isShown && it.getGlobalVisibleRect(screen) && screen.width() > it.width / 2 }
    }

    private fun waitUntil(what: () -> String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for ${what()}")
    }
}
