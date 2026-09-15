package com.codesteem.quranapp.Repository

import com.codesteem.quranapp.DAO.QuranDao
import com.codesteem.quranapp.UIModels.AyahUiModel
import com.codesteem.quranapp.UIModels.ChapterUiModel
import com.codesteem.quranapp.UIModels.SuraUiModel
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import com.codesteem.quranapp.DAO.SearchRow
import com.codesteem.quranapp.UIModels.Anchors
import com.codesteem.quranapp.UIModels.QuranFlow
import com.codesteem.quranapp.UIModels.QuranPageUiModel
import com.codesteem.quranapp.helper.ArabicNormalizer
import com.codesteem.quranapp.helper.QuranMetaDataHelper

class QuranRepository(private val dao: QuranDao) {

    suspend fun getAllSuras(): List<SuraUiModel> {
        return dao.getAllSuras().map {
            SuraUiModel(
                suraNumber = it.suraNumber,
                nameAr = it.nameAr,
                nameEn = it.nameEn,
                ayahCount = it.ayahCount,
                startPage = it.startPage
            )
        }
    }

    suspend fun getAllChapters(): List<ChapterUiModel> {
        return dao.getAllChapters().map {
            ChapterUiModel(
                chapterId = it.chapterId,
                nameEn = it.nameEn,      // Example: "Juz' 1"
                nameAr = it.nameAr,      // Example: "Juz' 1"
                startPage = it.startPage // Example: 1
            )
        }
    }

    suspend fun getAyahsBySurah(suraNumber: Int): List<AyahUiModel> {
        return dao.getAyasBySurah(suraNumber).map {
            AyahUiModel(
                id = it.id,
                suraNumber = it.suraNumber,
                ayaNumber = it.ayaNumber,
                page = it.page,
                textUthmani = it.textUthmani,
                ayaText = it.textSimple
            )
        }
    }

    suspend fun getAllAyahs(): List<AyahUiModel> {
        return dao.getAllAyasOrdered().map {
            AyahUiModel(
                id = it.id,
                suraNumber = it.suraNumber,
                ayaNumber = it.ayaNumber,
                textUthmani = it.textUthmani,
                ayaText = it.textSimple,
                page = it.page
            )
        }
    }
    suspend fun buildPreviewFlow(pageNos: List<Int>, greenColor: Int): QuranFlow {
        val suraMap = dao.getAllSuras().associateBy { it.suraNumber }
        val ayahs = dao.getAyasByPages(pageNos)

        val bismillah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

          fun toArabicIndicDigits(number: Int): String {
            val map = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
            return number.toString().map { ch ->
                if (ch in '0'..'9') map[ch - '0'] else ch
            }.joinToString("")
        }

          fun ayahWithMarker(textUthmani: String, ayahNo: Int): String {
            val num = toArabicIndicDigits(ayahNo)
            val marker = " ﴿$num﴾"
            if (textUthmani.contains("﴿") || textUthmani.contains("﴾")) return textUthmani
            return textUthmani + marker
        }

        fun appendCenteredGreen(sb: SpannableStringBuilder, text: String) {
            val start = sb.length
            sb.append(text)
            val end = sb.length
            sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(1.15f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        fun appendSurahDivider(sb: SpannableStringBuilder) {
            val start = sb.length
            sb.append("────────────")
            val end = sb.length
            sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        val sb = SpannableStringBuilder()
        val anchors = ArrayList<Anchors>(200)

        var lastSurahGlobally = -1

        for (ayah in ayahs) {
            val surahNameAr = suraMap[ayah.suraNumber]?.nameAr ?: "سورة ${ayah.suraNumber}"
            val chapterNameAr = QuranMetaDataHelper.juzNameFor(ayah.chapterId).ar

            anchors.add(Anchors(start = sb.length, surahNameAr = surahNameAr, chapterNameAr = chapterNameAr))

            if (ayah.suraNumber != lastSurahGlobally) {
                if (lastSurahGlobally != -1) appendSurahDivider(sb)
                lastSurahGlobally = ayah.suraNumber

                appendCenteredGreen(sb, surahNameAr)
                if (ayah.suraNumber != 9) appendCenteredGreen(sb, bismillah)
            }

            sb.append(ayahWithMarker(ayah.textUthmani, ayah.ayaNumber))
            sb.append("  ")
        }

        return QuranFlow(content = sb, anchors = anchors)
    }

    suspend fun buildQuranFlow(greenColor: Int): QuranFlow {
        val suraMap = dao.getAllSuras().associateBy { it.suraNumber }
        val ayahs = dao.getAllAyasOrderedByPage() // already ordered; ok

        val bismillah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

          fun toArabicIndicDigits(number: Int): String {
            val map = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
            return number.toString().map { ch ->
                if (ch in '0'..'9') map[ch - '0'] else ch
            }.joinToString("")
        }

          fun ayahWithMarker(textUthmani: String, ayahNo: Int): String {
            val num = toArabicIndicDigits(ayahNo)
            val marker = " ﴿$num﴾"
            if (textUthmani.contains("﴿") || textUthmani.contains("﴾")) return textUthmani
            return textUthmani + marker
        }

        fun appendCenteredGreen(sb: SpannableStringBuilder, text: String) {
            val start = sb.length
            sb.append(text)
            val end = sb.length
            sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(1.15f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        fun appendSurahDivider(sb: SpannableStringBuilder) {
            val start = sb.length
            sb.append("────────────")
            val end = sb.length
            sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        val sb = SpannableStringBuilder()
        val anchors = ArrayList<Anchors>(500)

        var lastSurahGlobally = -1

        for (ayah in ayahs) {
            val surahNameAr = suraMap[ayah.suraNumber]?.nameAr ?: "سورة ${ayah.suraNumber}"
            val chapterNameAr = QuranMetaDataHelper.juzNameFor(ayah.chapterId).ar

            // anchor at start of each ayah (good enough for page header)
            anchors.add(Anchors(start = sb.length, surahNameAr = surahNameAr, chapterNameAr = chapterNameAr))

            if (ayah.suraNumber != lastSurahGlobally) {
                if (lastSurahGlobally != -1) appendSurahDivider(sb)
                lastSurahGlobally = ayah.suraNumber

                appendCenteredGreen(sb, surahNameAr)
                if (ayah.suraNumber != 9) appendCenteredGreen(sb, bismillah)
            }

            sb.append(ayahWithMarker(ayah.textUthmani, ayah.ayaNumber))
            sb.append("  ")
        }

        return QuranFlow(content = sb, anchors = anchors)
    }

    // inside QuranRepository
    private var cachedSearch: List<Pair<SearchRow, String>>? = null

    private suspend fun getCachedSearchRows(): List<Pair<SearchRow, String>> {
        val current = cachedSearch
        if (current != null) return current

        val rows = dao.getAllSearchRows()
        val prepared = rows.map { row ->
            row to ArabicNormalizer.normalize(row.textSimple)
        }
        cachedSearch = prepared
        return prepared
    }

    suspend fun searchArabic(queryRaw: String, limit: Int = 200): List<SearchRow> {
        val q = ArabicNormalizer.normalize(queryRaw)
        if (q.isBlank()) return emptyList()

        val tokens = q.split(" ").filter { it.isNotBlank() }

        val items = getCachedSearchRows()

        // score = earlier match + more tokens matched naturally
        return items.asSequence()
            .mapNotNull { (row, normText) ->
                if (tokens.all { normText.contains(it) }) {
                    val score = tokens.sumOf { t ->
                        val idx = normText.indexOf(t)
                        if (idx >= 0) (10_000 - idx) else 0
                    }
                    row to score
                } else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }



}
