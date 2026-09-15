package com.codesteem.quranapp.ViewModels


import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.codesteem.quranapp.QuranDB.QuranDatabase
import com.codesteem.quranapp.R
import com.codesteem.quranapp.Repository.QuranRepository
import com.codesteem.quranapp.UIModels.QuranFlow
import com.codesteem.quranapp.UIModels.QuranPageUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranPagesViewModel(app: Application) : AndroidViewModel(app) {

    private val _previewFlow = MutableLiveData<QuranFlow>()
    val previewFlow: LiveData<QuranFlow> = _previewFlow


    private val _flow = MutableLiveData<QuranFlow>()
    val flow: LiveData<QuranFlow> = _flow


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPages() {
        viewModelScope.launch {
            try {
                _error.value = null
                val dao = QuranDatabase.getInstance(getApplication()).quranDao()
                val repo = QuranRepository(dao)
                val green = ContextCompat.getColor(getApplication(), R.color.bottom_nav_icon_color)

                // 1) Show immediate page(s) quickly
                val preview = withContext(Dispatchers.IO) {
                    repo.buildPreviewFlow(listOf(1,2,3), green)
                }
                _previewFlow.value = preview

                // 2) Build flow in background (slower)
                val flow = withContext(Dispatchers.Default) {
                    repo.buildQuranFlow(green)
                }
                _flow.value = flow

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load pages"
            }
        }
    }
}
