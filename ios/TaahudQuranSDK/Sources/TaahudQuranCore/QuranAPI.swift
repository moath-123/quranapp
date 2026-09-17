import Foundation

/// Entry point of the SDK. Mirrors `QuranApi` on Android (same functions, same models, same data).
///
/// ```swift
/// let quran = QuranAPI()
/// try await quran.initialize()
/// let surahs = try await quran.getSurahs()
/// try await quran.ensurePagesInstalled { progress in … }
/// ```
public actor QuranAPI {
    public nonisolated let config: QuranConfig
    private let resources: Bundle
    private let installer: PagesInstaller
    private var store: QuranDataStore?
    private var geometry: PageGeometry?

    public init(config: QuranConfig = QuranConfig()) {
        self.init(config: config, resources: .module, session: .shared)
    }

    init(config: QuranConfig, resources: Bundle, session: URLSession) {
        self.config = config
        self.resources = resources
        installer = PagesInstaller(config: config, session: session)
    }

    // MARK: - Data (parity with Android)

    /// Loads and validates the bundled data. Safe to call multiple times.
    public func initialize() throws {
        _ = try loadedStore()
    }

    public func getSurahs() throws -> [Surah] { try loadedStore().surahs }

    public func getJuzList() throws -> [Juz] { try loadedStore().juz }

    public func search(_ query: String, limit: Int = 200) throws -> [SearchResult] {
        QuranSearch.search(query, limit: limit, in: try loadedStore())
    }

    public func getAyahsByPage(_ page: Int) throws -> [Ayah] {
        try loadedStore().ayahsByPage[page] ?? []
    }

    public func getAyahsBySurah(_ number: Int) throws -> [Ayah] {
        try loadedStore().ayahs.filter { $0.surahNumber == number }
    }

    public func getAyahsByJuz(_ number: Int) throws -> [Ayah] {
        try loadedStore().ayahs.filter { $0.juzNumber == number }
    }

    public func getAyahById(_ id: Int) throws -> Ayah? {
        try loadedStore().ayah(id: id)
    }

    public func getFirstAyahOnPage(_ page: Int) throws -> Ayah? {
        try loadedStore().ayahsByPage[page]?.first
    }

    public func getMaxPage() throws -> Int { try loadedStore().maxPage }

    // MARK: - Mushaf pages

    public nonisolated var arePagesInstalled: Bool { installer.isInstalled }

    /// Downloads and installs the page images if needed (first launch). Safe to call multiple times.
    public func ensurePagesInstalled(progress: (@Sendable (PagesInstallProgress) -> Void)? = nil) async throws {
        try await installer.install(progress: progress)
    }

    /// Local file URL of a page image (valid once `arePagesInstalled` is true).
    public nonisolated func pageImageURL(_ page: Int) -> URL { installer.pageURL(page) }

    /// Highlight rectangles on `page`, in 1024×1656 image coordinates.
    public func ayahRects(page: Int) throws -> [AyahRect] {
        try loadedGeometry().rects(page: page)
    }

    /// The ayah under a point given in 1024×1656 image coordinates, if any.
    public func ayahAt(page: Int, x: Double, y: Double) throws -> Ayah? {
        guard let id = try loadedGeometry().ayahAt(page: page, x: x, y: y) else { return nil }
        return try getAyahById(id)
    }

    public static let pageImageSize = (width: PageGeometry.imageWidth, height: PageGeometry.imageHeight)

    // MARK: - Loading

    private func loadedStore() throws -> QuranDataStore {
        if let store { return store }
        guard let url = resources.url(forResource: "quran", withExtension: "json") else {
            throw QuranError.missingResource("quran.json")
        }
        let loaded = try QuranDataStore.load(from: url)
        store = loaded
        return loaded
    }

    private func loadedGeometry() throws -> PageGeometry {
        if let geometry { return geometry }
        guard let url = resources.url(forResource: "ayahinfo_1024", withExtension: "db") else {
            throw QuranError.missingResource("ayahinfo_1024.db")
        }
        let loaded = PageGeometry(databaseURL: url, ayahs: try loadedStore().ayahs)
        geometry = loaded
        return loaded
    }
}

/// Resource bundle of this module (used by tests).
enum TaahudQuranCoreResources {
    static var bundle: Bundle { .module }
}
