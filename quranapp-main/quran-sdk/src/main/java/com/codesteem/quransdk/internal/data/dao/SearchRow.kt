package com.codesteem.quransdk.internal.data.dao

data class SearchRow(
    val id: Int,
    val suraNumber: Int,
    val ayaNumber: Int,
    val chapterId: Int,
    val page: Int,
    val textUthmani: String,
    val textSimple: String,
    val suraNameAr: String,
    val suraNameEn: String,
    val chapterNameAr: String?,
    val chapterNameEn: String?
)
