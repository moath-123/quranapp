package com.codesteem.quransdk.internal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suras")
data class SuraEntity(
    @PrimaryKey val suraNumber: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahCount: Int,
    val startPage: Int
)
