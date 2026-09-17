package com.codesteem.quransdk.internal

import com.codesteem.quransdk.internal.data.QuranRepository
import com.codesteem.quransdk.internal.data.dao.QuranDao
import com.codesteem.quransdk.internal.data.dao.SearchRow
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.ChapterEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranRepositorySearchTest {

    private fun row(id: Int, simple: String, uthmani: String = simple) = SearchRow(
        id = id, suraNumber = 1, ayaNumber = id, chapterId = 1, page = 1,
        textUthmani = uthmani, textSimple = simple,
        suraNameAr = "الفاتحة", suraNameEn = "Al-Faatiha", chapterNameAr = "الجزء الأول", chapterNameEn = "Part 1"
    )

    private class FakeDao(var rows: List<SearchRow>) : QuranDao {
        var searchLoads = 0
        override suspend fun getAllSearchRows(): List<SearchRow> { searchLoads++; return rows }
        override suspend fun insertSuras(items: List<SuraEntity>) = error("unused")
        override suspend fun insertChapters(items: List<ChapterEntity>) = error("unused")
        override suspend fun insertAyas(items: List<AyaEntity>) = error("unused")
        override suspend fun deleteAllAyas() = error("unused")
        override suspend fun deleteAllSuras() = error("unused")
        override suspend fun deleteAllChapters() = error("unused")
        override suspend fun countAyas(): Int = error("unused")
        override suspend fun countSuras(): Int = error("unused")
        override suspend fun getAyaById(ayahId: Int): AyaEntity? = error("unused")
        override suspend fun getMaxPage(): Int? = error("unused")
        override suspend fun getFirstAyahOnPage(pageNumber: Int): AyaEntity? = error("unused")
        override suspend fun getAyasBySurah(suraNumber: Int): List<AyaEntity> = error("unused")
        override suspend fun getAyasByChapter(chapterId: Int): List<AyaEntity> = error("unused")
        override suspend fun getAllSuras(): List<SuraEntity> = error("unused")
        override suspend fun getAllChapters(): List<ChapterEntity> = error("unused")
        override suspend fun getAyasByPage(pageNumber: Int): List<AyaEntity> = error("unused")
    }

    private val rows = listOf(
        row(1, "بسم الله الرحمن الرحيم"),
        row(2, "الحمد لله رب العالمين"),
        row(3, "الرحمن الرحيم"),
        row(4, "إياك نعبد وإياك نستعين"),
    )

    @Test
    fun matchesIgnoringDiacriticsAndHamza() = runTest {
        val repo = QuranRepository(FakeDao(rows))
        assertEquals(listOf(4), repo.searchArabic("اياك").map { it.id })
        assertEquals(listOf(3, 1), repo.searchArabic("ٱلرَّحۡمَٰنِ").map { it.id })
    }

    @Test
    fun earlierMatchRanksHigher() = runTest {
        val ids = QuranRepository(FakeDao(rows)).searchArabic("الرحيم").map { it.id }
        assertEquals(listOf(3, 1), ids)
    }

    @Test
    fun allTokensMustMatch() = runTest {
        val repo = QuranRepository(FakeDao(rows))
        assertEquals(listOf(1), repo.searchArabic("بسم الرحيم").map { it.id })
        assertTrue(repo.searchArabic("بسم نستعين").isEmpty())
    }

    @Test
    fun respectsLimitAndBlankQuery() = runTest {
        val repo = QuranRepository(FakeDao(rows))
        assertEquals(1, repo.searchArabic("الله", limit = 1).size)
        assertTrue(repo.searchArabic("   ").isEmpty())
        assertTrue(repo.searchArabic("ـ").isEmpty())
    }

    @Test
    fun cachesRowsUntilCleared() = runTest {
        val dao = FakeDao(rows)
        val repo = QuranRepository(dao)
        repo.searchArabic("الله")
        repo.searchArabic("الرحمن")
        assertEquals(1, dao.searchLoads)

        dao.rows = listOf(row(9, "قل هو الله احد"))
        assertTrue(repo.searchArabic("احد").isEmpty())
        repo.clearCache()
        assertEquals(listOf(9), repo.searchArabic("احد").map { it.id })
        assertEquals(2, dao.searchLoads)
    }
}
