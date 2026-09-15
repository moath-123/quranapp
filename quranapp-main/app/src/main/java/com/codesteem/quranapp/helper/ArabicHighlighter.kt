package com.codesteem.quranapp.helper

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan

object ArabicHighlighter {

    // normalize for matching (remove tashkeel, normalize alif forms, etc.)
    private fun normalizeChar(ch: Char): String {
        if (ch in '\u064B'..'\u065F') return ""  // harakat
        if (ch == '\u0670') return ""            // superscript alif

        return when (ch) {
            'أ','إ','آ','ٱ' -> "ا"
            'ى' -> "ي"
            'ة' -> "ه"
            else -> ch.toString()
        }
    }

    private fun normalizeString(s: String): String {
        val out = StringBuilder()
        for (ch in s) out.append(normalizeChar(ch))
        return out.toString()
    }

    private data class NormMap(val norm: String, val mapToOriginalIndex: IntArray)

    private fun buildNormalizedMap(original: String): NormMap {
        val normBuilder = StringBuilder()
        val mapList = ArrayList<Int>()

        for (i in original.indices) {
            val n = normalizeChar(original[i])
            if (n.isNotEmpty()) {
                for (k in n.indices) {
                    normBuilder.append(n[k])
                    mapList.add(i)
                }
            }
        }
        return NormMap(normBuilder.toString(), IntArray(mapList.size) { mapList[it] })
    }

    fun highlightUthmani(uthmani: String, query: String, highlightColor: Int): CharSequence {
        val q = normalizeString(query).trim()
        if (q.isEmpty()) return uthmani

        val nm = buildNormalizedMap(uthmani)
        val normText = nm.norm
        if (normText.isEmpty()) return uthmani

        val spannable = SpannableString(uthmani)

        var start = normText.indexOf(q, 0)
        while (start >= 0) {
            val end = start + q.length - 1
            val origStart = nm.mapToOriginalIndex[start]
            val origEnd = nm.mapToOriginalIndex[end] + 1

            if (origStart < origEnd) {
                spannable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    origStart,
                    origEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            start = normText.indexOf(q, start + q.length)
        }
        return spannable
    }



    fun highlightInPlace(spannable: SpannableStringBuilder, query: String, highlightColor: Int) {
        val q = query.trim()
        if (q.isEmpty()) return

        // Remove old highlights
        val old = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
        for (s in old) spannable.removeSpan(s)

        val text = spannable.toString()
        val highlighted = ArabicHighlighter.highlightUthmani(text, q, highlightColor)

        // Apply spans from highlighted result to original spannable
        // highlighted is SpannableString most of the time
        if (highlighted is Spanned) {
            val spans = highlighted.getSpans(0, highlighted.length, BackgroundColorSpan::class.java)
            for (sp in spans) {
                val start = highlighted.getSpanStart(sp)
                val end = highlighted.getSpanEnd(sp)
                if (start in 0..spannable.length && end in 0..spannable.length && start < end) {
                    spannable.setSpan(
                        BackgroundColorSpan(highlightColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

}
