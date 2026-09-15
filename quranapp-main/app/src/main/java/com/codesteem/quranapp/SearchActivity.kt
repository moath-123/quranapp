package com.codesteem.quranapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codesteem.quranapp.Adapters.SearchAdapter
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.Repository.QuranRepository
import com.codesteem.quranapp.helper.ArabicNormalizer
import com.codesteem.quranapp.helper.ArabicNormalizer.extractPageNumber
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var db: QuranDatabase
    private lateinit var repo: QuranRepository
    private lateinit var adapter: SearchAdapter
    private lateinit var edtSearchBar: EditText
    private lateinit var mainText: TextView
    private lateinit var txtResultCount: TextView

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        txtResultCount = findViewById(R.id.txtSearchCount)
        txtResultCount.text = "Results: 0"


        val appBar = findViewById<View>(R.id.searchAppBar)

        edtSearchBar = findViewById(R.id.edtSearchBar)
        mainText = findViewById(R.id.mainText)

        mainText.text = "Search Quran"


        ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, insets ->
            val statusBarHeight =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top

            view.setPadding( view.paddingLeft,  statusBarHeight,  view.paddingRight,  view.paddingBottom)
            insets
        }


        db = QuranDatabase.getInstance(this)
        repo = QuranRepository(db.quranDao())

        val rv = findViewById<RecyclerView>(R.id.rvResults)

        adapter = SearchAdapter { row ->
            // Return selection to HomePageActivity
            val data = Intent().apply {
                putExtra("page", row.page)
                putExtra("suraNumber", row.suraNumber)
                putExtra("ayaNumber", row.ayaNumber)
                putExtra("ayaId", row.id)
                putExtra("textUthmani", row.textUthmani) // "text"
                putExtra("ayaText", row.textSimple)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        rv.addItemDecoration(divider)

        edtSearchBar.setOnEditorActionListener { _, actionId, event ->
            val isSearch =
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)


            if (!isSearch) return@setOnEditorActionListener false

            lifecycleScope.launch {
                val page = ArabicNormalizer.extractPageNumber(edtSearchBar.text.toString())
                if (page != null) {
                    val maxPage = withContext(Dispatchers.IO) { db.quranDao().getMaxPage() ?: 604 }
                    if (page in 1..maxPage) {
                        // optional: find a stable anchor ayah on that page
                        val anchor = withContext(Dispatchers.IO) { db.quranDao().getFirstAyahOnPage(page) }

                        val data = Intent().apply {
                            putExtra("page", page)
                            putExtra("suraNumber", anchor?.suraNumber ?: 1)
                            putExtra("ayaNumber", anchor?.ayaNumber ?: 1)
                            putExtra("ayaId", anchor?.id ?: -1)
                            putExtra("textUthmani", anchor?.textUthmani)
                            putExtra("ayaText", anchor?.textSimple)
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                        return@launch
                    } else {
                        Toast.makeText(this@SearchActivity, "Page must be 1 to $maxPage", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                // Not a page number → proceed with normal Arabic search (your existing debounce does this too)
                // You can either trigger it here or let the TextWatcher handle it.
            }
            true
        }


        edtSearchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString().orEmpty()

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(200) // debounce

                    // ✅ If user is typing ONLY a page number, don't run text search
                    val page = extractPageNumber(q)
                    val isOnlyDigits = q.trim().all { it.isDigit() || it in "٠١٢٣٤٥٦٧٨٩" }
                    if (page != null && isOnlyDigits) {
                        adapter.submit(emptyList())
                        return@launch
                    }

                    val results = withContext(Dispatchers.Default) {
                        repo.searchArabic(q)
                    }
                    adapter.submit(results)
                    if(results.size >0){
                        txtResultCount.visibility = View.VISIBLE
                        txtResultCount.text = "Quran Text: ${results.size} Results"
                    }else{
                        txtResultCount.visibility = View.GONE

                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        /* et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString().orEmpty()

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(200) // debounce
                    val results = withContext(Dispatchers.Default) {
                        repo.searchArabic(q)
                    }
                    adapter.submit(results)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })*/
    }
}
