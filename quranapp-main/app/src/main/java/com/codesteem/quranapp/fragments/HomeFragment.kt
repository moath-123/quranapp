package com.codesteem.quranapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.Adapters.ChapterAdapter
import com.codesteem.quranapp.Adapters.SuraAdapter
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.R
import com.codesteem.quranapp.Repository.QuranRepository
import com.codesteem.quranapp.ViewModels.HomeViewModel
import com.codesteem.quranapp.ViewModels.HomeViewModelFactory

class HomeFragment : Fragment() {

    private var optionLayout: LinearLayout? = null
    private var txtSurahList: TextView? = null
    private var txtJuzList: TextView? = null
    private var txtBookMarkList: TextView? = null

    private lateinit var rvSuras: RecyclerView
    private lateinit var viewModel: HomeViewModel

    private lateinit var suraAdapter: SuraAdapter
    private lateinit var chapterAdapter: ChapterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        optionLayout = view.findViewById(R.id.optionLayout)
        txtSurahList = view.findViewById(R.id.txtSurahList)
        txtJuzList = view.findViewById(R.id.txtJuzList)
        txtBookMarkList = view.findViewById(R.id.txtBookMarkList)

        rvSuras = view.findViewById(R.id.rvSuras)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupViewModel()
        observeData()
        setupTabClicks()

        // default list = Surah
        selectSurahTabUI()
        rvSuras.adapter = suraAdapter
        viewModel.loadSuras()
    }

    private fun setupRecycler() {

        suraAdapter = SuraAdapter { sura ->
            loadFragment(ReadFragment.newInstanceSurah(sura.suraNumber))
        }

        chapterAdapter = ChapterAdapter { chapter ->
             loadFragment( ReadFragment.newInstanceJuz(chapter.chapterId))
        }

        rvSuras.layoutManager = LinearLayoutManager(requireContext())
        rvSuras.adapter = suraAdapter
    }

    private fun setupViewModel() {
        val dao = QuranDatabase.getInstance(requireContext()).quranDao()
        val repo = QuranRepository(dao)
        val factory = HomeViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }

    private fun observeData() {

        // Surah list observer
        viewModel.suras.observe(viewLifecycleOwner) { list ->
            if (rvSuras.adapter != suraAdapter) return@observe
            suraAdapter.submitList(list)
        }

        // Chapter/Juz list observer
        viewModel.chapters.observe(viewLifecycleOwner) { list ->
            if (rvSuras.adapter != chapterAdapter) return@observe
            chapterAdapter.submitList(list)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTabClicks() {

        txtSurahList?.setOnClickListener {
            selectSurahTabUI()
            rvSuras.adapter = suraAdapter
            viewModel.loadSuras()
        }

        txtJuzList?.setOnClickListener {
            selectJuzTabUI()
            rvSuras.adapter = chapterAdapter
            viewModel.loadChapters()
        }

        txtBookMarkList?.setOnClickListener {
            selectBookmarkTabUI()
            Toast.makeText(requireContext(), "Bookmark list next", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- UI SELECTORS ----------------

    private fun selectSurahTabUI() {
        txtSurahList?.setBackgroundResource(R.drawable.custom__left_rounder_button_)
        txtSurahList?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white_color))

        txtJuzList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtBookMarkList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtJuzList?.background = null
        txtBookMarkList?.background = null
    }

    private fun selectJuzTabUI() {
        txtJuzList?.setBackgroundResource(R.drawable.custom_center_rounder_button_)
        txtJuzList?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white_color))

        txtSurahList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtBookMarkList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtSurahList?.background = null
        txtBookMarkList?.background = null
    }

    private fun selectBookmarkTabUI() {
        txtBookMarkList?.setBackgroundResource(R.drawable.custom_right_rounder_button_)
        txtBookMarkList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.white_color
            )
        )

        txtSurahList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtJuzList?.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.quran_text_color
            )
        )
        txtSurahList?.background = null
        txtJuzList?.background = null
    }

    private fun loadFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
