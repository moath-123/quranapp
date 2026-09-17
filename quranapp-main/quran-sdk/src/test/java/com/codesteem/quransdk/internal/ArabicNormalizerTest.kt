package com.codesteem.quransdk.internal

import com.codesteem.quransdk.internal.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArabicNormalizerTest {

    @Test
    fun removesDiacriticsAndQuranicMarks() {
        assertEquals("الرحمن", ArabicNormalizer.normalize("ٱلرَّحۡمَٰنِ"))
        assertEquals("بسم الله الرحمن الرحيم", ArabicNormalizer.normalize("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"))
    }

    @Test
    fun unifiesHamzaAndAlefForms() {
        assertEquals("ايمان", ArabicNormalizer.normalize("إيمان"))
        assertEquals("امن", ArabicNormalizer.normalize("آمن"))
        assertEquals("اخرة", ArabicNormalizer.normalize("أخرة"))
        assertEquals("موسي", ArabicNormalizer.normalize("موسى"))
        assertEquals("مومن", ArabicNormalizer.normalize("مؤمن"))
        assertEquals("شيء", ArabicNormalizer.normalize("شىء"))
    }

    @Test
    fun removesTatweelAndCollapsesSpaces() {
        assertEquals("اوليك", ArabicNormalizer.normalize("أُوْلَـٰٓئِكَ"))
        assertEquals("قل هو الله", ArabicNormalizer.normalize("  قُلۡ   هُوَ\tٱللَّهُ "))
    }

    @Test
    fun stripsAyahBracketsAndPunctuation() {
        assertEquals("الحمد لله ١", ArabicNormalizer.normalize("الحمد لله ﴿١﴾"))
        assertEquals("من ذا", ArabicNormalizer.normalize("من، ذا؟"))
    }

    @Test
    fun blankInputGivesEmptyString() {
        assertEquals("", ArabicNormalizer.normalize(""))
        assertEquals("", ArabicNormalizer.normalize("   "))
    }

    @Test
    fun extractsPageNumberFromWesternAndArabicDigits() {
        assertEquals(50, ArabicNormalizer.extractPageNumber("صفحة 50"))
        assertEquals(604, ArabicNormalizer.extractPageNumber("٦٠٤"))
        assertNull(ArabicNormalizer.extractPageNumber("الرحمن"))
        assertNull(ArabicNormalizer.extractPageNumber(""))
    }
}
