package com.codesteem.quranapp.UIModels


data class AyahUiModel(
    val id: Int,
    val suraNumber: Int,
    val ayaNumber: Int,
    val page: Int,
    val textUthmani: String, // display
    val ayaText: String      // search-friendly (aya_text)
)
