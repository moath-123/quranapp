import Foundation

/// Parsed, validated contents of `quran.json` — the same file the Android SDK bundles.
/// Validation rules match `QuranDataMapper` on Android.
struct QuranDataStore: Sendable {
    static let expectedSuras = 114
    static let expectedAyahs = 6236
    static let expectedJuz = 30

    let surahs: [Surah]
    let juz: [Juz]
    let ayahs: [Ayah]              // index = id - 1
    let ayahsByPage: [Int: [Ayah]]
    let normalizedText: [String]   // index = id - 1

    var maxPage: Int { ayahsByPage.keys.max() ?? 0 }

    func ayah(id: Int) -> Ayah? {
        ayahs.indices.contains(id - 1) ? ayahs[id - 1] : nil
    }

    static func load(from url: URL) throws -> QuranDataStore {
        let data = try Data(contentsOf: url)
        let root: RawQuran
        do {
            root = try JSONDecoder().decode(RawQuran.self, from: data)
        } catch {
            throw QuranError.invalidData("quran.json could not be decoded: \(error)")
        }
        return try build(root)
    }

    static func build(_ root: RawQuran) throws -> QuranDataStore {
        var ayahs: [Ayah] = []
        ayahs.reserveCapacity(expectedAyahs)
        for sura in root.suras {
            for a in sura.ayas {
                guard let juz = a.chapterId else {
                    throw QuranError.invalidData("Ayah \(a.id) (\(a.sura):\(a.aya)) has no chapter_id")
                }
                ayahs.append(Ayah(
                    id: a.id, surahNumber: a.sura, ayahNumber: a.aya, juzNumber: juz, pageNumber: a.page,
                    textUthmani: a.text, textSimple: a.ayaText, lineStart: a.lineStart, lineEnd: a.lineEnd))
            }
        }

        let surahs = try root.suras.map { s -> Surah in
            guard let first = s.ayas.first else { throw QuranError.invalidData("Sura \(s.id ?? -1) has no ayahs") }
            return Surah(
                number: s.id ?? first.sura, nameAr: s.nameAr, nameEn: s.nameEn,
                ayahCount: s.ayaNumbers, startPage: s.ayas.map(\.page).min() ?? first.page)
        }

        // Juz start pages come from the ayahs; sura.chapters[].page_number means something else (see F4).
        var names: [Int: (String, String)] = [:]
        for c in (root.chapters ?? []) + root.suras.flatMap(\.chapters) where names[c.id] == nil {
            names[c.id] = (c.nameAr, c.nameEn)
        }
        var juzStart: [Int: Int] = [:]
        for a in ayahs { juzStart[a.juzNumber] = min(juzStart[a.juzNumber] ?? .max, a.pageNumber) }
        let juz = juzStart.keys.sorted().map { id in
            Juz(number: id, nameAr: names[id]?.0 ?? "الجزء \(id)", nameEn: names[id]?.1 ?? "Juz \(id)",
                startPage: juzStart[id]!)
        }

        try validate(surahs: surahs, juz: juz, ayahs: ayahs)

        return QuranDataStore(
            surahs: surahs,
            juz: juz,
            ayahs: ayahs,
            ayahsByPage: Dictionary(grouping: ayahs, by: \.pageNumber),
            normalizedText: ayahs.map { ArabicNormalizer.normalize($0.textSimple) })
    }

    private static func validate(surahs: [Surah], juz: [Juz], ayahs: [Ayah]) throws {
        func check(_ ok: Bool, _ message: @autoclosure () -> String) throws {
            if !ok { throw QuranError.invalidData(message()) }
        }
        try check(surahs.count == expectedSuras, "Expected \(expectedSuras) suras, found \(surahs.count)")
        try check(ayahs.count == expectedAyahs, "Expected \(expectedAyahs) ayahs, found \(ayahs.count)")
        try check(juz.count == expectedJuz, "Expected \(expectedJuz) juz, found \(juz.count)")
        try check(ayahs.map(\.id) == Array(1...expectedAyahs), "Ayah ids are not sequential 1...\(expectedAyahs)")
        var counts: [Int: Int] = [:]
        for a in ayahs { counts[a.surahNumber, default: 0] += 1 }
        for s in surahs {
            try check(counts[s.number] == s.ayahCount,
                      "Sura \(s.number): aya_numbers=\(s.ayahCount) but has \(counts[s.number] ?? 0) ayahs")
        }
        try check(zip(ayahs, ayahs.dropFirst()).allSatisfy { $1.pageNumber >= $0.pageNumber },
                  "Ayah pages are not in reading order")
    }
}

// MARK: - quran.json schema

struct RawQuran: Codable {
    var suras: [RawSura]
    var chapters: [RawChapter]?
}

struct RawSura: Codable {
    var id: Int?
    var nameAr: String
    var nameEn: String
    var ayaNumbers: Int
    var pageNumber: Int?
    var chapters: [RawChapter]
    var ayas: [RawAya]

    enum CodingKeys: String, CodingKey {
        case id, chapters, ayas
        case nameAr = "name_ar", nameEn = "name_en", ayaNumbers = "aya_numbers", pageNumber = "page_number"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(Int.self, forKey: .id)
        nameAr = try c.decode(String.self, forKey: .nameAr)
        nameEn = try c.decode(String.self, forKey: .nameEn)
        ayaNumbers = try c.decode(Int.self, forKey: .ayaNumbers)
        pageNumber = try c.decodeIfPresent(Int.self, forKey: .pageNumber)
        chapters = try c.decodeIfPresent([RawChapter].self, forKey: .chapters) ?? []
        ayas = try c.decodeIfPresent([RawAya].self, forKey: .ayas) ?? []
    }
}

struct RawChapter: Codable {
    var id: Int
    var nameAr: String
    var nameEn: String
    var pageNumber: Int

    enum CodingKeys: String, CodingKey {
        case id
        case nameAr = "name_ar", nameEn = "name_en", pageNumber = "page_number"
    }
}

struct RawAya: Codable {
    var id: Int
    var sura: Int
    var aya: Int
    var chapterId: Int?
    var page: Int
    var lineStart: Int?
    var lineEnd: Int?
    var text: String
    var ayaText: String

    enum CodingKeys: String, CodingKey {
        case id, sura, aya, page, text
        case chapterId = "chapter_id", lineStart = "line_start", lineEnd = "line_end", ayaText = "aya_text"
    }
}
