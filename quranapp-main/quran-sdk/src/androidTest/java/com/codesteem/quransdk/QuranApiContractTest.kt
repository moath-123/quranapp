package com.codesteem.quransdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codesteem.quransdk.api.QuranApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/** The public QuranApi contract, against the real bundled data and the real on-disk database. */
@RunWith(AndroidJUnit4::class)
class QuranApiContractTest {

    companion object {
        private lateinit var api: QuranApi

        @BeforeClass
        @JvmStatic
        fun init() = runBlocking {
            api = QuranSdk.create(ApplicationProvider.getApplicationContext<Context>())
            api.initialize()
            api.initialize() // safe to call twice
        }
    }

    @Test
    fun surahs() = runBlocking {
        val surahs = api.getSurahs()
        assertEquals(114, surahs.size)
        assertEquals((1..114).toList(), surahs.map { it.number })
        with(surahs.first()) {
            assertEquals("الفاتحة", nameAr)
            assertEquals(7, ayahCount)
            assertEquals(1, startPage)
        }
        assertEquals(6236, surahs.sumOf { it.ayahCount })
    }

    @Test
    fun juzList() = runBlocking {
        val juz = api.getJuzList()
        assertEquals((1..30).toList(), juz.map { it.number })
        assertEquals(listOf(1, 22, 42), juz.take(3).map { it.startPage })
        assertEquals(582, juz.last().startPage)
    }

    @Test
    fun ayahsByPageSurahAndJuz() = runBlocking {
        assertEquals(7, api.getAyahsByPage(1).size)
        assertTrue(api.getAyahsByPage(1).all { it.surahNumber == 1 })
        assertEquals(286, api.getAyahsBySurah(2).size)
        assertEquals((1..286).toList(), api.getAyahsBySurah(2).map { it.ayahNumber })
        assertEquals(564, api.getAyahsByJuz(30).size)
        assertTrue(api.getAyahsByPage(9999).isEmpty())
    }

    @Test
    fun ayahById() = runBlocking {
        val ayah = assertNotNullAndGet(api.getAyahById(123))
        assertEquals(2, ayah.surahNumber)
        assertEquals(116, ayah.ayahNumber)
        assertEquals(1, ayah.juzNumber)
        assertEquals(18, ayah.pageNumber)
        assertTrue(ayah.textUthmani.isNotBlank() && ayah.textSimple.isNotBlank())
        assertTrue((ayah.lineStart ?: 0) > 0)
        assertNull(api.getAyahById(0))
        assertNull(api.getAyahById(6237))
    }

    @Test
    fun pageAnchorsAndMaxPage() = runBlocking {
        assertEquals(604, api.getMaxPage())
        val anchor = assertNotNullAndGet(api.getFirstAyahOnPage(15))
        assertEquals(15, anchor.pageNumber)
        assertEquals(api.getAyahsByPage(15).first().id, anchor.id)
        // 1405-edition page for an ayah that the old data placed one page earlier
        assertEquals(121, api.getAyahById(746)!!.pageNumber)
    }

    @Test
    fun search() = runBlocking {
        val results = api.search("الرحمن")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.textSimple.contains("الرحمن") })
        with(results.first()) {
            assertTrue(surahNameAr.isNotBlank())
            assertTrue(!juzNameAr.isNullOrBlank())
        }
        assertEquals(5, api.search("الله", limit = 5).size)
        assertTrue(api.search("").isEmpty())
        assertEquals(listOf(79), api.search("فادارأتم").map { it.ayahId })
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
