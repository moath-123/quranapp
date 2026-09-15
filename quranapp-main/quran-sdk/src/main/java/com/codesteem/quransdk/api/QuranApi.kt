package com.codesteem.quransdk.api

import com.codesteem.quransdk.api.model.Ayah
import com.codesteem.quransdk.api.model.Juz
import com.codesteem.quransdk.api.model.SearchResult
import com.codesteem.quransdk.api.model.Surah

interface QuranApi {

    /**
     * Loads Quran data from bundled assets into the local database if needed.
     * Safe to call multiple times.
     */
    suspend fun initialize()

    suspend fun getSurahs(): List<Surah>

    suspend fun getJuzList(): List<Juz>

    suspend fun search(query: String, limit: Int = 200): List<SearchResult>

    suspend fun getAyahsByPage(pageNumber: Int): List<Ayah>

    suspend fun getAyahsBySurah(surahNumber: Int): List<Ayah>

    suspend fun getAyahsByJuz(juzNumber: Int): List<Ayah>

    suspend fun getAyahById(ayahId: Int): Ayah?

    suspend fun getFirstAyahOnPage(pageNumber: Int): Ayah?

    suspend fun getMaxPage(): Int
}
