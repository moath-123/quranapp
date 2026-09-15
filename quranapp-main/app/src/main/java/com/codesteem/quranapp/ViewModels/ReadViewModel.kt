package com.codesteem.quranapp.ViewModels


import androidx.lifecycle.*
import com.codesteem.quranapp.Repository.QuranRepository
import com.codesteem.quranapp.UIModels.AyahUiModel
import kotlinx.coroutines.launch

class ReadViewModel(private val repo: QuranRepository) : ViewModel() {

    private val _ayahs = MutableLiveData<List<AyahUiModel>>()
    val ayahs: LiveData<List<AyahUiModel>> = _ayahs

    private val _query = MutableLiveData<String?>()
    val query: LiveData<String?> = _query

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSurah(suraNumber: Int) {
        viewModelScope.launch {
            try {
                _error.value = null
                _ayahs.value = repo.getAyahsBySurah(suraNumber)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load surah"
            }
        }
    }

    fun loadFullQuran() {
        viewModelScope.launch {
            try {
                _error.value = null
                _ayahs.value = repo.getAllAyahs()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load Quran"
            }
        }
    }


    fun setSearchQuery(q: String?) {
        _query.value = q?.trim().takeUnless { it.isNullOrEmpty() }
    }
}
