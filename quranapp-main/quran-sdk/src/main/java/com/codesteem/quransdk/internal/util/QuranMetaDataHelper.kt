package com.codesteem.quransdk.internal.util

internal object QuranMetaDataHelper {

    data class JuzName(val en: String, val ar: String)

    private val JUZ_NAMES: Map<Int, JuzName> = mapOf(
        1 to JuzName("Alif-Lam-Mim", "آلم"),
        2 to JuzName("Sayaqool", "سَيَقُولُ"),
        3 to JuzName("Tilkal Rusul", "تِلْكَ ٱلْرُّسُلُ"),
        4 to JuzName("Lan Tanalu", "لَنْ تَنَالُوا"),
        5 to JuzName("Wal Muhsanat", "وَٱلْمُحْصَنَاتُ"),
        6 to JuzName("La Yuhibbullah", "لَا يُحِبُّ ٱللهُ"),
        7 to JuzName("Wa Iz Sami'u", "وَإِذَا سَمِعُوا"),
        8 to JuzName("Wa Lau Annana", "وَلَوْ أَنَّنَا"),
        9 to JuzName("Qalal Mala'u", "قَالَ ٱلْمَلَأُ"),
        10 to JuzName("Wa'lamu", "وَٱعْلَمُواْ"),
        11 to JuzName("Ya'taziruna", "يَعْتَذِرُونَ"),
        12 to JuzName("Wa Ma Min Dabbatin", "وَمَا مِنْ دَآبَّةٍ"),
        13 to JuzName("Wa Ma Ubri'u", "وَمَا أُبَرِّئُ"),
        14 to JuzName("Rubama", "رُبَمَا"),
        15 to JuzName("Subhanallazi", "سُبْحَانَ ٱلَّذِى"),
        16 to JuzName("Qala Alam", "قَالَ أَلَمْ"),
        17 to JuzName("Iqtaraba Linnasi", "ٱقْتَرَبَ لِلْنَّاسِ"),
        18 to JuzName("Qad Aflaha", "قَدْ أَفْلَحَ"),
        19 to JuzName("Wa Qalallazina", "وَقَالَ ٱلَّذِينَ"),
        20 to JuzName("Amman Khalaqa", "أَمَّنْ خَلَقَ"),
        21 to JuzName("Utlu Ma Oohiya", "أُتْلُ مَاأُوْحِیَ"),
        22 to JuzName("Wa Man Yaqnut", "وَمَنْ يَّقْنُتْ"),
        23 to JuzName("Wa Mali", "وَمَآ لي"),
        24 to JuzName("Faman Azlamu", "فَمَنْ أَظْلَمُ"),
        25 to JuzName("Ilaihi Yuraddu", "إِلَيْهِ يُرَدُّ"),
        26 to JuzName("Ha-Mim", "حم"),
        27 to JuzName("Qala Fama Khatbukum", "قَالَ فَمَا خَطْبُكُم"),
        28 to JuzName("Qad Sami'allahu", "قَدْ سَمِعَ ٱللهُ"),
        29 to JuzName("Tabarakallazi", "تَبَارَكَ ٱلَّذِى"),
        30 to JuzName("Amma", "عَمَّ")
    )

    fun juzNameFor(juzNumber: Int): JuzName =
        JUZ_NAMES[juzNumber] ?: JuzName("Juz' $juzNumber", "الجزء $juzNumber")
}
