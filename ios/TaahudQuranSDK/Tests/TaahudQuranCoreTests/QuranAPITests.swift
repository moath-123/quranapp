import Foundation
import Testing
@testable import TaahudQuranCore

/// The public API against the real bundled data. Mirrors `QuranApiContractTest` on Android,
/// so both SDKs are checked against the same expectations.
struct QuranAPITests {
    let api = QuranAPI()

    @Test func surahs() async throws {
        try await api.initialize()
        try await api.initialize() // safe twice
        let surahs = try await api.getSurahs()
        #expect(surahs.count == 114)
        #expect(surahs.map(\.number) == Array(1...114))
        #expect(surahs[0].nameAr == "الفاتحة")
        #expect(surahs[0].ayahCount == 7)
        #expect(surahs[0].startPage == 1)
        #expect(surahs[2].startPage == 50)
        #expect(surahs.map(\.ayahCount).reduce(0, +) == 6236)
    }

    @Test func juzList() async throws {
        let juz = try await api.getJuzList()
        #expect(juz.map(\.number) == Array(1...30))
        #expect(juz.prefix(3).map(\.startPage) == [1, 22, 42])
        #expect(juz.last?.startPage == 582)
        #expect(juz.first?.nameAr == "الجزء الأول")
        #expect(zip(juz, juz.dropFirst()).allSatisfy { $1.startPage > $0.startPage })
    }

    @Test func ayahsByPageSurahAndJuz() async throws {
        let page1 = try await api.getAyahsByPage(1)
        #expect(page1.count == 7)
        #expect(page1.allSatisfy { $0.surahNumber == 1 })
        #expect(try await api.getAyahsBySurah(2).map(\.ayahNumber) == Array(1...286))
        #expect(try await api.getAyahsByJuz(30).count == 564)
        #expect(try await api.getAyahsByPage(9999).isEmpty)
    }

    @Test func ayahById() async throws {
        let ayah = try #require(try await api.getAyahById(123))
        #expect(ayah.surahNumber == 2)
        #expect(ayah.ayahNumber == 116)
        #expect(ayah.juzNumber == 1)
        #expect(ayah.pageNumber == 18)
        #expect((ayah.lineStart ?? 0) > 0)
        #expect(try await api.getAyahById(0) == nil)
        #expect(try await api.getAyahById(6237) == nil)
    }

    @Test func pagesFollowTheMadani1405Images() async throws {
        #expect(try await api.getMaxPage() == 604)
        let anchor = try #require(try await api.getFirstAyahOnPage(15))
        #expect(anchor.pageNumber == 15)
        #expect(try await api.getAyahById(746)?.pageNumber == 121)
    }

    @Test func noStraySpacesInsideWords() async throws {
        // Al-Baqarah 72 has 10 words; a stray U+2009 used to split «فَٱدَّٰرَٰٔتُمۡ».
        let ayah = try #require(try await api.getAyahById(79))
        #expect(ayah.textUthmani.split(whereSeparator: \.isWhitespace).count == 10)
    }

    @Test func search() async throws {
        let results = try await api.search("الرحمن")
        #expect(results.count == 48)
        #expect(results.allSatisfy { $0.textSimple.contains("الرحمن") })
        #expect(results[0].juzNameAr?.isEmpty == false)
        #expect(try await api.search("الله", limit: 5).count == 5)
        #expect(try await api.search("").isEmpty)
        #expect(try await api.search("ـ").isEmpty)
        #expect(try await api.search("فادارأتم").map(\.ayahId) == [79])
        // All tokens must match; earlier matches rank first.
        let both = try await api.search("بسم الرحيم")
        #expect(both.first?.ayahId == 1)
    }

    @Test func missingChapterIdIsRejected() throws {
        let url = try #require(Bundle.module(for: QuranAPI.self).url(forResource: "quran", withExtension: "json"))
        var raw = try JSONDecoder().decode(RawQuran.self, from: Data(contentsOf: url))
        raw.suras[0].ayas[0].chapterId = nil
        #expect(throws: QuranError.self) { try QuranDataStore.build(raw) }
    }

    @Test func truncatedDataIsRejected() throws {
        let url = try #require(Bundle.module(for: QuranAPI.self).url(forResource: "quran", withExtension: "json"))
        var raw = try JSONDecoder().decode(RawQuran.self, from: Data(contentsOf: url))
        raw.suras.removeLast()
        #expect(throws: QuranError.self) { try QuranDataStore.build(raw) }
        raw = try JSONDecoder().decode(RawQuran.self, from: Data(contentsOf: url))
        raw.suras[1].ayas.removeFirst()
        #expect(throws: QuranError.self) { try QuranDataStore.build(raw) }
    }
}

extension Bundle {
    /// The core target's resource bundle, reachable from tests.
    static func module(for _: Any.Type) -> Bundle { TaahudQuranCoreResources.bundle }
}
