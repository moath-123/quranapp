package com.codesteem.quranapp.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codesteem.quranapp.DataEntities.AyaEntity
import com.codesteem.quranapp.DataEntities.BookmarkEntity
import com.codesteem.quranapp.DataEntities.ChapterEntity
import com.codesteem.quranapp.DataEntities.SuraEntity

@Dao
interface QuranDao {

    /* ---------- INSERT ---------- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuras(items: List<SuraEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(items: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyas(items: List<AyaEntity>)



    /* ---------- BOOKMARK QUERIES ---------- */
    @Query("""
    SELECT * FROM ayas
    WHERE page = :page AND ayaNumber = :ayaNumber
    LIMIT 1
""")
    suspend fun getAyaByPageAndAyaNumber(page: Int, ayaNumber: Int): AyaEntity?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookmark(b: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE ayaId = :ayaId")
    suspend fun removeBookmark(ayaId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE ayaId = :ayaId)")
    suspend fun isBookmarked(ayaId: Int): Boolean

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarks(): List<BookmarkEntity>

    // Needed for bookmarking from Reader using ayaId
    @Query("SELECT * FROM ayas WHERE id = :ayaId LIMIT 1")
    suspend fun getAyaById(ayaId: Int): AyaEntity?



    /* ---------- QUERIES ---------- */


    @Query("""
    SELECT 
        a.id AS id,
        a.suraNumber AS suraNumber,
        a.ayaNumber AS ayaNumber,
        a.chapterId AS chapterId,
        a.page AS page,
        a.textUthmani AS textUthmani,
        a.textSimple AS textSimple,
        s.nameAr AS suraNameAr,
        s.nameEn AS suraNameEn,
        c.nameAr AS chapterNameAr,
        c.nameEn AS chapterNameEn
    FROM ayas a
    JOIN suras s ON s.suraNumber = a.suraNumber
    LEFT JOIN chapters c ON c.chapterId = a.chapterId
    ORDER BY a.suraNumber ASC, a.ayaNumber ASC
""")
    suspend fun getAllSearchRows(): List<SearchRow>

    @Query("SELECT MAX(page) FROM ayas")
    suspend fun getMaxPage(): Int?

    @Query("""
    SELECT * FROM ayas
    WHERE page = :pageNumber
    ORDER BY suraNumber ASC, ayaNumber ASC
    LIMIT 1
""")
    suspend fun getFirstAyahOnPage(pageNumber: Int): AyaEntity?



    @Query("""
    SELECT * FROM ayas
    WHERE chapterId = :chapterId
    ORDER BY page ASC, suraNumber ASC, ayaNumber ASC
""")
    suspend fun getAyasByChapterOrderedByPage(chapterId: Int): List<AyaEntity>


    @Query("SELECT * FROM ayas WHERE suraNumber = :suraNumber ORDER BY page ASC, ayaNumber ASC")
    suspend fun getAyasBySurahOrderedByPage(suraNumber: Int): List<AyaEntity>


    @Query("""
    SELECT * FROM ayas
    WHERE page IN (:pages)
    ORDER BY page ASC, suraNumber ASC, ayaNumber ASC
""")
    suspend fun getAyasByPages(pages: List<Int>): List<AyaEntity>

    @Query("SELECT * FROM ayas ORDER BY page ASC, suraNumber ASC, ayaNumber ASC")
    suspend fun getAllAyasOrderedByPage(): List<AyaEntity>

    // Get All Quran
    @Query("SELECT * FROM ayas ORDER BY suraNumber ASC, ayaNumber ASC")
    suspend fun getAllAyasOrdered(): List<AyaEntity>

    // Surah-wise Ayahs
    @Query("""
        SELECT * FROM ayas
        WHERE suraNumber = :suraNumber
        ORDER BY ayaNumber ASC
    """)
    suspend fun getAyasBySurah(suraNumber: Int): List<AyaEntity>

    // ✅ Chapter-wise (Juz-wise equivalent)
    @Query("""
        SELECT * FROM ayas
        WHERE chapterId = :chapterId
        ORDER BY suraNumber ASC, ayaNumber ASC
    """)
    suspend fun getAyasByChapter(chapterId: Int): List<AyaEntity>

    @Query("SELECT * FROM suras ORDER BY suraNumber ASC")
    suspend fun getAllSuras(): List<SuraEntity>

    @Query("SELECT * FROM chapters ORDER BY chapterId ASC")
    suspend fun getAllChapters(): List<ChapterEntity>

    @Query("""
    SELECT * FROM ayas
    WHERE page = :pageNumber
    ORDER BY suraNumber ASC, ayaNumber ASC
""")
    suspend fun getAyasByPage(pageNumber: Int): List<AyaEntity>

}
