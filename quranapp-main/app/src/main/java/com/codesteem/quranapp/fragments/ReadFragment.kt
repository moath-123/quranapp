package com.codesteem.quranapp.fragments

import android.os.Bundle
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.codesteem.quranapp.Adapters.ReadJuzPageAdapter
import com.codesteem.quranapp.Adapters.ReadSurahPageAdapter
import com.codesteem.quranapp.DataEntities.AyaEntity
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.JuzPageUiModel
import com.codesteem.quranapp.UIModels.SurahPageUiModel
import com.codesteem.quranapp.helper.QuranMetaDataHelper
import com.codesteem.quranapp.helper.SmoothPageTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadFragment : Fragment() {

    companion object {
        private const val ARG_SURA = "arg_sura"
        private const val ARG_CHAPTER = "arg_chapter"
        private const val BISMILLAH = "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ"

        fun newInstanceSurah(suraNumber: Int): ReadFragment {
            return ReadFragment().apply {
                arguments = Bundle().apply { putInt(ARG_SURA, suraNumber) }
            }
        }

        fun newInstanceJuz(chapterId: Int): ReadFragment {
            return ReadFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CHAPTER, chapterId) }
            }
        }
    }

    private lateinit var pager: ViewPager2
    private lateinit var surahAdapter: ReadSurahPageAdapter
    private lateinit var juzAdapter: ReadJuzPageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_read, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pager = view.findViewById(R.id.pagerSurah)

        // pager common setup once
        pager.offscreenPageLimit = 1
        pager.setPageTransformer(SmoothPageTransformer())
        pager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        pager.layoutDirection = View.LAYOUT_DIRECTION_RTL

        surahAdapter = ReadSurahPageAdapter()
        juzAdapter = ReadJuzPageAdapter()

        val hasSurah = arguments?.containsKey(ARG_SURA) == true
        val hasJuz = arguments?.containsKey(ARG_CHAPTER) == true

        when {
            hasJuz -> {
                val chapterId = arguments?.getInt(ARG_CHAPTER, -1) ?: -1
                if (chapterId <= 0) {
                    Toast.makeText(requireContext(), "Invalid Juz id: $chapterId", Toast.LENGTH_LONG).show()
                    return
                }
                pager.adapter = juzAdapter
                loadJuzPages(chapterId)
            }

            hasSurah -> {
                val suraNumber = arguments?.getInt(ARG_SURA, 1) ?: 1
                pager.adapter = surahAdapter
                loadSurahPages(suraNumber)
            }

            else -> {
                Toast.makeText(requireContext(), "No Surah/Juz argument found", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ------------------ shared helpers ------------------

    private fun toArabicIndicDigits(number: Int): String {
        val map = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
        return number.toString().map { ch ->
            if (ch in '0'..'9') map[ch - '0'] else ch
        }.joinToString("")
    }

    private fun ayahWithMarker(textUthmani: String, ayahNo: Int): String {
        val hasMarker = textUthmani.contains("﴿") || textUthmani.contains("﴾") ||
                textUthmani.contains('\u06DD') || textUthmani.contains('۝')

        return if (hasMarker) textUthmani else "$textUthmani ﴿${toArabicIndicDigits(ayahNo)}﴾"
    }

    private fun appendCenteredGreen(sb: SpannableStringBuilder, text: String, greenColor: Int) {
        val start = sb.length
        sb.append(text)
        val end = sb.length

        sb.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.setSpan(RelativeSizeSpan(1.25f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        sb.append("\n")
    }

    private fun appendDivider(sb: SpannableStringBuilder) {
        val start = sb.length
        sb.append("────────────")
        val end = sb.length
        sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n\n")
    }

    // ------------------ SURAH MODE ------------------

    private fun loadSurahPages(suraNumber: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pages = withContext(Dispatchers.IO) {
                    val dao = QuranDatabase.getInstance(requireContext()).quranDao()

                    val surah = dao.getAllSuras().firstOrNull { it.suraNumber == suraNumber }
                    val surahNameAr = surah?.nameAr ?: "سورة $suraNumber"
                    val surahNameEn = surah?.nameEn ?: "Surah $suraNumber"

                    val ayahs = dao.getAyasBySurahOrderedByPage(suraNumber)

                    buildSurahPagesFromDbPages(suraNumber, surahNameAr, surahNameEn, ayahs)
                }

                if (isAdded) {
                    surahAdapter.submitList(pages)
                    pager.setCurrentItem(0, false)
                }
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), e.message ?: "Failed to load surah", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildSurahPagesFromDbPages(
        suraNumber: Int,
        surahNameAr: String,
        surahNameEn: String,
        ayahs: List<AyaEntity>
    ): List<SurahPageUiModel> {

        val green = ContextCompat.getColor(requireContext(), R.color.quran_serial_no_color)
        val grouped = ayahs.groupBy { it.page }.toSortedMap()
        val pages = ArrayList<SurahPageUiModel>(grouped.size)

        var isFirst = true

        for ((pageNo, pageAyahs) in grouped) {
            val sb = SpannableStringBuilder()

            if (isFirst) {
                if (suraNumber != 9) appendCenteredGreen(sb, BISMILLAH, green)
                isFirst = false
            }

            for (a in pageAyahs) {
                sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber)).append("  ")
            }

            val juz = QuranMetaDataHelper.juzNameFor(pageAyahs.first().chapterId)

            pages.add(
                SurahPageUiModel(
                    pageNumber = pageNo,
                    surahNameAr = surahNameAr,
                    surahNameEn = surahNameEn,
                    juzNameAr = juz.ar,
                    juzNameEn = juz.en,
                    content = sb
                )
            )
        }

        return pages
    }

    // ------------------ JUZ MODE ------------------

    private fun loadJuzPages(chapterId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pages = withContext(Dispatchers.IO) {
                    val dao = QuranDatabase.getInstance(requireContext()).quranDao()
                    val suraMap = dao.getAllSuras().associateBy { it.suraNumber }
                    val ayahs = dao.getAyasByChapterOrderedByPage(chapterId)
                    val juz = QuranMetaDataHelper.juzNameFor(chapterId)

                    buildJuzPagesFromDbPages(chapterId, juz.ar, juz.en, suraMap, ayahs)
                }

                if (isAdded) {
                    juzAdapter.submitList(pages)
                    pager.setCurrentItem(0, false)
                }
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), e.message ?: "Failed to load juz", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildJuzPagesFromDbPages(
        chapterId: Int,
        juzNameAr: String,
        juzNameEn: String,
        suraMap: Map<Int, com.codesteem.quranapp.DataEntities.SuraEntity>,
        ayahs: List<AyaEntity>
    ): List<JuzPageUiModel> {

        val green = ContextCompat.getColor(requireContext(), R.color.quran_serial_no_color)
        val grouped = ayahs.groupBy { it.page }.toSortedMap()
        val pages = ArrayList<JuzPageUiModel>(grouped.size)

        if (ayahs.isEmpty()) return pages

        val firstSurahInJuz = ayahs.first().suraNumber
        var juzStartBismillahDone = false

        // Track the last surah we printed ayahs for (across pages)
        var lastSurahGlobal = -1

        for ((pageNo, pageAyahs) in grouped) {

            val sb = SpannableStringBuilder()

            // Header info: first ayah on this page
            val first = pageAyahs.first()
            val headerSurahAr = suraMap[first.suraNumber]?.nameAr ?: "سورة ${first.suraNumber}"
            val headerSurahEn = suraMap[first.suraNumber]?.nameEn ?: "Surah ${first.suraNumber}"

            for ((index, a) in pageAyahs.withIndex()) {

                val isNewSurah = (a.suraNumber != lastSurahGlobal)

                if (isNewSurah) {
                    val startsAtTopOfPage = (index == 0)
                    val isRealSurahStart = (a.ayaNumber == 1) // true only when a surah actually begins here

                    // ---------------- RULE 1: Juz start ----------------
                    if (!juzStartBismillahDone) {
                        juzStartBismillahDone = true
                        lastSurahGlobal = a.suraNumber

                        // Show bismillah once at the very start of the juz (except surah 9)
                        if (firstSurahInJuz != 9) {
                            appendCenteredGreen(sb, BISMILLAH, green)
                            // small spacing after bismillah
                            sb.append("")
                        }

                        // IMPORTANT: do NOT also show "surah-start bismillah" here,
                        // otherwise it duplicates. We skip further surah-start inserts for this first item.
                    } else {

                        // ---------------- RULE 2 + 3: Surah start logic ----------------

                        // Only do surah-start inserts if this is truly the start of a surah (ayah 1).
                        if (isRealSurahStart) {

                            if (startsAtTopOfPage) {
                                // RULE 2: new surah starts at top of page -> ONLY bismillah (except surah 9)
                                if (a.suraNumber != 9) {
                                    appendCenteredGreen(sb, BISMILLAH, green)
                                    sb.append("")
                                }
                            } else {
                                // RULE 3: new surah starts mid-page -> newline + surah name + bismillah (except 9)
                                sb.append("\n") // small break so title doesn't attach to previous ayah

                                val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                                appendCenteredGreen(sb, surahAr, green)

                                if (a.suraNumber != 9) {
                                    appendCenteredGreen(sb, BISMILLAH, green)
                                }

                                sb.append("")
                            }
                        }

                        lastSurahGlobal = a.suraNumber
                    }
                }

                // ---------------- RULE 4: Continuation -> no extra inserts ----------------
                // Append ayah as normal
                sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber))
                sb.append("  ")
            }

            pages.add(
                JuzPageUiModel(
                    pageNumber = pageNo,
                    surahNameAr = headerSurahAr,
                    surahNameEn = headerSurahEn,
                    juzNameAr = juzNameAr,
                    juzNameEn = juzNameEn,
                    content = sb
                )
            )
        }

        return pages
    }



    /*  private fun buildJuzPagesFromDbPages(
        chapterId: Int,
        juzNameAr: String,
        juzNameEn: String,
        suraMap: Map<Int, com.codesteem.quranapp.DataEntities.SuraEntity>,
        ayahs: List<AyaEntity>
    ): List<JuzPageUiModel> {

        val green = ContextCompat.getColor(requireContext(), R.color.quran_serial_no_color)
        val grouped = ayahs.groupBy { it.page }.toSortedMap()
        val pages = ArrayList<JuzPageUiModel>(grouped.size)

        if (ayahs.isEmpty()) return pages

        // Juz start surah
        val firstSurahInJuz = ayahs.first().suraNumber
        var bismillahAddedForJuzStart = false

        // Track surah changes across pages
        var lastSurahGlobal = -1

        for ((pageNo, pageAyahs) in grouped) {
            val sb = SpannableStringBuilder()

            // ✅ Bismillah at start of Juz (once)
            if (!bismillahAddedForJuzStart) {
                bismillahAddedForJuzStart = true
                if (firstSurahInJuz != 9) {
                    appendCenteredGreen(sb, BISMILLAH, green)
                }
            }

            for ((index, a) in pageAyahs.withIndex()) {

                val isNewSurah = (a.suraNumber != lastSurahGlobal)

                *//*if (isNewSurah) {
                    // Surah is starting now
                    val startsAtTopOfPage = (index == 0)

                    if (a.suraNumber != 9) {
                        if (startsAtTopOfPage) {
                            // ✅ new surah starts at top of new page
                            // header already shows surah name -> only bismillah
                            appendCenteredGreen(sb, BISMILLAH, green)
                        } else {
                            // ✅ new surah starts mid-page
                            // show surah name + bismillah
                            val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                            appendCenteredGreen(sb, surahAr, green)
                            appendCenteredGreen(sb, BISMILLAH, green)
                        }
                    } else {
                        // Surah 9: no bismillah; but if it starts mid-page you may still want the surah name
                        if (!startsAtTopOfPage) {
                            val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                            appendCenteredGreen(sb, surahAr, green)
                        }
                    }

                    lastSurahGlobal = a.suraNumber
                }*//*

                if (isNewSurah) {
                    val startsAtTopOfPage = (index == 0)

                    // Add spacing AFTER previous surah ends
                    if (lastSurahGlobal != -1) {
                        sb.append("\n\n")
                    }

                    if (a.suraNumber != 9) {
                        if (startsAtTopOfPage) {
                            // New surah starts at top of page → header already shows name
                            appendCenteredGreen(sb, BISMILLAH, green)
                        } else {
                            // New surah starts mid-page → show centered surah name + bismillah
                            val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"

                            appendCenteredGreen(sb, surahAr, green)
                            appendCenteredGreen(sb, BISMILLAH, green)
                        }
                    } else {
                        // Surah 9 (no bismillah)
                        if (!startsAtTopOfPage) {
                            val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                            appendCenteredGreen(sb, surahAr, green)
                        }
                    }

                    lastSurahGlobal = a.suraNumber
                }

                // Add ayah
                sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber))
                sb.append("  ")
            }

            // Header for this page
            val first = pageAyahs.first()
            val headerSurahAr = suraMap[first.suraNumber]?.nameAr ?: "سورة ${first.suraNumber}"
            val headerSurahEn = suraMap[first.suraNumber]?.nameEn ?: "Surah ${first.suraNumber}"

            pages.add(
                JuzPageUiModel(
                    pageNumber = pageNo,
                    surahNameAr = headerSurahAr,
                    surahNameEn = headerSurahEn,
                    juzNameAr = juzNameAr,
                    juzNameEn = juzNameEn,
                    content = sb
                )
            )
        }

        return pages
    }*/



    /* private fun buildJuzPagesFromDbPages(
         chapterId: Int,
         juzNameAr: String,
         juzNameEn: String,
         suraMap: Map<Int, com.codesteem.quranapp.DataEntities.SuraEntity>,
         ayahs: List<AyaEntity>
     ): List<JuzPageUiModel> {

         val green = ContextCompat.getColor(requireContext(), R.color.quran_serial_no_color)
         val grouped = ayahs.groupBy { it.page }.toSortedMap()
         val pages = ArrayList<JuzPageUiModel>(grouped.size)

         var lastSurah = -1

         for ((pageNo, pageAyahs) in grouped) {
             val sb = SpannableStringBuilder()

             for (a in pageAyahs) {
                 if (a.suraNumber != lastSurah) {
                     if (lastSurah != -1) appendDivider(sb)
                     lastSurah = a.suraNumber

                     val surahAr = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                     appendCenteredGreen(sb, surahAr, green)
                     if (a.suraNumber != 9) appendCenteredGreen(sb, BISMILLAH, green)
                 }

                 sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber)).append("  ")
             }

             val first = pageAyahs.first()
             val headerSurahAr = suraMap[first.suraNumber]?.nameAr ?: "سورة ${first.suraNumber}"
             val headerSurahEn = suraMap[first.suraNumber]?.nameEn ?: "Surah ${first.suraNumber}"

             pages.add(
                 JuzPageUiModel(
                     pageNumber = pageNo,
                     surahNameAr = headerSurahAr,
                     surahNameEn = headerSurahEn,
                     juzNameAr = juzNameAr,
                     juzNameEn = juzNameEn,
                     content = sb
                 )
             )
         }

         return pages
     }*/
}
