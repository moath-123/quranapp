package com.codesteem.quransdk.internal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.ChapterEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity

@Dao
interface QuranDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuras(items: List<SuraEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(items: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyas(items: List<AyaEntity>)

    @Query("DELETE FROM ayas")
    suspend fun deleteAllAyas()

    @Query("DELETE FROM suras")
    suspend fun deleteAllSuras()

    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()

    @Query("SELECT COUNT(*) FROM ayas")
    suspend fun countAyas(): Int

    @Query("SELECT COUNT(*) FROM suras")
    suspend fun countSuras(): Int

    @Query("SELECT * FROM ayas WHERE id = :ayahId LIMIT 1")
    suspend fun getAyaById(ayahId: Int): AyaEntity?

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
        WHERE suraNumber = :suraNumber
        ORDER BY ayaNumber ASC
    """)
    suspend fun getAyasBySurah(suraNumber: Int): List<AyaEntity>

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
