package com.codesteem.quransdk.internal.data

import com.codesteem.quransdk.api.InvalidQuranDataException
import com.codesteem.quransdk.internal.data.entity.AyaEntity
import com.codesteem.quransdk.internal.data.entity.ChapterEntity
import com.codesteem.quransdk.internal.data.entity.SuraEntity
import com.codesteem.quransdk.internal.data.json.QuranJson

internal class QuranData(
    val suras: List<SuraEntity>,
    val chapters: List<ChapterEntity>,
    val ayas: List<AyaEntity>
)

/**
 * Converts parsed quran.json into database rows, and rejects incomplete data instead of importing it.
 *
 * Start pages are derived from the ayahs themselves: `sura.chapters[].page_number` in quran.json is the
 * page of that sura inside the juz, not where the juz starts, so it must not be used for juz start pages.
 */
internal object QuranDataMapper {

    const val EXPECTED_SURAS = 114
    const val EXPECTED_AYAHS = 6236
    const val EXPECTED_JUZ = 30

    fun map(root: QuranJson): QuranData {
        val ayas = root.suras.flatMap { s ->
            s.ayas.map { a ->
                AyaEntity(
                    id = a.id,
                    suraNumber = a.sura,
                    ayaNumber = a.aya,
                    chapterId = a.chapterId
                        ?: throw InvalidQuranDataException("Ayah ${a.id} (${a.sura}:${a.aya}) has no chapter_id"),
                    page = a.page,
                    lineStart = a.lineStart,
                    lineEnd = a.lineEnd,
                    textUthmani = a.text,
                    textSimple = a.ayaText
                )
            }
        }

        val suras = root.suras.map { s ->
            if (s.ayas.isEmpty()) throw InvalidQuranDataException("Sura ${s.id} has no ayahs")
            SuraEntity(
                suraNumber = s.id ?: s.ayas.first().sura,
                nameAr = s.nameAr,
                nameEn = s.nameEn,
                ayahCount = s.ayaNumbers,
                startPage = s.ayas.minOf { it.page }
            )
        }

        val names = LinkedHashMap<Int, Pair<String, String>>()
        (root.chapters + root.suras.flatMap { it.chapters }).forEach { c ->
            names.putIfAbsent(c.id, c.nameAr to c.nameEn)
        }
        val juzStartPage = ayas.groupBy { it.chapterId }.mapValues { (_, list) -> list.minOf { it.page } }
        val chapters = juzStartPage.keys.sorted().map { id ->
            val (nameAr, nameEn) = names[id] ?: ("الجزء $id" to "Juz $id")
            ChapterEntity(chapterId = id, nameAr = nameAr, nameEn = nameEn, startPage = juzStartPage.getValue(id))
        }

        validate(suras, chapters, ayas)
        return QuranData(suras, chapters, ayas)
    }

    private fun validate(suras: List<SuraEntity>, chapters: List<ChapterEntity>, ayas: List<AyaEntity>) {
        fun check(ok: Boolean, message: () -> String) {
            if (!ok) throw InvalidQuranDataException(message())
        }
        check(suras.size == EXPECTED_SURAS) { "Expected $EXPECTED_SURAS suras, found ${suras.size}" }
        check(ayas.size == EXPECTED_AYAHS) { "Expected $EXPECTED_AYAHS ayahs, found ${ayas.size}" }
        check(chapters.size == EXPECTED_JUZ) { "Expected $EXPECTED_JUZ juz, found ${chapters.size}" }
        check(ayas.map { it.id } == (1..EXPECTED_AYAHS).toList()) { "Ayah ids are not sequential 1..$EXPECTED_AYAHS" }
        suras.forEach { s ->
            val count = ayas.count { it.suraNumber == s.suraNumber }
            check(count == s.ayahCount) { "Sura ${s.suraNumber}: aya_numbers=${s.ayahCount} but has $count ayahs" }
        }
        check(ayas.zipWithNext().all { (a, b) -> b.page >= a.page }) { "Ayah pages are not in reading order" }
    }
}
