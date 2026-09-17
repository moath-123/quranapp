package com.codesteem.quransdk.internal

import android.content.Context
import com.codesteem.quransdk.api.QuranApi
import com.codesteem.quransdk.api.model.Ayah
import com.codesteem.quransdk.api.model.Juz
import com.codesteem.quransdk.api.model.SearchResult
import com.codesteem.quransdk.api.model.Surah
import com.codesteem.quransdk.internal.data.QuranImporter
import com.codesteem.quransdk.internal.data.QuranRepository
import com.codesteem.quransdk.internal.data.db.QuranDatabase
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.ChapterEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class QuranApiImpl(
    private val appContext: Context
) : QuranApi {

    private val database by lazy { QuranDatabase.getInstance(appContext) }
    private val dao by lazy { database.quranDao() }
    private val repository by lazy { QuranRepository(dao) }

    internal fun getDao() = dao

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            if (QuranImporter.importIfNeeded(appContext, database)) repository.clearCache()
        }
    }

    override suspend fun getSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        dao.getAllSuras().map { it.toSurah() }
    }

    override suspend fun getJuzList(): List<Juz> = withContext(Dispatchers.IO) {
        dao.getAllChapters().map { it.toJuz() }
    }

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        withContext(Dispatchers.Default) {
            repository.searchArabic(query, limit).map { it.toSearchResult() }
        }

    override suspend fun getAyahsByPage(pageNumber: Int): List<Ayah> =
        withContext(Dispatchers.IO) {
            dao.getAyasByPage(pageNumber).map { it.toAyah() }
        }

    override suspend fun getAyahsBySurah(surahNumber: Int): List<Ayah> =
        withContext(Dispatchers.IO) {
            dao.getAyasBySurah(surahNumber).map { it.toAyah() }
        }

    override suspend fun getAyahsByJuz(juzNumber: Int): List<Ayah> =
        withContext(Dispatchers.IO) {
            dao.getAyasByChapter(juzNumber).map { it.toAyah() }
        }

    override suspend fun getAyahById(ayahId: Int): Ayah? = withContext(Dispatchers.IO) {
        dao.getAyaById(ayahId)?.toAyah()
    }

    override suspend fun getFirstAyahOnPage(pageNumber: Int): Ayah? =
        withContext(Dispatchers.IO) {
            dao.getFirstAyahOnPage(pageNumber)?.toAyah()
        }

    override suspend fun getMaxPage(): Int = withContext(Dispatchers.IO) {
        dao.getMaxPage() ?: 604
    }

    private fun SuraEntity.toSurah() = Surah(
        number = suraNumber,
        nameAr = nameAr,
        nameEn = nameEn,
        ayahCount = ayahCount,
        startPage = startPage
    )

    private fun ChapterEntity.toJuz() = Juz(
        number = chapterId,
        nameAr = nameAr,
        nameEn = nameEn,
        startPage = startPage
    )

    private fun AyaEntity.toAyah() = Ayah(
        id = id,
        surahNumber = suraNumber,
        ayahNumber = ayaNumber,
        juzNumber = chapterId,
        pageNumber = page,
        textUthmani = textUthmani,
        textSimple = textSimple,
        lineStart = lineStart,
        lineEnd = lineEnd
    )

    private fun com.codesteem.quransdk.internal.data.dao.SearchRow.toSearchResult() =
        SearchResult(
            ayahId = id,
            ayahNumber = ayaNumber,
            surahNumber = suraNumber,
            surahNameAr = suraNameAr,
            surahNameEn = suraNameEn,
            textUthmani = textUthmani,
            textSimple = textSimple,
            pageNumber = page,
            juzNumber = chapterId,
            juzNameAr = chapterNameAr,
            juzNameEn = chapterNameEn
        )
}
