package com.codesteem.quranapp.sdktest

import kotlinx.serialization.Serializable

@Serializable
data class ClientBookmark(
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val textUthmani: String,
    val surahName: String = ""
)
