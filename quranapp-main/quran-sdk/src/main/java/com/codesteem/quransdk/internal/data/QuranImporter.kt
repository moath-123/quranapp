package com.codesteem.quransdk.internal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.codesteem.quransdk.internal.data.db.QuranDatabase
import com.codesteem.quransdk.internal.data.json.QuranJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal object QuranImporter {

    /**
     * Bump whenever assets/quran.json changes so existing installs re-import it.
     * 2: pages aligned to the Madani 1405 edition, line numbers filled, juz start pages fixed.
     */
    const val DATA_VERSION = 2

    private const val PREFS_NAME = "quransdk_meta"
    private const val KEY_DATA_VERSION = "data_version"
    private const val ASSET_NAME = "quran.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val mutex = Mutex()

    /** Returns true when data was (re)imported. */
    suspend fun importIfNeeded(context: Context, database: QuranDatabase): Boolean =
        importIfNeeded(
            database = database,
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            readJson = { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }
        )

    /**
     * Imports only when the stored version differs or the tables are not complete. The whole replace runs
     * in one transaction, and the version is stored only after it commits, so an interrupted first launch
     * leaves either the previous data or nothing — never a partial import.
     *
     * [afterInsert] runs inside the transaction; tests use it to simulate a crash mid-import.
     */
    suspend fun importIfNeeded(
        database: QuranDatabase,
        prefs: SharedPreferences,
        readJson: () -> String,
        afterInsert: suspend () -> Unit = {}
    ): Boolean = mutex.withLock {
        val dao = database.quranDao()
        val upToDate = prefs.getInt(KEY_DATA_VERSION, 0) == DATA_VERSION &&
            dao.countSuras() == QuranDataMapper.EXPECTED_SURAS &&
            dao.countAyas() == QuranDataMapper.EXPECTED_AYAHS
        if (upToDate) return@withLock false

        val data = QuranDataMapper.map(json.decodeFromString<QuranJson>(readJson()))

        database.withTransaction {
            dao.deleteAllAyas()
            dao.deleteAllSuras()
            dao.deleteAllChapters()
            dao.insertSuras(data.suras)
            dao.insertChapters(data.chapters)
            dao.insertAyas(data.ayas)
            afterInsert()
        }
        prefs.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).commit()
        true
    }
}
