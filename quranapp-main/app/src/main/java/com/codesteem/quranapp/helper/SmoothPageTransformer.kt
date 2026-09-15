package com.codesteem.quranapp.helper


import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class SmoothPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.cameraDistance = 20000f

        val absPos = abs(position)

        // Soft fade
        page.alpha = (1f - absPos * 0.35f).coerceIn(0.6f, 1f)

        // Slight scale (depth)
        val scale = 0.95f + (1f - absPos) * 0.05f
        page.scaleX = scale
        page.scaleY = scale

        // Mild Y rotation (paper feel)
        val rotation = position * -20f
        page.pivotY = page.height * 0.5f
        page.pivotX = if (position < 0) page.width.toFloat() else 0f
        page.rotationY = rotation

        // Parallax shift
        page.translationX = -position * page.width * 0.08f
    }
}
