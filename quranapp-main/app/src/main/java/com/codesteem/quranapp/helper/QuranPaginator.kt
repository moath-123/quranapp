package com.codesteem.quranapp.helper

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2

object QuranPaginator {

    fun paginateNextBatch(
        full: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        heightPx: Int,
        startFrom: Int,
        pagesToGenerate: Int = 20
    ): Pair<List<IntRange>, Int> {
        if (startFrom >= full.length) return emptyList<IntRange>() to full.length

        val ranges = ArrayList<IntRange>(pagesToGenerate)
        var start = startFrom
        var count = 0

        while (start < full.length && count < pagesToGenerate) {
            count++

            var low = start + 1
            var high = (start + 4000).coerceAtMost(full.length)

            while (high < full.length && fits(full, paint, widthPx, heightPx, start, high)) {
                low = high
                high = (high + 4000).coerceAtMost(full.length)
                if (high == full.length) break
            }

            var best = low
            var l = low
            var r = high

            while (l <= r) {
                val mid = (l + r) ushr 1
                if (fits(full, paint, widthPx, heightPx, start, mid)) {
                    best = mid
                    l = mid + 1
                } else {
                    r = mid - 1
                }
            }

            var end = best
            while (end > start && end < full.length && full[end - 1].isWhitespace()) end--
            if (end <= start) end = (start + 50).coerceAtMost(full.length)

            ranges.add(start until end)
            start = end
        }

        return ranges to start
    }


    /** Inflate the item layout and measure it at ViewPager2 size, return the target TextView. */
    fun measureTextArea(
        pager: ViewPager2,
        itemLayoutRes: Int,
        textViewId: Int
    ): TextView {
        val parent = pager as ViewGroup
        val v = LayoutInflater.from(pager.context).inflate(itemLayoutRes, parent, false)

        v.measure(
            View.MeasureSpec.makeMeasureSpec(pager.measuredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(pager.measuredHeight, View.MeasureSpec.EXACTLY)
        )
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)

        return v.findViewById(textViewId)
    }

    /** Fast paginator: finds page ranges that fit into (widthPx,heightPx). */
    fun paginateFast(
        full: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        heightPx: Int,
        maxPages: Int = 2000
    ): List<IntRange> {

        if (full.isEmpty() || widthPx <= 0 || heightPx <= 0) return emptyList()

        val ranges = ArrayList<IntRange>(700)
        var start = 0
        var pages = 0

        while (start < full.length && pages < maxPages) {
            pages++

            // If remaining is small, take all
            if (full.length - start < 2000) {
                ranges.add(start until full.length)
                break
            }

            // Expand upper bound
            var low = start + 1
            var high = (start + 4000).coerceAtMost(full.length)

            while (high < full.length && fits(full, paint, widthPx, heightPx, start, high)) {
                low = high
                high = (high + 4000).coerceAtMost(full.length)
                if (high == full.length) break
            }

            // Binary search
            var best = low
            var l = low
            var r = high

            while (l <= r) {
                val mid = (l + r) ushr 1
                if (fits(full, paint, widthPx, heightPx, start, mid)) {
                    best = mid
                    l = mid + 1
                } else {
                    r = mid - 1
                }
            }

            var end = best
            while (end > start && end < full.length && full[end - 1].isWhitespace()) end--

            if (end <= start) {
                end = (start + 50).coerceAtMost(full.length) // force progress
            }

            ranges.add(start until end)
            start = end
        }

        return ranges
    }

    private fun fits(
        full: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        heightPx: Int,
        start: Int,
        end: Int
    ): Boolean {
        val layout = buildLayout(full, paint, widthPx, start, end)
        return layout.height <= heightPx
    }

    private fun buildLayout(
        full: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        start: Int,
        end: Int
    ): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(full, start, end, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE) // RTL feel
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                full.subSequence(start, end),
                paint,
                widthPx,
                Layout.Alignment.ALIGN_OPPOSITE,
                1f,
                0f,
                false
            )
        }
    }
}
