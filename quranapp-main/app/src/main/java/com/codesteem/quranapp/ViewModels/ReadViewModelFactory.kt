package com.codesteem.quranapp.ViewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codesteem.quranapp.Repository.QuranRepository

class ReadViewModelFactory(private val repo: QuranRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReadViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
