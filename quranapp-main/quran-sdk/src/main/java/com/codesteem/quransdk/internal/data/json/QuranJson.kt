package com.codesteem.quransdk.internal.data.json

import kotlinx.serialization.Serializable

@Serializable
data class QuranJson(
    val suras: List<SuraJson>
)
