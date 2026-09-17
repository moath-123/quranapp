import Foundation

/// Same algorithm as `QuranRepository.searchArabic` on Android: every normalized token must appear in the
/// ayah's normalized simple text, and earlier matches rank higher. Like Android, page numbers are not
/// searched here — hosts can use `ArabicNormalizer.extractPageNumber` + `getAyahsByPage` for that.
enum QuranSearch {

    static func search(_ query: String, limit: Int, in store: QuranDataStore) -> [SearchResult] {
        guard limit > 0 else { return [] }
        let normalized = ArabicNormalizer.normalize(query)
        guard !normalized.isEmpty else { return [] }
        let tokens = normalized.split(separator: " ").map(String.init)

        var hits: [(index: Int, score: Int)] = []
        for (index, text) in store.normalizedText.enumerated() {
            var score = 0
            var matchesAll = true
            for token in tokens {
                guard let range = text.range(of: token) else { matchesAll = false; break }
                score += 10_000 - text.distance(from: text.startIndex, to: range.lowerBound)
            }
            if matchesAll { hits.append((index, score)) }
        }
        // Stable: equal scores keep mushaf order, like Kotlin's sortedByDescending.
        let ranked = hits.enumerated()
            .sorted { $0.element.score != $1.element.score ? $0.element.score > $1.element.score : $0.offset < $1.offset }
            .prefix(limit)
        return ranked.map { result(for: store.ayahs[$0.element.index], in: store) }
    }

    private static func result(for a: Ayah, in store: QuranDataStore) -> SearchResult {
        let sura = store.surahs[a.surahNumber - 1]
        let juz = store.juz.first { $0.number == a.juzNumber }
        return SearchResult(
            ayahId: a.id, ayahNumber: a.ayahNumber, surahNumber: a.surahNumber,
            surahNameAr: sura.nameAr, surahNameEn: sura.nameEn,
            textUthmani: a.textUthmani, textSimple: a.textSimple,
            pageNumber: a.pageNumber, juzNumber: a.juzNumber,
            juzNameAr: juz?.nameAr, juzNameEn: juz?.nameEn)
    }
}
