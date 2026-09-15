package com.codesteem.quranapp.UIModels


data class JuzPageUiModel(
    val pageNumber: Int,

    val surahNameAr: String,
    val surahNameEn: String,

    val juzNameAr: String,
    val juzNameEn: String,

    val content: CharSequence
)
