package com.codesteem.quransdk.api.model

data class Surah(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahCount: Int,
    val startPage: Int
)
