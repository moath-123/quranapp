package com.codesteem.quranapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.codesteem.quranapp.fragments.HomeFragment
import com.codesteem.quranapp.fragments.ReadQuranPagesFragment
import com.codesteem.quranapp.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomePageActivity : AppCompatActivity() {

    var bottomNavigationView: BottomNavigationView ?= null
    var btnSearchText: ImageView ?= null

    private val searchLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = res.data ?: return@registerForActivityResult

            val page = data.getIntExtra("page", 1)
            val suraNumber = data.getIntExtra("suraNumber", 1)
            val ayaNumber = data.getIntExtra("ayaNumber", 1)
            val ayaId = data.getIntExtra("ayaId", -1)
            val textUthmani = data.getStringExtra("textUthmani")
            val ayaText = data.getStringExtra("ayaText")

            openReaderAtPage(page, suraNumber, ayaNumber, ayaId,textUthmani, ayaText)

        }

    private fun openReaderAtPage(
        page: Int,
        suraNumber: Int,
        ayaNumber: Int,
        ayaId: Int,
        textUthmani: String?,
        ayaText: String?
    ) {
        // ✅ create fragment with args
        val frag = ReadQuranPagesFragment().apply {
            arguments = Bundle().apply {
                putInt("page", page)
                putInt("suraNumber", suraNumber)
                putInt("ayaNumber", ayaNumber)
                putInt("ayaId", ayaId)
                putString("textUthmani", textUthmani)
                putString("ayaText", ayaText)
            }
        }

        //bottomNavigationView?.menu?.findItem(R.id.nav_read_quran)?.isChecked = true
        loadFragment(frag)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        btnSearchText = findViewById(R.id.btnSearchText)

        val appBar = findViewById<View>(R.id.appBar)

        ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, insets ->
            val statusBarHeight =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top

            view.setPadding( view.paddingLeft,  statusBarHeight,  view.paddingRight,  view.paddingBottom)
            insets
        }

        bottomNavigationView = findViewById(R.id.bottom_nav)
         loadFragment(HomeFragment())

        bottomNavigationView?.setOnItemSelectedListener {
            when(it.itemId){
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_bookmark_quran -> loadFragment(ReadQuranPagesFragment())//Toast.makeText(applicationContext , "Bookmarks Later" , Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(applicationContext , "Settings Later" , Toast.LENGTH_SHORT).show()
            }
            true
        }
        btnSearchText?.setOnClickListener {
            searchLauncher.launch(Intent(this, SearchActivity::class.java))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


}