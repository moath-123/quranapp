package com.codesteem.quranapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.Repository.QuranImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        startApp()

    }


    private fun startApp() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = QuranDatabase.getInstance(this@SplashScreenActivity)
                QuranImporter.importIfNeeded(
                    this@SplashScreenActivity,
                    db
                )
            }

            // Optional: keep splash visible for UX
            delay(2000)

            startActivity(Intent(this@SplashScreenActivity, HomePageActivity::class.java))
            finish()
        }
    }

}