package com.codesteem.quransdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.codesteem.quransdk.api.QuranApi
import com.codesteem.quransdk.api.QuranPageListener
import kotlinx.coroutines.launch

/**
 * Drop-in Fragment that hosts [QuranPageView].
 * Set [quranApi] before the fragment is shown.
 */
class QuranPageFragment : Fragment() {

    var quranApi: QuranApi? = null
    var pageListener: QuranPageListener? = null

    private var pageView: QuranPageView? = null
    private var pendingPage: Int? = null
    private var pendingAyahId: Int? = null
    private var pendingAyahPage: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = QuranPageView(requireContext())
        pageView = view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val api = quranApi ?: return
        val pv = pageView ?: return

        pv.setPageListener(pageListener)
        pv.bind(api)

        pendingPage?.let { page ->
            viewLifecycleOwner.lifecycleScope.launch {
                pv.goToPage(page)
            }
            pendingPage = null
        }
        pendingAyahId?.let { id ->
            pv.highlightAyah(id, pendingAyahPage ?: 1)
            pendingAyahId = null
            pendingAyahPage = null
        }
    }

    fun goToPage(page: Int) {
        pageView?.goToPage(page) ?: run { pendingPage = page }
    }

    fun highlightAyah(ayahId: Int, page: Int) {
        pageView?.highlightAyah(ayahId, page) ?: run {
            pendingAyahId = ayahId
            pendingAyahPage = page
        }
    }

    companion object {
        fun newInstance(): QuranPageFragment = QuranPageFragment()
    }
}
