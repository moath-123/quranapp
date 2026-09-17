import Testing
@testable import TaahudQuranCore

/// Same cases as `ArabicNormalizerTest.kt` on Android.
struct ArabicNormalizerTests {

    @Test func removesDiacriticsAndQuranicMarks() {
        #expect(ArabicNormalizer.normalize("ٱلرَّحۡمَٰنِ") == "الرحمن")
        #expect(ArabicNormalizer.normalize("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ") == "بسم الله الرحمن الرحيم")
    }

    @Test func unifiesHamzaAndAlefForms() {
        #expect(ArabicNormalizer.normalize("إيمان") == "ايمان")
        #expect(ArabicNormalizer.normalize("آمن") == "امن")
        #expect(ArabicNormalizer.normalize("أخرة") == "اخرة")
        #expect(ArabicNormalizer.normalize("موسى") == "موسي")
        #expect(ArabicNormalizer.normalize("مؤمن") == "مومن")
        #expect(ArabicNormalizer.normalize("شىء") == "شيء")
    }

    @Test func removesTatweelAndCollapsesSpaces() {
        #expect(ArabicNormalizer.normalize("أُوْلَـٰٓئِكَ") == "اوليك")
        #expect(ArabicNormalizer.normalize("  قُلۡ   هُوَ\tٱللَّهُ ") == "قل هو الله")
    }

    @Test func stripsAyahBracketsAndPunctuation() {
        #expect(ArabicNormalizer.normalize("الحمد لله ﴿١﴾") == "الحمد لله ١")
        #expect(ArabicNormalizer.normalize("من، ذا؟") == "من ذا")
    }

    @Test func blankInputGivesEmptyString() {
        #expect(ArabicNormalizer.normalize("") == "")
        #expect(ArabicNormalizer.normalize("   ") == "")
    }

    @Test func extractsPageNumberFromWesternAndArabicDigits() {
        #expect(ArabicNormalizer.extractPageNumber("صفحة 50") == 50)
        #expect(ArabicNormalizer.extractPageNumber("٦٠٤") == 604)
        #expect(ArabicNormalizer.extractPageNumber("الرحمن") == nil)
        #expect(ArabicNormalizer.extractPageNumber("") == nil)
    }
}
