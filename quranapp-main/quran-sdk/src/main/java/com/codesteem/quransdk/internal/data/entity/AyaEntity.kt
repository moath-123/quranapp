package com.codesteem.quransdk.internal.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ayas",
    indices = [
        Index(value = ["suraNumber"]),
        Index(value = ["chapterId"]),
        Index(value = ["suraNumber", "ayaNumber"], unique = true)
    ]
)
data class AyaEntity(
    @PrimaryKey val id: Int,
    val suraNumber: Int,
    val ayaNumber: Int,
    val chapterId: Int,
    val page: Int,
    val lineStart: Int?,
    val lineEnd: Int?,
    val textUthmani: String,
    val textSimple: String
)
