package com.codesteem.quranapp.Adapters

import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.DAO.QuranDao
import com.codesteem.quranapp.DataEntities.SuraEntity
import com.codesteem.quranapp.R
import com.codesteem.quranapp.UIModels.QuranPageUiModel
import com.codesteem.quranapp.fragments.ReadQuranPagesFragment
import com.codesteem.quranapp.helper.ArabicHighlighter.highlightInPlace
import com.codesteem.quranapp.helper.QuranMetaDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranPageAdapter(
    private val dao: QuranDao,
    private val scope: CoroutineScope,
    private val maxPage: Int,
    private val suraMap: Map<Int, SuraEntity>,
    private val headerColor: Int,
    private val selectedBgColor: Int,
    private val onAyahTapped: (ReadQuranPagesFragment.AyahSelection) -> Unit,
    private val onAyahLongPressed: (ReadQuranPagesFragment.AyahSelection, rawX: Float, rawY: Float) -> Unit
) : RecyclerView.Adapter<QuranPageAdapter.VH>() {

    data class AyahRange(
        val start: Int,
        val end: Int, // exclusive
        val ayaId: Int,
        val suraNumber: Int,
        val ayaNumber: Int,
        val chapterId: Int,
        val page: Int,
        val textUthmani: String,
        val textSimple: String
    )

    data class PageModel(
        val ui: QuranPageUiModel,
        val ranges: List<AyahRange>
    )

    private val cache = HashMap<Int, PageModel>()
    private val inFlight = HashSet<Int>()

    private var query: String? = null
    private var selectedAyahId: Int? = null
    private var selectedPage: Int? = null

    fun setQuery(q: String?) {
        query = q?.trim().takeUnless { it.isNullOrEmpty() }
        notifyDataSetChanged()
    }

    fun setSelectedAyah(ayaId: Int, page: Int) {
        val oldPage = selectedPage
        selectedAyahId = ayaId
        selectedPage = page

        oldPage?.let { if (it in 1..maxPage) notifyItemChanged(it - 1) }
        if (page in 1..maxPage) notifyItemChanged(page - 1)
    }

    override fun getItemCount(): Int = maxPage

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_quran_page, parent, false)
        return VH(v, onAyahTapped, onAyahLongPressed, selectedBgColor)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pageNo = position + 1
        holder.boundPage = pageNo

        cache[pageNo]?.let { model ->
            holder.bind(model, query, selectedAyahId)
            return
        }

        holder.bindLoading(pageNo)

        if (inFlight.contains(pageNo)) return
        inFlight.add(pageNo)

        scope.launch {
            val model = withContext(Dispatchers.IO) { buildPageModel(pageNo) }
            cache[pageNo] = model
            inFlight.remove(pageNo)

            if (holder.boundPage == pageNo) {
                holder.bind(model, query, selectedAyahId)
            }
        }
    }

    private suspend fun buildPageModel(pageNo: Int): PageModel {
        val ayas = dao.getAyasByPage(pageNo)
        if (ayas.isEmpty()) {
            val emptyUi = QuranPageUiModel(
                pageNumber = pageNo,
                surahNameAr = "",
                chapterNameAr = "",
                content = SpannableStringBuilder(""),
                searchText = ""
            )
            return PageModel(emptyUi, emptyList())
        }

        val first = ayas.first()
        val surahNameAr = suraMap[first.suraNumber]?.nameAr ?: "سورة ${first.suraNumber}"
        val chapterNameAr = QuranMetaDataHelper.juzNameFor(first.chapterId).ar

        val bismillah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

        fun toArabicIndicDigits(number: Int): String {
            val map = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
            return number.toString().map { ch -> if (ch in '0'..'9') map[ch - '0'] else ch }.joinToString("")
        }

        fun ayahWithMarker(textUthmani: String, ayahNo: Int): String {
            val num = toArabicIndicDigits(ayahNo)
            val marker = " ﴿$num﴾"
            if (textUthmani.contains("﴿") || textUthmani.contains("﴾")) return textUthmani
            return textUthmani + marker
        }

        fun ensureNewLine(sb: SpannableStringBuilder) {
            if (sb.isNotEmpty() && sb[sb.length - 1] != '\n') sb.append("\n")
        }

        fun appendCenteredHeader(sb: SpannableStringBuilder, text: String) {
            ensureNewLine(sb)
            val start = sb.length
            sb.append(text)
            val end = sb.length
            sb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(1.15f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(headerColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        val sb = SpannableStringBuilder()
        val ranges = ArrayList<AyahRange>(ayas.size)

        var lastSura = -1

        for (a in ayas) {
            // show surah/bismillah when surah starts on this page
            if (a.suraNumber != lastSura && a.ayaNumber == 1) {
                val sName = suraMap[a.suraNumber]?.nameAr ?: "سورة ${a.suraNumber}"
                appendCenteredHeader(sb, sName)
                if (a.suraNumber != 9) appendCenteredHeader(sb, bismillah)
            }
            lastSura = a.suraNumber

            val start = sb.length
            sb.append(ayahWithMarker(a.textUthmani, a.ayaNumber))
            val end = sb.length

            ranges.add(
                AyahRange(
                    start = start,
                    end = end,
                    ayaId = a.id,
                    suraNumber = a.suraNumber,
                    ayaNumber = a.ayaNumber,
                    chapterId = a.chapterId,
                    page = a.page,
                    textUthmani = a.textUthmani,
                    textSimple = a.textSimple
                )
            )

            sb.append("  ")
        }

        val searchText = ayas.joinToString(" ") { it.textSimple }

        val ui = QuranPageUiModel(
            pageNumber = pageNo,
            surahNameAr = surahNameAr,
            chapterNameAr = chapterNameAr,
            content = sb,
            searchText = searchText
        )

        return PageModel(ui, ranges)
    }

    class VH(
        itemView: View,
        private val onAyahTapped: (ReadQuranPagesFragment.AyahSelection) -> Unit,
        private val onAyahLongPressed: (ReadQuranPagesFragment.AyahSelection, rawX: Float, rawY: Float) -> Unit,
        private val selectedBgColor: Int
    ) : RecyclerView.ViewHolder(itemView) {

        var boundPage: Int = -1

        private val txtSurahName: TextView = itemView.findViewById(R.id.txtSurahName)
        private val txtPageNo: TextView = itemView.findViewById(R.id.txtPageNo)
        private val txtChapterName: TextView = itemView.findViewById(R.id.txtChapterName)
        private val txtPageText: TextView = itemView.findViewById(R.id.txtPageText)

        private var boundModel: PageModel? = null

        fun bindLoading(pageNo: Int) {
            txtSurahName.text = ""
            txtChapterName.text = ""
            txtPageNo.text = pageNo.toString()
            txtPageText.text = "Loading…"
            boundModel = null
        }

        private fun selectionAtOffset(offset: Int): ReadQuranPagesFragment.AyahSelection? {
            val ranges = boundModel?.ranges ?: return null
            val hit = ranges.firstOrNull { offset in it.start until it.end }
                ?: ranges.lastOrNull { it.start <= offset }
                ?: return null

            return ReadQuranPagesFragment.AyahSelection(
                ayaId = hit.ayaId,
                suraNumber = hit.suraNumber,
                ayaNumber = hit.ayaNumber,
                chapterId = hit.chapterId,
                page = hit.page,
                textUthmani = hit.textUthmani,
                textSimple = hit.textSimple
            )
        }

        fun bind(model: PageModel, query: String?, selectedAyahId: Int?) {
            boundModel = model

            txtSurahName.text = model.ui.surahNameAr
            txtPageNo.text = model.ui.pageNumber.toString()
            txtChapterName.text = model.ui.chapterNameAr

            val sb = SpannableStringBuilder(model.ui.content)

            // query highlight
            if (!query.isNullOrEmpty() && !query.all { it.isDigit() || it in "٠١٢٣٤٥٦٧٨٩" }) {
                val color = ContextCompat.getColor(itemView.context, R.color.highlight_yellow)
                highlightInPlace(sb, query, color)
            }

            // selected highlight
            if (selectedAyahId != null) {
                val r = model.ranges.firstOrNull { it.ayaId == selectedAyahId }
                if (r != null) {
                    sb.setSpan(
                        BackgroundColorSpan(selectedBgColor),
                        r.start, r.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            txtPageText.text = sb

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                txtPageText.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }

            var didLongPress = false

            val detector = GestureDetector(
                itemView.context,
                object : GestureDetector.SimpleOnGestureListener() {

                    override fun onDown(e: MotionEvent): Boolean {
                        didLongPress = false
                        return true
                    }

                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (didLongPress) return true

                        val layout = txtPageText.layout ?: return false
                        val x = (e.x - txtPageText.totalPaddingLeft + txtPageText.scrollX).toInt()
                        val y = (e.y - txtPageText.totalPaddingTop + txtPageText.scrollY).toInt()
                        if (x < 0 || y < 0) return false

                        val line = layout.getLineForVertical(y)
                        val off = layout.getOffsetForHorizontal(line, x.toFloat())

                        val sel = selectionAtOffset(off) ?: return false
                        onAyahTapped(sel)
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        didLongPress = true

                        val layout = txtPageText.layout ?: return
                        val x = (e.x - txtPageText.totalPaddingLeft + txtPageText.scrollX).toInt()
                        val y = (e.y - txtPageText.totalPaddingTop + txtPageText.scrollY).toInt()
                        if (x < 0 || y < 0) return

                        val line = layout.getLineForVertical(y)
                        val off = layout.getOffsetForHorizontal(line, x.toFloat())

                        val sel = selectionAtOffset(off) ?: return

                        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                        // select first, then open menu near finger
                        onAyahTapped(sel)
                        onAyahLongPressed(sel, e.rawX, e.rawY)
                    }
                }
            )

            txtPageText.setOnTouchListener { _, ev ->
                detector.onTouchEvent(ev)
                true // consume so we fully control tap/long press
            }
        }
    }
}
