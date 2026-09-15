package com.codesteem.quransdk.api.model

data class SearchResult(
    val ayahId: Int,
    val ayahNumber: Int,
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val textUthmani: String,
    val textSimple: String,
    val pageNumber: Int,
    val juzNumber: Int,
    val juzNameAr: String?,
    val juzNameEn: String?
)
