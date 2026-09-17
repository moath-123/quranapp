package com.codesteem.quransdk.internal.data

import com.codesteem.quransdk.internal.data.dao.QuranDao
import com.codesteem.quransdk.internal.data.dao.SearchRow
import com.codesteem.quransdk.internal.util.ArabicNormalizer

internal class QuranRepository(private val dao: QuranDao) {

    private var cachedSearch: List<Pair<SearchRow, String>>? = null

    private suspend fun getCachedSearchRows(): List<Pair<SearchRow, String>> {
        val current = cachedSearch
        if (current != null) return current

        val rows = dao.getAllSearchRows()
        val prepared = rows.map { row ->
            row to ArabicNormalizer.normalize(row.textSimple)
        }
        cachedSearch = prepared
        return prepared
    }

    fun clearCache() {
        cachedSearch = null
    }

    suspend fun searchArabic(queryRaw: String, limit: Int = 200): List<SearchRow> {
        val q = ArabicNormalizer.normalize(queryRaw)
        if (q.isBlank()) return emptyList()

        val tokens = q.split(" ").filter { it.isNotBlank() }
        val items = getCachedSearchRows()

        return items.asSequence()
            .mapNotNull { (row, normText) ->
                if (tokens.all { normText.contains(it) }) {
                    val score = tokens.sumOf { t ->
                        val idx = normText.indexOf(t)
                        if (idx >= 0) (10_000 - idx) else 0
                    }
                    row to score
                } else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }
}
