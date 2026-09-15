package com.codesteem.quransdk.internal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codesteem.quransdk.internal.data.dao.QuranDao
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.ChapterEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity

@Database(
    entities = [AyaEntity::class, SuraEntity::class, ChapterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var instance: QuranDatabase? = null

        fun getInstance(context: Context): QuranDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_sdk.db"
                ).build().also { instance = it }
            }
    }
}
