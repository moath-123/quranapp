package com.codesteem.quransdk

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codesteem.quransdk.api.InvalidQuranDataException
import com.codesteem.quransdk.internal.data.QuranImporter
import com.codesteem.quransdk.internal.data.db.QuranDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: QuranDatabase
    private lateinit var prefs: SharedPreferences
    private val realJson by lazy { context.assets.open("quran.json").bufferedReader().use { it.readText() } }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java).build()
        prefs = context.getSharedPreferences("import_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        db.close()
        prefs.edit().clear().commit()
    }

    private suspend fun import(json: String = realJson, afterInsert: suspend () -> Unit = {}) =
        QuranImporter.importIfNeeded(db, prefs, { json }, afterInsert)

    private suspend fun counts() = db.quranDao().run { Triple(countSuras(), countAyas(), getAllChapters().size) }

    @Test
    fun firstImportLoadsEverythingAndSecondCallIsANoOp() = runBlocking {
        assertTrue(import())
        assertEquals(Triple(114, 6236, 30), counts())
        assertEquals(QuranImporter.DATA_VERSION, prefs.getInt("data_version", 0))
        assertFalse(import())
    }

    @Test
    fun olderDataVersionTriggersReimport() = runBlocking {
        import()
        prefs.edit().putInt("data_version", QuranImporter.DATA_VERSION - 1).commit()
        assertTrue(import())
        assertEquals(Triple(114, 6236, 30), counts())
    }

    @Test
    fun incompleteTablesTriggerReimport() = runBlocking {
        import()
        db.quranDao().deleteAllAyas()
        assertTrue(import())
        assertEquals(Triple(114, 6236, 30), counts())
    }

    @Test
    fun crashDuringFirstImportLeavesNothingBehind() = runBlocking {
        try {
            import(afterInsert = { throw RuntimeException("simulated crash") })
            fail("expected crash")
        } catch (e: RuntimeException) {
            assertEquals("simulated crash", e.message)
        }
        assertEquals(Triple(0, 0, 0), counts())
        assertEquals(0, prefs.getInt("data_version", 0))

        assertTrue(import())
        assertEquals(Triple(114, 6236, 30), counts())
    }

    @Test
    fun crashDuringReimportKeepsPreviousData() = runBlocking {
        import()
        prefs.edit().putInt("data_version", 1).commit()
        try {
            import(afterInsert = { throw RuntimeException("simulated crash") })
            fail("expected crash")
        } catch (_: RuntimeException) {
        }
        assertEquals(Triple(114, 6236, 30), counts())
        assertEquals(1, prefs.getInt("data_version", 0))
    }

    @Test
    fun invalidDataIsRejectedWithoutTouchingTheDatabase() = runBlocking {
        import()
        prefs.edit().putInt("data_version", 1).commit()
        // The first ayah's chapter_id (the sura object has its own chapter_id field before it).
        val broken = realJson.replaceFirst(Regex("\"chapter_id\": 1,(\\s*)\"sura\""), "\"chapter_id\": null,$1\"sura\"")
        assertTrue(broken != realJson)
        try {
            import(json = broken)
            fail("expected InvalidQuranDataException")
        } catch (e: InvalidQuranDataException) {
            assertTrue(e.message!!.contains("chapter_id"))
        }
        assertEquals(Triple(114, 6236, 30), counts())
    }

    @Test
    fun juzStartPagesAreCorrectInTheDatabase() = runBlocking {
        import()
        val pages = db.quranDao().getAllChapters().associate { it.chapterId to it.startPage }
        assertEquals(1, pages[1])
        assertEquals(42, pages[3])
        assertEquals(582, pages[30])
    }
}
