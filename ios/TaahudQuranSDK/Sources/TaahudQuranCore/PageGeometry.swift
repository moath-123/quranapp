import Foundation
import SQLite3

/// Ayah highlight rectangles and tap hit-testing for the 1024-px Madani (1405) page images,
/// read from `ayahinfo_1024.db`. Rectangles are merged per ayah per line and span the full line
/// height, exactly like `tools/build_web_data.py` produces for the web demo.
final class PageGeometry: @unchecked Sendable {
    static let imageWidth = 1024
    static let imageHeight = 1656

    private let databaseURL: URL
    private let lock = NSLock()
    private var cache: [Int: [AyahRect]] = [:]
    private let ayahIds: [SuraAyah: Int]

    struct SuraAyah: Hashable {
        let sura: Int
        let ayah: Int
    }

    init(databaseURL: URL, ayahs: [Ayah]) {
        self.databaseURL = databaseURL
        var ids: [SuraAyah: Int] = [:]
        for a in ayahs { ids[SuraAyah(sura: a.surahNumber, ayah: a.ayahNumber)] = a.id }
        ayahIds = ids
    }

    func rects(page: Int) throws -> [AyahRect] {
        lock.lock()
        defer { lock.unlock() }
        if let cached = cache[page] { return cached }
        let rects = try loadRects(page: page)
        cache[page] = rects
        return rects
    }

    /// The ayah at (x, y) in 1024-px image coordinates, with a small horizontal tolerance
    /// so taps between words or at line edges still resolve.
    func ayahAt(page: Int, x: Double, y: Double, tolerance: Double = 18) throws -> Int? {
        var best: (id: Int, distance: Double)?
        for r in try rects(page: page) {
            guard y >= Double(r.minY) - 4, y <= Double(r.maxY) + 4 else { continue }
            let distance = x < Double(r.minX) ? Double(r.minX) - x : x > Double(r.maxX) ? x - Double(r.maxX) : 0
            if distance < tolerance, distance < (best?.distance ?? .infinity) {
                best = (r.ayahId, distance)
            }
        }
        return best?.id
    }

    private func loadRects(page: Int) throws -> [AyahRect] {
        var db: OpaquePointer?
        // immutable=1: read-only, never creates -wal/-shm files next to the bundled database.
        let uri = "file:\(databaseURL.path)?mode=ro&immutable=1"
        guard sqlite3_open_v2(uri, &db, SQLITE_OPEN_READONLY | SQLITE_OPEN_URI, nil) == SQLITE_OK else {
            let message = db.map { String(cString: sqlite3_errmsg($0)) } ?? "open failed"
            sqlite3_close(db)
            throw QuranError.database(message)
        }
        defer { sqlite3_close(db) }

        let bands = try query(db, """
            SELECT line_number, MIN(min_y), MAX(max_y) FROM glyphs WHERE page_number = ? GROUP BY line_number
            """, page: page) { row in (row[0], (row[1], row[2])) }
        let lineBand = Dictionary(bands, uniquingKeysWith: { first, _ in first })

        return try query(db, """
            SELECT line_number, sura_number, ayah_number, MIN(min_x), MAX(max_x) FROM glyphs
            WHERE page_number = ? GROUP BY line_number, sura_number, ayah_number
            ORDER BY line_number, MIN(min_x) DESC
            """, page: page) { row -> AyahRect? in
            guard let id = ayahIds[SuraAyah(sura: row[1], ayah: row[2])], let band = lineBand[row[0]] else { return nil }
            return AyahRect(ayahId: id, minX: row[3], minY: band.0, maxX: row[4], maxY: band.1)
        }.compactMap { $0 }
    }

    private func query<T>(_ db: OpaquePointer?, _ sql: String, page: Int, map: ([Int]) -> T) throws -> [T] {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else {
            throw QuranError.database(String(cString: sqlite3_errmsg(db)))
        }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_int(statement, 1, Int32(page))
        var out: [T] = []
        let columns = sqlite3_column_count(statement)
        while true {
            let step = sqlite3_step(statement)
            if step == SQLITE_DONE { break }
            guard step == SQLITE_ROW else { throw QuranError.database(String(cString: sqlite3_errmsg(db))) }
            out.append(map((0..<columns).map { Int(sqlite3_column_int64(statement, $0)) }))
        }
        return out
    }
}
