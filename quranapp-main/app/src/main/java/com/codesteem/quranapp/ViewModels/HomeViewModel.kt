package com.codesteem.quranapp.ViewModels


import androidx.lifecycle.*
import com.codesteem.quranapp.Repository.QuranRepository
import com.codesteem.quranapp.UIModels.ChapterUiModel
import com.codesteem.quranapp.UIModels.SuraUiModel
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: QuranRepository) : ViewModel() {

    private val _suras = MutableLiveData<List<SuraUiModel>>()
    val suras: LiveData<List<SuraUiModel>> = _suras

    private val _chapters = MutableLiveData<List<ChapterUiModel>>()
    val chapters: LiveData<List<ChapterUiModel>> = _chapters

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSuras() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _suras.value = repo.getAllSuras()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadChapters() {
        viewModelScope.launch {
            try {
                _error.value = null
                _chapters.value = repo.getAllChapters()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load chapters"
            }
        }
    }

}
