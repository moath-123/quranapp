package com.codesteem.quranapp.QuranDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codesteem.quranapp.DAO.QuranDao
import com.codesteem.quranapp.DataEntities.AyaEntity
import com.codesteem.quranapp.DataEntities.BookmarkEntity
import com.codesteem.quranapp.DataEntities.ChapterEntity
import com.codesteem.quranapp.DataEntities.SuraEntity

@Database(
    entities = [AyaEntity::class, SuraEntity::class, ChapterEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile private var INSTANCE: QuranDatabase? = null

        // ✅ Migration: 1 -> 2 (creates bookmarks table)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ayaId INTEGER NOT NULL,
                        suraNumber INTEGER NOT NULL,
                        ayaNumber INTEGER NOT NULL,
                        chapterId INTEGER NOT NULL,
                        page INTEGER NOT NULL,
                        textUthmani TEXT NOT NULL,
                        textSimple TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarks_ayaId ON bookmarks(ayaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_page ON bookmarks(page)")
            }
        }

        fun getInstance(context: Context): QuranDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran.db"
                )
                    // ✅ THIS is where migration is added
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/*
package com.codesteem.quranapp.QuranDB


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codesteem.quranapp.DAO.QuranDao
import com.codesteem.quranapp.DataEntities.AyaEntity
import com.codesteem.quranapp.DataEntities.ChapterEntity
import com.codesteem.quranapp.DataEntities.SuraEntity

@Database(
    entities = [AyaEntity::class, SuraEntity::class, ChapterEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile private var INSTANCE: QuranDatabase? = null


        fun getInstance(context: Context): QuranDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran.db"
                ).build().also { INSTANCE = it }
            }
    }
}
*/
