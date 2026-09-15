package com.codesteem.quranapp.sdktest

enum class SdkInputType {
    NONE,
    SEARCH,
    PAGE,
    SURAH,
    JUZ,
    AYAH_ID
}

data class SdkApiAction(
    val methodName: String,
    val description: String,
    val inputType: SdkInputType,
    val inputHint: String = "",
    val requiresInit: Boolean = true
)

object SdkApiCatalog {

    val all: List<SdkApiAction> = listOf(
        SdkApiAction(
            methodName = "getSurahs()",
            description = "Returns all 114 surahs with Arabic/English names",
            inputType = SdkInputType.NONE
        ),
        SdkApiAction(
            methodName = "getJuzList()",
            description = "Returns all 30 juz with names and start pages",
            inputType = SdkInputType.NONE
        ),
        SdkApiAction(
            methodName = "search(query)",
            description = "Arabic text search — returns matching ayahs",
            inputType = SdkInputType.SEARCH,
            inputHint = "Arabic search text e.g. الرحمن"
        ),
        SdkApiAction(
            methodName = "getAyahsByPage(page)",
            description = "All ayahs on a mushaf page (1–604)",
            inputType = SdkInputType.PAGE,
            inputHint = "Page number e.g. 1"
        ),
        SdkApiAction(
            methodName = "getAyahsBySurah(surah)",
            description = "All ayahs of a specific surah",
            inputType = SdkInputType.SURAH,
            inputHint = "Surah number 1–114"
        ),
        SdkApiAction(
            methodName = "getAyahsByJuz(juz)",
            description = "All ayahs in a juz",
            inputType = SdkInputType.JUZ,
            inputHint = "Juz number 1–30"
        ),
        SdkApiAction(
            methodName = "getAyahById(id)",
            description = "Single ayah by database ID",
            inputType = SdkInputType.AYAH_ID,
            inputHint = "Ayah ID e.g. 1"
        ),
        SdkApiAction(
            methodName = "getFirstAyahOnPage(page)",
            description = "First ayah anchor on a page (for page jump)",
            inputType = SdkInputType.PAGE,
            inputHint = "Page number e.g. 15"
        ),
        SdkApiAction(
            methodName = "getMaxPage()",
            description = "Total number of mushaf pages (usually 604)",
            inputType = SdkInputType.NONE
        )
    )
}
