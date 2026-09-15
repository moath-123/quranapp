package com.codesteem.quranapp.DBModels


import kotlinx.serialization.Serializable

@Serializable
data class QuranJson(
    val suras: List<SuraJson>
)
