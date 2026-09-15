package com.codesteem.quranapp.UIModels

import android.text.SpannableStringBuilder
import com.codesteem.quranapp.UIModels.Anchors

data class QuranFlow(
    val content: SpannableStringBuilder,
    val anchors: List<Anchors>
)