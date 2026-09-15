package com.codesteem.quranapp.sdktest

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Client-app bookmark storage for SDK test page only.
 * This is NOT part of the Quran SDK — demonstrates how a client app
 * would manage bookmarks using its own storage.
 */
class ClientBookmarkStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<ClientBookmark> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ClientBookmark>>(raw)
        }.getOrDefault(emptyList())
            .sortedByDescending { it.ayahId }
    }

    fun isBookmarked(ayahId: Int): Boolean =
        getAll().any { it.ayahId == ayahId }

    fun add(bookmark: ClientBookmark) {
        val current = getAll().toMutableList()
        if (current.any { it.ayahId == bookmark.ayahId }) return
        current.add(0, bookmark)
        save(current)
    }

    fun remove(ayahId: Int) {
        val updated = getAll().filterNot { it.ayahId == ayahId }
        save(updated)
    }

    fun toggle(bookmark: ClientBookmark): Boolean {
        return if (isBookmarked(bookmark.ayahId)) {
            remove(bookmark.ayahId)
            false
        } else {
            add(bookmark)
            true
        }
    }

    fun count(): Int = getAll().size

    private fun save(items: List<ClientBookmark>) {
        prefs.edit().putString(KEY_ITEMS, json.encodeToString(items)).apply()
    }

    companion object {
        private const val PREFS_NAME = "sdk_client_bookmarks"
        private const val KEY_ITEMS = "bookmarks"
    }
}
