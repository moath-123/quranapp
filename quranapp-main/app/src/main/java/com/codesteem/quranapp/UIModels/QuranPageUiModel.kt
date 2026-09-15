package com.codesteem.quranapp.UIModels


data class QuranPageUiModel(
    val pageNumber: Int,
    val surahNameAr: String,
    val chapterNameAr: String,
    val content: CharSequence,   // Uthmani content (shown to user)
    val searchText: String       // aya_text merged (used for search)

)
