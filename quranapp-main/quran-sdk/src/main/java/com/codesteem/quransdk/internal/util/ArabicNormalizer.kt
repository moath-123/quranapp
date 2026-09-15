package com.codesteem.quransdk.internal.util

internal object ArabicNormalizer {

    private val diacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private const val tatweel = '\u0640'

    fun normalize(input: String): String {
        if (input.isBlank()) return ""

        var s = diacritics.replace(input, "")
        s = s.replace(tatweel.toString(), "")
        s = s.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
        s = s.replace("﴿", " ").replace("﴾", " ")
            .replace("،", " ").replace("؛", " ").replace("؟", " ")

        return s.trim().replace(Regex("\\s+"), " ")
    }

    fun extractPageNumber(input: String): Int? {
        if (input.isBlank()) return null

        val normalizedDigits = input.map { ch ->
            when (ch) {
                '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                else -> ch
            }
        }.joinToString("")

        val match = Regex("(\\d{1,4})").find(normalizedDigits) ?: return null
        return match.value.toIntOrNull()
    }
}
