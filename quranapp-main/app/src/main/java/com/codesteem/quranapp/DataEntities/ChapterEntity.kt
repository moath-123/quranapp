package com.codesteem.quranapp.DataEntities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val chapterId: Int,
    val nameAr: String,
    val nameEn: String,
    val startPage: Int
)
