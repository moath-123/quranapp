import Foundation

/// A sura. Mirrors `com.codesteem.quransdk.api.model.Surah` on Android.
public struct Surah: Hashable, Sendable {
    public let number: Int
    public let nameAr: String
    public let nameEn: String
    public let ayahCount: Int
    public let startPage: Int
}

/// A juz (part). Mirrors `Juz` on Android.
public struct Juz: Hashable, Sendable {
    public let number: Int
    public let nameAr: String
    public let nameEn: String
    public let startPage: Int
}

/// An ayah. `id` is the 1-based position in the mushaf (1...6236). Mirrors `Ayah` on Android.
public struct Ayah: Hashable, Sendable {
    public let id: Int
    public let surahNumber: Int
    public let ayahNumber: Int
    public let juzNumber: Int
    public let pageNumber: Int
    public let textUthmani: String
    public let textSimple: String
    public let lineStart: Int?
    public let lineEnd: Int?
}

/// A search hit. Mirrors `SearchResult` on Android.
public struct SearchResult: Hashable, Sendable {
    public let ayahId: Int
    public let ayahNumber: Int
    public let surahNumber: Int
    public let surahNameAr: String
    public let surahNameEn: String
    public let textUthmani: String
    public let textSimple: String
    public let pageNumber: Int
    public let juzNumber: Int
    public let juzNameAr: String?
    public let juzNameEn: String?
}

/// Highlight rectangle of an ayah on one line of a page image, in 1024-px image coordinates.
public struct AyahRect: Hashable, Sendable {
    public let ayahId: Int
    public let minX: Int
    public let minY: Int
    public let maxX: Int
    public let maxY: Int

    public var width: Int { maxX - minX }
    public var height: Int { maxY - minY }
}

public enum QuranError: Error, Equatable, LocalizedError {
    /// Bundled data is incomplete or inconsistent. Mirrors `InvalidQuranDataException` on Android.
    case invalidData(String)
    /// A bundled resource is missing from the package.
    case missingResource(String)
    /// Page images are not installed yet — call `ensurePagesInstalled`.
    case pagesNotInstalled
    /// The downloaded archive did not match `QuranConfig.pagesArchiveSHA256`.
    case checksumMismatch(expected: String, actual: String)
    /// The archive was extracted but does not contain all pages.
    case incompleteArchive(missingPages: [Int])
    case downloadFailed(String)
    case database(String)

    public var errorDescription: String? {
        switch self {
        case .invalidData(let message): return "Invalid Quran data: \(message)"
        case .missingResource(let name): return "Missing bundled resource: \(name)"
        case .pagesNotInstalled: return "Mushaf page images are not installed"
        case .checksumMismatch(let expected, let actual): return "Checksum mismatch (expected \(expected), got \(actual))"
        case .incompleteArchive(let missing): return "Archive is missing \(missing.count) pages (e.g. \(missing.prefix(5)))"
        case .downloadFailed(let message): return "Download failed: \(message)"
        case .database(let message): return "Database error: \(message)"
        }
    }
}
