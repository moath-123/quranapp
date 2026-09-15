package com.codesteem.quransdk.internal.ui

import android.graphics.text.LineBreaker
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quransdk.R
import com.codesteem.quransdk.api.QuranPageListener
import com.codesteem.quransdk.api.model.Ayah
import com.codesteem.quransdk.internal.QuranApiImpl
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class QuranPageAdapter(
    private val api: QuranApiImpl,
    private val scope: CoroutineScope,
    private val maxPage: Int,
    private val headerColor: Int,
    private val selectedColor: Int,
    private var listener: QuranPageListener?
) : RecyclerView.Adapter<QuranPageAdapter.VH>() {

    private val cache = HashMap<Int, QuranPageContent>()
    private val inFlight = HashSet<Int>()
    private var selectedAyahId: Int? = null

    fun setListener(listener: QuranPageListener?) {
        this.listener = listener
    }

    fun setSelectedAyah(ayahId: Int?, page: Int) {
        val oldPage = selectedAyahId?.let { id ->
            cache.entries.firstOrNull { e -> e.value.ayahRanges.any { r -> r.ayah.id == id } }?.key
        }
        selectedAyahId = ayahId
        oldPage?.let { if (it in 1..maxPage) notifyItemChanged(it - 1) }
        if (page in 1..maxPage) notifyItemChanged(page - 1)
    }

    override fun getItemCount(): Int = maxPage

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.quransdk_page_item, parent, false)
        return VH(v, headerColor, selectedColor) { ayah, x, y, isLong ->
            val model = ayah.toAyah()
            if (isLong) listener?.onAyahLongPressed(model, x, y)
            else listener?.onAyahTapped(model)
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pageNo = position + 1
        holder.boundPage = pageNo

        cache[pageNo]?.let {
            holder.bind(it, selectedAyahId, selectedColor)
            return
        }

        holder.bindLoading(pageNo)
        if (inFlight.contains(pageNo)) return
        inFlight.add(pageNo)

        scope.launch {
            val model = withContext(Dispatchers.IO) {
                val dao = api.getDao()
                val suraMap = dao.getAllSuras().associateBy { it.suraNumber }
                QuranPageBuilder.buildPage(dao, suraMap, pageNo, headerColor)
            }
            cache[pageNo] = model
            inFlight.remove(pageNo)
            if (holder.boundPage == pageNo) {
                holder.bind(model, selectedAyahId, selectedColor)
            }
        }
    }

    private fun AyaEntity.toAyah() = Ayah(
        id = id,
        surahNumber = suraNumber,
        ayahNumber = ayaNumber,
        juzNumber = chapterId,
        pageNumber = page,
        textUthmani = textUthmani,
        textSimple = textSimple,
        lineStart = lineStart,
        lineEnd = lineEnd
    )

    class VH(
        itemView: View,
        private val headerColor: Int,
        private val selectedColor: Int,
        private val onAyahEvent: (AyaEntity, Float, Float, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        var boundPage: Int = -1
        private var boundContent: QuranPageContent? = null

        private val txtSurahName: TextView = itemView.findViewById(R.id.txtSurahName)
        private val txtPageNo: TextView = itemView.findViewById(R.id.txtPageNo)
        private val txtChapterName: TextView = itemView.findViewById(R.id.txtChapterName)
        private val txtPageText: TextView = itemView.findViewById(R.id.txtPageText)

        fun bindLoading(pageNo: Int) {
            txtSurahName.text = ""
            txtChapterName.text = ""
            txtPageNo.text = pageNo.toString()
            txtPageText.text = "…"
            boundContent = null
        }

        fun bind(content: QuranPageContent, selectedAyahId: Int?, selectedColor: Int) {
            boundContent = content
            txtSurahName.text = content.surahNameAr
            txtPageNo.text = content.pageNumber.toString()
            txtChapterName.text = content.juzNameAr

            val sb = SpannableStringBuilder(content.content)
            if (selectedAyahId != null) {
                content.ayahRanges.firstOrNull { it.ayah.id == selectedAyahId }?.let { r ->
                    sb.setSpan(
                        BackgroundColorSpan(selectedColor),
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
                        hitTest(e)?.let { onAyahEvent(it, e.rawX, e.rawY, false) }
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        didLongPress = true
                        hitTest(e)?.let {
                            itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onAyahEvent(it, e.rawX, e.rawY, true)
                        }
                    }
                }
            )
            txtPageText.setOnTouchListener { _, ev ->
                detector.onTouchEvent(ev)
                true
            }
        }

        private fun hitTest(e: MotionEvent): AyaEntity? {
            val layout = txtPageText.layout ?: return null
            val x = (e.x - txtPageText.totalPaddingLeft + txtPageText.scrollX).toInt()
            val y = (e.y - txtPageText.totalPaddingTop + txtPageText.scrollY).toInt()
            if (x < 0 || y < 0) return null
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val ranges = boundContent?.ayahRanges ?: return null
            return ranges.firstOrNull { off in it.start until it.end }?.ayah
                ?: ranges.lastOrNull { it.start <= off }?.ayah
        }
    }
}
