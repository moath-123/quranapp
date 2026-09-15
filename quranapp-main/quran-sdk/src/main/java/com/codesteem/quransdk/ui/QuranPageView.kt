package com.codesteem.quransdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.codesteem.quransdk.R
import com.codesteem.quransdk.api.QuranApi
import com.codesteem.quransdk.api.QuranPageListener
import com.codesteem.quransdk.internal.QuranApiImpl
import com.codesteem.quransdk.internal.ui.QuranPageAdapter
import kotlinx.coroutines.launch

/**
 * Embeddable mushaf page reader. Renders Quran pages only — no navigation, search, or menus.
 *
 * ```
 * val api = QuranSdk.create(context)
 * api.initialize()
 * val pageView = QuranPageView(context)
 * pageView.bind(api)
 * pageView.setPageListener(listener)
 * pageView.goToPage(1)
 * ```
 */
class QuranPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pager: ViewPager2
    private var adapter: QuranPageAdapter? = null
    private var boundApi: QuranApiImpl? = null
    private var maxPage: Int = 604
    private var listener: QuranPageListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.quransdk_page_view, this, true)
        pager = findViewById(R.id.quransdkPager)
        pager.layoutDirection = LAYOUT_DIRECTION_RTL
        pager.offscreenPageLimit = 1
        pager.getChildAt(0)?.overScrollMode = OVER_SCROLL_NEVER
    }

    /**
     * Binds a [QuranApi] instance. Call after [QuranApi.initialize].
     */
    fun bind(api: QuranApi) {
        val impl = api as? QuranApiImpl
            ?: error("QuranPageView requires an API instance from QuranSdk.create()")
        boundApi = impl
        val scope = findViewTreeLifecycleOwner()?.lifecycleScope
            ?: error("QuranPageView must be attached to a LifecycleOwner (use in Activity/Fragment)")
        val headerColor = ContextCompat.getColor(context, R.color.quransdk_header)
        val selectedColor = ContextCompat.getColor(context, R.color.quransdk_highlight)

        scope.launch {
            maxPage = api.getMaxPage()
            adapter = QuranPageAdapter(
                api = impl,
                scope = scope,
                maxPage = maxPage,
                headerColor = headerColor,
                selectedColor = selectedColor,
                listener = listener
            )
            pager.adapter = adapter
        }
    }

    fun setPageListener(listener: QuranPageListener?) {
        this.listener = listener
        adapter?.setListener(listener)
    }

    fun goToPage(page: Int) {
        val safe = page.coerceIn(1, maxPage)
        pager.post { pager.setCurrentItem(safe - 1, false) }
    }

    fun highlightAyah(ayahId: Int, page: Int) {
        adapter?.setSelectedAyah(ayahId, page)
    }

    fun clearHighlight() {
        adapter?.setSelectedAyah(null, 1)
    }
}
