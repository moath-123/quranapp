package com.codesteem.quranapp.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.codesteem.quranapp.Adapters.QuranPageAdapter
import com.codesteem.quranapp.DataEntities.BookmarkEntity
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ReadQuranPagesFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: QuranPageAdapter

    private var startPage: Int = 1
    private var highlightAyaId: Int = -1
    private var maxPage: Int = 604

    // current selected ayah
    private var selected: AyahSelection? = null

    private var ayahPopup: PopupWindow? = null

    data class AyahSelection(
        val ayaId: Int,
        val suraNumber: Int,
        val ayaNumber: Int,
        val chapterId: Int,
        val page: Int,
        val textUthmani: String,
        val textSimple: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startPage = arguments?.getInt("page", 1) ?: 1
        highlightAyaId = arguments?.getInt("ayaId", -1) ?: -1
    }

    override fun onDestroyView() {
        ayahPopup?.dismiss()
        ayahPopup = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_read_quran_pages, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.readQuranViewPager)
        viewPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        viewPager.offscreenPageLimit = 1
        viewPager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        viewPager.isUserInputEnabled = true

        val dao = QuranDatabase.getInstance(requireContext()).quranDao()

        viewLifecycleOwner.lifecycleScope.launch {
            maxPage = withContext(Dispatchers.IO) { dao.getMaxPage() ?: 604 }

            val suraMap = withContext(Dispatchers.IO) {
                dao.getAllSuras().associateBy { it.suraNumber }
            }

            val headerColor = ContextCompat.getColor(requireContext(), R.color.quran_serial_no_color)
            val selectedColor = ContextCompat.getColor(requireContext(), R.color.highlight_yellow)

            adapter = QuranPageAdapter(
                dao = dao,
                scope = viewLifecycleOwner.lifecycleScope,
                maxPage = maxPage,
                suraMap = suraMap,
                headerColor = headerColor,
                selectedBgColor = selectedColor,

                // tap = select highlight
                onAyahTapped = { selection ->
                    selected = selection
                    adapter.setSelectedAyah(selection.ayaId, selection.page)
                },

                // long press = show popup menu at finger location
                onAyahLongPressed = { selection, rawX, rawY ->
                    selected = selection
                    adapter.setSelectedAyah(selection.ayaId, selection.page)
                    showAyahPopup(selection, rawX, rawY)
                }
            )

            viewPager.adapter = adapter

            // jump to start page
            val safePage = startPage.coerceIn(1, maxPage)
            viewPager.post { viewPager.setCurrentItem(safePage - 1, false) }

            // initial selection from search
            if (highlightAyaId > 0) {
                val aya = withContext(Dispatchers.IO) { dao.getAyaById(highlightAyaId) }
                if (aya != null) {
                    val sel = AyahSelection(
                        ayaId = aya.id,
                        suraNumber = aya.suraNumber,
                        ayaNumber = aya.ayaNumber,
                        chapterId = aya.chapterId,
                        page = aya.page,
                        textUthmani = aya.textUthmani,
                        textSimple = aya.textSimple
                    )
                    selected = sel
                    adapter.setSelectedAyah(sel.ayaId, sel.page)
                }
            }
        }
    }

    private fun showAyahPopup(sel: AyahSelection, rawX: Float, rawY: Float) {
        ayahPopup?.dismiss()

        lifecycleScope.launch {
            val dao = QuranDatabase.getInstance(requireContext()).quranDao()
            val isMarked = withContext(Dispatchers.IO) { dao.isBookmarked(sel.ayaId) }

            val ctx = requireContext()

            // menu container
            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }

            fun addItem(title: String, onClick: () -> Unit) {
                val tv = TextView(ctx).apply {
                    text = title
                    setTextColor(ContextCompat.getColor(ctx, R.color.quran_text_color))
                    textSize = 14f
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setOnClickListener {
                        ayahPopup?.dismiss()
                        onClick()
                    }
                }
                box.addView(tv)
            }

            addItem(if (isMarked) "Remove Bookmark" else "Bookmark Ayah") {
                lifecycleScope.launch {
                    toggleBookmark(sel)
                }
            }

            addItem("Copy (Uthmani)") {
                copyToClipboard("Ayah (Uthmani)", sel.textUthmani)
            }

           /* addItem("Copy (Simple)") {
                copyToClipboard("Ayah (Simple)", sel.textSimple)
            }*/

            val popup = PopupWindow(
                box,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                elevation = dp(8).toFloat()
            }

            ayahPopup = popup

            // show near finger (slightly above)
            val decor = requireActivity().window.decorView
            val x = rawX.roundToInt()
            val y = (rawY - dp(12)).roundToInt()

            popup.showAtLocation(decor, Gravity.NO_GRAVITY, x, y)
        }
    }

    private suspend fun toggleBookmark(sel: AyahSelection) {
        val dao = QuranDatabase.getInstance(requireContext()).quranDao()

        val isMarked = withContext(Dispatchers.IO) { dao.isBookmarked(sel.ayaId) }
        if (isMarked) {
            withContext(Dispatchers.IO) { dao.removeBookmark(sel.ayaId) }
            Toast.makeText(requireContext(), "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            val b = BookmarkEntity(
                ayaId = sel.ayaId,
                suraNumber = sel.suraNumber,
                ayaNumber = sel.ayaNumber,
                chapterId = sel.chapterId,
                page = sel.page,
                textUthmani = sel.textUthmani,
                textSimple = sel.textSimple
            )

            val id = withContext(Dispatchers.IO) { dao.addBookmark(b) }
            if (id == -1L) {
                Toast.makeText(requireContext(), "Already bookmarked", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Bookmarked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "Ayah Copied", Toast.LENGTH_SHORT).show()
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).roundToInt()
    }
}
