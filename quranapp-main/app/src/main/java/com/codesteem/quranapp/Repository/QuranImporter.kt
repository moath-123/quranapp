package com.codesteem.quranapp.Repository


import android.content.Context
import android.util.Log
import com.codesteem.quranapp.DBModels.QuranJson
import com.codesteem.quranapp.DataEntities.AyaEntity
import com.codesteem.quranapp.DataEntities.ChapterEntity
import com.codesteem.quranapp.DataEntities.SuraEntity
import com.codesteem.quranapp.QuranDB.QuranDatabase
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object QuranImporter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importIfNeeded(context: Context, database: QuranDatabase) {
        val dao = database.quranDao()

        if (dao.getAllSuras().isNotEmpty()) return

        val jsonText = context.assets
            .open("quran.json")
            .bufferedReader()
            .use { it.readText() }

        //val root = json.decodeFromString(QuranJson.serializer(), jsonText)
        val root = json.decodeFromString<QuranJson>(jsonText)

        val suras = root.suras.map {
            SuraEntity(
                suraNumber = it.id ?: it.ayas.first().sura,
                nameAr = it.nameAr,
                nameEn = it.nameEn,
                ayahCount = it.ayaNumbers,
                startPage = it.pageNumber ?: it.ayas.minOf { a -> a.page }
            )
        }

        val chapters = mutableMapOf<Int, ChapterEntity>()
        root.suras.forEach { s ->
            s.chapters.forEach { c ->
                chapters[c.id] = ChapterEntity(
                    chapterId = c.id,
                    nameAr = c.nameAr,
                    nameEn = c.nameEn,
                    startPage = c.pageNumber
                )
            }
        }

        val ayas = root.suras.flatMap { s ->
            s.ayas.map { a ->
                AyaEntity(
                    id = a.id,
                    suraNumber = a.sura,
                    ayaNumber = a.aya,
                    chapterId = a.chapterId ?: 0,
                    page = a.page,
                    lineStart = a.lineStart,
                    lineEnd = a.lineEnd,
                    textUthmani = a.text,
                    textSimple = a.ayaText
                )
            }
        }

        dao.insertSuras(suras)
        dao.insertChapters(chapters.values.toList())
        dao.insertAyas(ayas)

        Log.d("QURAN_DB", "Suras: ${suras.size}")
        Log.d("QURAN_DB", "Chapters: ${chapters.size}")
        Log.d("QURAN_DB", "Ayas: ${ayas.size}")

    }
}
