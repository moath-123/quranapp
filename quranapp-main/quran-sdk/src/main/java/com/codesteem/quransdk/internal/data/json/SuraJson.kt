package com.codesteem.quransdk.internal.data.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuraJson(
    val id: Int? = null,
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("aya_numbers") val ayaNumbers: Int,
    @SerialName("page_number") val pageNumber: Int? = null,
    val chapters: List<ChapterJson> = emptyList(),
    val ayas: List<AyaJson> = emptyList()
)
