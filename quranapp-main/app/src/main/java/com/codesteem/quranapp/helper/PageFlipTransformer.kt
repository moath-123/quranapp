package com.codesteem.quranapp.helper


import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class PageFlipTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        // position: 0 = centered, -1 = one full page to left, +1 = one full page to right
        page.cameraDistance = 20000f

        when {
            position < -1f || position > 1f -> {
                page.alpha = 0f
            }

            else -> {
                page.alpha = 1f

                // Pivot like a book: left page rotates around right edge, right page rotates around left edge
                page.pivotY = page.height * 0.5f
                page.pivotX = if (position < 0) page.width.toFloat() else 0f

                // Rotate Y for flip effect
                val rotation = 180f * position
                page.rotationY = rotation

                // Slight scaling for depth
                val scale = 0.9f + (1f - abs(position)) * 0.1f
                page.scaleX = scale
                page.scaleY = scale
            }
        }
    }
}
