package com.codesteem.quranapp.DataEntities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["ayaId"], unique = true),
        Index(value = ["page"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ayaId: Int,
    val suraNumber: Int,
    val ayaNumber: Int,
    val chapterId: Int,
    val page: Int,
    val textUthmani: String,
    val textSimple: String,
    val createdAt: Long = System.currentTimeMillis()
)
