package com.codesteem.quransdk.internal.ui

import android.text.SpannableStringBuilder
import com.codesteem.quransdk.internal.data.dao.QuranDao
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity
import com.codesteem.quransdk.internal.util.QuranMetaDataHelper

internal data class QuranPageContent(
    val pageNumber: Int,
    val surahNameAr: String,
    val juzNameAr: String,
    val content: SpannableStringBuilder,
    val ayahRanges: List<AyahTextRange>
)

internal data class AyahTextRange(
    val start: Int,
    val end: Int,
    val ayah: AyaEntity
)

internal object QuranPageBuilder {

    suspend fun buildPage(
        dao: QuranDao,
        suraMap: Map<Int, SuraEntity>,
        pageNo: Int,
        headerColor: Int
    ): QuranPageContent {
        val ayas = dao.getAyasByPage(pageNo)
        if (ayas.isEmpty()) {
            return QuranPageContent(
                pageNumber = pageNo,
                surahNameAr = "",
                juzNameAr = "",
                content = SpannableStringBuilder(""),
                ayahRanges = emptyList()
            )
        }

        val first = ayas.first()
        val surahNameAr = suraMap[first.suraNumber]?.nameAr ?: "سورة ${first.suraNumber}"
        val juzNameAr = QuranMetaDataHelper.juzNameFor(first.chapterId).ar
        val bismillah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

        val sb = SpannableStringBuilder()
        val ranges = ArrayList<AyahTextRange>(ayas.size)
        var lastSura = -1

        for (a in ayas) {
            if (a.suraNumber != lastSura && a.ayaNumber == 1) {
                val sName = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                appendCenteredHeader(sb, sName, headerColor)
                if (a.suraNumber != 9) appendCenteredHeader(sb, bismillah, headerColor)
            }
            lastSura = a.suraNumber

            val start = sb.length
            sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber))
            val end = sb.length
            ranges.add(AyahTextRange(start, end, a))
            sb.append("  ")
        }

        return QuranPageContent(pageNo, surahNameAr, juzNameAr, sb, ranges)
    }

    private fun ayahWithMarker(textUthmani: String, ayahNo: Int): String {
        if (textUthmani.contains("﴿") || textUthmani.contains("﴾")) return textUthmani
        return "$textUthmani ﴿${toArabicIndicDigits(ayahNo)}﴾"
    }

    private fun toArabicIndicDigits(number: Int): String {
        val map = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { ch -> if (ch in '0'..'9') map[ch - '0'] else ch }.joinToString("")
    }

    private fun appendCenteredHeader(sb: SpannableStringBuilder, text: String, color: Int) {
        if (sb.isNotEmpty() && sb[sb.length - 1] != '\n') sb.append("\n")
        val start = sb.length
        sb.append(text)
        val end = sb.length
        sb.setSpan(
            android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
            start, end,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.setSpan(
            android.text.style.RelativeSizeSpan(1.15f),
            start, end,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.setSpan(
            android.text.style.ForegroundColorSpan(color),
            start, end,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.append("\n")
    }
}
