package com.codesteem.quranapp.DBModels


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterJson(
    val id: Int,
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("page_number") val pageNumber: Int
)
