package com.codesteem.quransdk.api.model

data class Ayah(
    val id: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val juzNumber: Int,
    val pageNumber: Int,
    val textUthmani: String,
    val textSimple: String,
    val lineStart: Int? = null,
    val lineEnd: Int? = null
)
