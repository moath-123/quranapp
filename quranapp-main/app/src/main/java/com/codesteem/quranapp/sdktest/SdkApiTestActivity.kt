package com.codesteem.quranapp.sdktest

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.R
import com.codesteem.quransdk.QuranSdk
import com.codesteem.quransdk.api.QuranApi
import com.codesteem.quransdk.api.model.Ayah
import com.codesteem.quransdk.api.model.Juz
import com.codesteem.quransdk.api.model.SearchResult
import com.codesteem.quransdk.api.model.Surah
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SdkApiTestActivity : AppCompatActivity() {

    private lateinit var quranApi: QuranApi
    private lateinit var bookmarkStore: ClientBookmarkStore
    private lateinit var resultAdapter: SdkTestResultAdapter
    private lateinit var bookmarkAdapter: SdkTestResultAdapter
    private lateinit var apiAdapter: SdkApiActionAdapter

    private lateinit var viewStatusDot: View
    private lateinit var txtSdkStatus: TextView
    private lateinit var chipSurahCount: TextView
    private lateinit var chipJuzCount: TextView
    private lateinit var chipMaxPage: TextView
    private lateinit var txtSelectedApi: TextView
    private lateinit var txtSelectedApiDesc: TextView
    private lateinit var txtResultCount: TextView
    private lateinit var txtLastApiCall: TextView
    private lateinit var txtEmptyResults: TextView
    private lateinit var txtEmptyBookmarks: TextView
    private lateinit var txtBookmarkCount: TextView
    private lateinit var rvSdkResults: RecyclerView
    private lateinit var rvBookmarks: RecyclerView
    private lateinit var scrollMain: androidx.core.widget.NestedScrollView
    private lateinit var panelApiExplorer: View
    private lateinit var panelBookmarks: View
    private lateinit var tabApiExplorer: TextView
    private lateinit var tabBookmarks: TextView
    private lateinit var cardResults: View
    private lateinit var layoutSdkInput: TextInputLayout
    private lateinit var edtInput: TextInputEditText
    private lateinit var progress: ProgressBar

    private var isInitialized = false
    private var selectedAction: SdkApiAction = SdkApiCatalog.all.first()
    private var showingBookmarksTab = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sdk_api_test)

        quranApi = QuranSdk.create(this)
        bookmarkStore = ClientBookmarkStore(this)
        bindViews()
        setupTabs()
        setupApiList()
        setupResults()
        setupBookmarksList()
        setupActions()
        applySelectedApi(selectedAction)
        refreshBookmarkPanel()
    }

    private fun bindViews() {
        viewStatusDot = findViewById(R.id.viewStatusDot)
        txtSdkStatus = findViewById(R.id.txtSdkStatus)
        chipSurahCount = findViewById(R.id.chipSurahCount)
        chipJuzCount = findViewById(R.id.chipJuzCount)
        chipMaxPage = findViewById(R.id.chipMaxPage)
        txtSelectedApi = findViewById(R.id.txtSelectedApi)
        txtSelectedApiDesc = findViewById(R.id.txtSelectedApiDesc)
        txtResultCount = findViewById(R.id.txtResultCount)
        txtLastApiCall = findViewById(R.id.txtLastApiCall)
        txtEmptyResults = findViewById(R.id.txtEmptyResults)
        txtEmptyBookmarks = findViewById(R.id.txtEmptyBookmarks)
        txtBookmarkCount = findViewById(R.id.txtBookmarkCount)
        rvSdkResults = findViewById(R.id.rvSdkResults)
        rvBookmarks = findViewById(R.id.rvBookmarks)
        scrollMain = findViewById(R.id.panelApiExplorer)
        panelApiExplorer = findViewById(R.id.panelApiExplorer)
        panelBookmarks = findViewById(R.id.panelBookmarks)
        tabApiExplorer = findViewById(R.id.tabApiExplorer)
        tabBookmarks = findViewById(R.id.tabBookmarks)
        cardResults = findViewById(R.id.cardResults)
        layoutSdkInput = findViewById(R.id.layoutSdkInput)
        edtInput = findViewById(R.id.edtSdkInput)
        progress = findViewById(R.id.progressSdkTest)
    }

    private fun setupTabs() {
        tabApiExplorer.setOnClickListener { showApiTab() }
        tabBookmarks.setOnClickListener { showBookmarksTab() }
    }

    private fun showApiTab() {
        showingBookmarksTab = false
        panelApiExplorer.visibility = View.VISIBLE
        panelBookmarks.visibility = View.GONE
        tabApiExplorer.setBackgroundResource(R.drawable.sdk_tab_selected_bg)
        tabApiExplorer.setTextColor(ContextCompat.getColor(this, R.color.white_color))
        tabBookmarks.setBackgroundResource(R.drawable.sdk_tab_unselected_bg)
        tabBookmarks.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tabBookmarks.alpha = 0.7f
        tabApiExplorer.alpha = 1f
    }

    private fun showBookmarksTab() {
        showingBookmarksTab = true
        panelApiExplorer.visibility = View.GONE
        panelBookmarks.visibility = View.VISIBLE
        tabBookmarks.setBackgroundResource(R.drawable.sdk_tab_selected_bg)
        tabBookmarks.setTextColor(ContextCompat.getColor(this, R.color.white_color))
        tabBookmarks.alpha = 1f
        tabApiExplorer.setBackgroundResource(R.drawable.sdk_tab_unselected_bg)
        tabApiExplorer.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tabApiExplorer.alpha = 0.7f
        refreshBookmarkPanel()
    }

    private fun setupApiList() {
        apiAdapter = SdkApiActionAdapter { action ->
            selectedAction = action
            applySelectedApi(action)
        }
        findViewById<RecyclerView>(R.id.rvApiList).apply {
            layoutManager = LinearLayoutManager(this@SdkApiTestActivity)
            adapter = apiAdapter
        }
    }

    private fun setupResults() {
        resultAdapter = SdkTestResultAdapter(bookmarkStore) {
            refreshBookmarkPanel()
            resultAdapter.notifyDataSetChanged()
        }
        rvSdkResults.apply {
            layoutManager = LinearLayoutManager(this@SdkApiTestActivity)
            adapter = resultAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupBookmarksList() {
        bookmarkAdapter = SdkTestResultAdapter(bookmarkStore) {
            refreshBookmarkPanel()
        }
        rvBookmarks.apply {
            layoutManager = LinearLayoutManager(this@SdkApiTestActivity)
            adapter = bookmarkAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun refreshBookmarkPanel() {
        val bookmarks = bookmarkStore.getAll()
        txtBookmarkCount.text = "${bookmarks.size} saved"
        tabBookmarks.text = "Client Bookmarks (${bookmarks.size})"

        if (bookmarks.isEmpty()) {
            txtEmptyBookmarks.visibility = View.VISIBLE
            rvBookmarks.visibility = View.GONE
            bookmarkAdapter.submit(emptyList())
        } else {
            txtEmptyBookmarks.visibility = View.GONE
            rvBookmarks.visibility = View.VISIBLE
            bookmarkAdapter.submit(
                bookmarks.map { b ->
                    SdkTestRow(
                        apiMethod = "client storage",
                        title = "${b.surahName.ifBlank { "Surah" }} ${b.surahNumber}:${b.ayahNumber}",
                        subtitle = "Page ${b.pageNumber} · Juz ${b.juzNumber} · ID ${b.ayahId}",
                        body = b.textUthmani,
                        isRtlBody = true,
                        bookmark = b
                    )
                }
            )
        }
    }

    private fun showResults(rows: List<SdkTestRow>) {
        txtResultCount.text = "${rows.size} items"
        if (rows.isEmpty()) {
            txtEmptyResults.visibility = View.VISIBLE
            txtEmptyResults.text = "API returned empty list."
            rvSdkResults.visibility = View.GONE
            resultAdapter.submit(emptyList())
        } else {
            txtEmptyResults.visibility = View.GONE
            rvSdkResults.visibility = View.VISIBLE
            resultAdapter.submit(rows)
        }
        scrollMain.post { scrollMain.smoothScrollTo(0, cardResults.top) }
    }

    private fun setupActions() {
        findViewById<MaterialButton>(R.id.btnInitialize).setOnClickListener {
            runSdkAction("initialize()") {
                quranApi.initialize()
                isInitialized = true
                updateStatusUi(true)
                refreshStats()
                showToast("SDK connected")
            }
        }
        findViewById<MaterialButton>(R.id.btnRunApi).setOnClickListener { runSelectedApi() }
    }

    private fun applySelectedApi(action: SdkApiAction) {
        txtSelectedApi.text = action.methodName
        txtSelectedApiDesc.text = action.description
        when (action.inputType) {
            SdkInputType.NONE -> layoutSdkInput.visibility = View.GONE
            SdkInputType.SEARCH -> {
                layoutSdkInput.visibility = View.VISIBLE
                layoutSdkInput.hint = action.inputHint
                edtInput.inputType = InputType.TYPE_CLASS_TEXT
            }
            SdkInputType.PAGE, SdkInputType.SURAH, SdkInputType.JUZ, SdkInputType.AYAH_ID -> {
                layoutSdkInput.visibility = View.VISIBLE
                layoutSdkInput.hint = action.inputHint
                edtInput.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }
    }

    private fun runSelectedApi() {
        if (selectedAction.requiresInit && !isInitialized) {
            showToast("Call initialize() first")
            return
        }
        when (selectedAction.methodName) {
            "getSurahs()" -> runSdkAction("getSurahs()") { showSurahs(quranApi.getSurahs()) }
            "getJuzList()" -> runSdkAction("getJuzList()") { showJuzList(quranApi.getJuzList()) }
            "search(query)" -> {
                val query = edtInput.text?.toString().orEmpty().trim()
                if (query.isEmpty()) { showToast("Enter search text"); return }
                runSdkAction("search(\"$query\")") { showSearchResults(quranApi.search(query), query) }
            }
            "getAyahsByPage(page)" -> {
                val page = readIntInput() ?: return
                runSdkAction("getAyahsByPage($page)") { showAyahs(quranApi.getAyahsByPage(page), "Page $page") }
            }
            "getAyahsBySurah(surah)" -> {
                val surah = readIntInput() ?: return
                runSdkAction("getAyahsBySurah($surah)") { showAyahs(quranApi.getAyahsBySurah(surah), "Surah $surah") }
            }
            "getAyahsByJuz(juz)" -> {
                val juz = readIntInput() ?: return
                runSdkAction("getAyahsByJuz($juz)") { showAyahs(quranApi.getAyahsByJuz(juz), "Juz $juz") }
            }
            "getAyahById(id)" -> {
                val id = readIntInput() ?: return
                runSdkAction("getAyahById($id)") {
                    val ayah = quranApi.getAyahById(id)
                    if (ayah == null) showEmpty("No ayah found for ID $id")
                    else showAyahs(listOf(ayah), "Ayah ID $id")
                }
            }
            "getFirstAyahOnPage(page)" -> {
                val page = readIntInput() ?: return
                runSdkAction("getFirstAyahOnPage($page)") {
                    val ayah = quranApi.getFirstAyahOnPage(page)
                    if (ayah == null) showEmpty("No ayah found on page $page")
                    else showAyahs(listOf(ayah), "First ayah on page $page")
                }
            }
            "getMaxPage()" -> runSdkAction("getMaxPage()") {
                showScalarResult("getMaxPage()", "Total Pages", quranApi.getMaxPage().toString())
            }
        }
    }

    private fun readIntInput(): Int? {
        val value = edtInput.text?.toString()?.trim()?.toIntOrNull()
        if (value == null) showToast("Enter a valid number")
        return value
    }

    private fun runSdkAction(methodLabel: String, block: suspend () -> Unit) {
        txtLastApiCall.text = "Last call: $methodLabel"
        setLoading(true)
        lifecycleScope.launch {
            try { block() }
            catch (e: Exception) {
                txtSdkStatus.text = "Error: ${e.message ?: "Unknown"}"
                showToast(e.message ?: "API failed")
            } finally { setLoading(false) }
        }
    }

    private fun updateStatusUi(ready: Boolean) {
        viewStatusDot.setBackgroundResource(if (ready) R.drawable.sdk_status_dot_on else R.drawable.sdk_status_dot_off)
        txtSdkStatus.text = if (ready) "SDK connected & ready" else "Not connected"
    }

    private suspend fun refreshStats() {
        chipSurahCount.text = "Surahs\n${quranApi.getSurahs().size}"
        chipJuzCount.text = "Juz\n${quranApi.getJuzList().size}"
        chipMaxPage.text = "Pages\n${quranApi.getMaxPage()}"
    }

    private fun ayahToBookmark(ayah: Ayah, surahName: String = "") = ClientBookmark(
        ayahId = ayah.id,
        surahNumber = ayah.surahNumber,
        ayahNumber = ayah.ayahNumber,
        pageNumber = ayah.pageNumber,
        juzNumber = ayah.juzNumber,
        textUthmani = ayah.textUthmani,
        surahName = surahName
    )

    private fun searchToBookmark(r: SearchResult) = ClientBookmark(
        ayahId = r.ayahId,
        surahNumber = r.surahNumber,
        ayahNumber = r.ayahNumber,
        pageNumber = r.pageNumber,
        juzNumber = r.juzNumber,
        textUthmani = r.textUthmani,
        surahName = r.surahNameEn
    )

    private fun showSurahs(surahs: List<Surah>) {
        showResults(surahs.map {
            SdkTestRow(
                apiMethod = "getSurahs()",
                title = "${it.number}. ${it.nameEn}",
                subtitle = "${it.nameAr} · ${it.ayahCount} ayahs · page ${it.startPage}",
                body = "number=${it.number}, ayahCount=${it.ayahCount}, startPage=${it.startPage}"
            )
        })
    }

    private fun showJuzList(juzList: List<Juz>) {
        showResults(juzList.map {
            SdkTestRow(
                apiMethod = "getJuzList()",
                title = "Juz ${it.number}: ${it.nameEn}",
                subtitle = "${it.nameAr} · starts page ${it.startPage}",
                body = "number=${it.number}, startPage=${it.startPage}"
            )
        })
    }

    private fun showSearchResults(results: List<SearchResult>, query: String) {
        showResults(results.map {
            SdkTestRow(
                apiMethod = "search(\"$query\")",
                title = "${it.surahNameEn} ${it.surahNumber}:${it.ayahNumber}",
                subtitle = "Page ${it.pageNumber} · Juz ${it.juzNumber} · ID ${it.ayahId}",
                body = it.textUthmani,
                isRtlBody = true,
                bookmark = searchToBookmark(it)
            )
        })
    }

    private fun showAyahs(ayahs: List<Ayah>, label: String) {
        val method = selectedAction.methodName
        showResults(ayahs.map {
            SdkTestRow(
                apiMethod = method,
                title = "$label — Surah ${it.surahNumber}:${it.ayahNumber}",
                subtitle = "Page ${it.pageNumber} · Juz ${it.juzNumber} · ID ${it.id}",
                body = it.textUthmani,
                isRtlBody = true,
                bookmark = ayahToBookmark(it)
            )
        })
    }

    private fun showScalarResult(method: String, title: String, value: String) {
        showResults(listOf(SdkTestRow(method, title, "Scalar response", value)))
    }

    private fun showEmpty(message: String) {
        txtResultCount.text = "0 items"
        txtEmptyResults.visibility = View.VISIBLE
        txtEmptyResults.text = message
        rvSdkResults.visibility = View.GONE
        resultAdapter.submit(emptyList())
        scrollMain.post { scrollMain.smoothScrollTo(0, cardResults.top) }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
