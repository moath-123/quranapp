package com.codesteem.quransdk.internal

import com.codesteem.quransdk.api.InvalidQuranDataException
import com.codesteem.quransdk.internal.data.QuranData
import com.codesteem.quransdk.internal.data.QuranDataMapper
import com.codesteem.quransdk.internal.data.json.QuranJson
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Maps the real bundled quran.json and checks the values the SDK exposes. */
class QuranDataMapperTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private lateinit var root: QuranJson
        private lateinit var data: QuranData

        @BeforeClass
        @JvmStatic
        fun load() {
            root = json.decodeFromString(File("src/main/assets/quran.json").readText())
            data = QuranDataMapper.map(root)
        }
    }

    @Test
    fun countsMatchTheMushaf() {
        assertEquals(114, data.suras.size)
        assertEquals(6236, data.ayas.size)
        assertEquals(30, data.chapters.size)
        assertEquals(604, data.ayas.maxOf { it.page })
        assertEquals(286, data.ayas.count { it.suraNumber == 2 })
    }

    @Test
    fun suraStartPages() {
        val start = data.suras.associate { it.suraNumber to it.startPage }
        assertEquals(1, start[1])   // Al-Fatiha (quran.json used to say 2)
        assertEquals(2, start[2])
        assertEquals(50, start[3])
        assertEquals(604, start[114])
    }

    @Test
    fun juzStartPagesComeFromAyahsNotFromPerSuraCopies() {
        val start = data.chapters.associate { it.chapterId to it.startPage }
        // Previously 28 of 30 were wrong (e.g. juz 30 -> 604, juz 3 -> 50).
        assertEquals(1, start[1])
        assertEquals(42, start[3])
        assertEquals(582, start[30])
        data.chapters.forEach { juz ->
            assertEquals(
                "juz ${juz.chapterId}",
                data.ayas.filter { it.chapterId == juz.chapterId }.minOf { it.page },
                juz.startPage
            )
        }
        assertEquals((1..30).toList(), data.chapters.map { it.chapterId })
        assertTrue(data.chapters.zipWithNext().all { (a, b) -> b.startPage > a.startPage })
    }

    @Test
    fun juzNamesComeFromTheData() {
        assertEquals("الجزء الأول", data.chapters.first().nameAr)
        assertEquals("Part 30", data.chapters.last().nameEn)
    }

    @Test
    fun ayahFieldsAreComplete() {
        data.ayas.forEach { a ->
            assertTrue("ayah ${a.id} chapter", a.chapterId in 1..30)
            assertTrue("ayah ${a.id} lines", (a.lineStart ?: 0) > 0 && (a.lineEnd ?: 0) >= (a.lineStart ?: 0))
            assertTrue("ayah ${a.id} text", a.textUthmani.isNotBlank() && a.textSimple.isNotBlank())
        }
    }

    @Test
    fun noStraySpacesInsideWords() {
        val odd = Regex("[  -​  　﻿]")
        assertFalse(data.ayas.any { odd.containsMatchIn(it.textUthmani) || odd.containsMatchIn(it.textSimple) })
        // Al-Baqarah 72 has 10 words; a stray U+2009 used to split «فَٱدَّٰرَٰٔتُمۡ» into two.
        assertEquals(10, data.ayas.first { it.id == 79 }.textUthmani.split(Regex("\\s+")).size)
    }

    @Test
    fun missingChapterIdIsRejected() {
        val broken = root.copy(suras = root.suras.mapIndexed { i, s ->
            if (i == 0) s.copy(ayas = s.ayas.mapIndexed { j, a -> if (j == 0) a.copy(chapterId = null) else a }) else s
        })
        val e = assertThrows(InvalidQuranDataException::class.java) { QuranDataMapper.map(broken) }
        assertTrue(e.message!!.contains("chapter_id"))
    }

    @Test
    fun truncatedDataIsRejected() {
        val truncated = root.copy(suras = root.suras.dropLast(1))
        assertThrows(InvalidQuranDataException::class.java) { QuranDataMapper.map(truncated) }
    }

    @Test
    fun missingAyahIsRejected() {
        val broken = root.copy(suras = root.suras.map { s ->
            if (s.id == 2) s.copy(ayas = s.ayas.drop(1)) else s
        })
        assertThrows(InvalidQuranDataException::class.java) { QuranDataMapper.map(broken) }
    }
}
