package com.codesteem.quranapp.helper

// com.codesteem.quranapp.helper.ArabicNormalizer.kt
object ArabicNormalizer {

    // Harakat + Quranic marks ranges
    private val diacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private const val tatweel = '\u0640'

    fun normalize(input: String): String {
        if (input.isBlank()) return ""

        var s = input

        // remove tashkeel / marks
        s = diacritics.replace(s, "")
        s = s.replace(tatweel.toString(), "")

        // unify common Arabic forms
        s = s.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')

        // optional (depends on your client expectation)
        // s = s.replace('ة', 'ه')

        // remove extra punctuation-ish things often present in Quran text
        s = s.replace("﴿", " ").replace("﴾", " ")
            .replace("،", " ").replace("؛", " ").replace("؟", " ")

        // collapse whitespace
        s = s.trim().replace(Regex("\\s+"), " ")
        return s
    }

    fun extractPageNumber(input: String): Int? {
        if (input.isBlank()) return null

        // Convert Arabic-Indic digits → Latin digits
        val normalizedDigits = input.map { ch ->
            when (ch) {
                '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                else -> ch
            }
        }.joinToString("")

        // Pull first number anywhere in the string (so "page 600" works too)
        val match = Regex("(\\d{1,4})").find(normalizedDigits) ?: return null
        return match.value.toIntOrNull()
    }

}
