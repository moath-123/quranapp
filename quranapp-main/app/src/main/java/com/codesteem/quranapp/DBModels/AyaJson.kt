package com.codesteem.quranapp.DBModels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AyaJson(
    val id: Int,
    val sura: Int,
    val aya: Int,
    @SerialName("chapter_id") val chapterId: Int? = null,
    val page: Int,
    @SerialName("line_start") val lineStart: Int? = null,
    @SerialName("line_end") val lineEnd: Int? = null,
    val text: String,
    @SerialName("aya_text") val ayaText: String
)
