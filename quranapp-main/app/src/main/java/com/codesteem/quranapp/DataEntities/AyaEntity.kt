package com.codesteem.quranapp.DataEntities

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
    val chapterId: Int,      // ← this is chapter_id from JSON
    val page: Int,
    val lineStart: Int?,
    val lineEnd: Int?,
    val textUthmani: String,
    val textSimple: String
)

