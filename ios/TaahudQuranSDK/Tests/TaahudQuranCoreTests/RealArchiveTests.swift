import Foundation
import ImageIO
import Testing
@testable import TaahudQuranCore

/// Downloads the real Quran.com archive (~63 MB) through URLSession and installs it.
/// Opt-in: `TAAHUD_NETWORK_TESTS=1 ./run-tests.sh --filter RealArchiveTests`
@Suite(.enabled(if: ProcessInfo.processInfo.environment["TAAHUD_NETWORK_TESTS"] == "1"))
struct RealArchiveTests {

    @Test
    func downloadsAndInstallsTheDefaultArchive() async throws {
        let storage = FileManager.default.temporaryDirectory
            .appendingPathComponent("TaahudQuranNetwork-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: storage) }
        let api = QuranAPI(config: QuranConfig(storageDirectory: storage, downloadRetries: 1))

        let events = EventLog()
        let started = Date()
        try await api.ensurePagesInstalled { events.append($0) }
        let seconds = Date().timeIntervalSince(started)

        #expect(api.arePagesInstalled)
        let fractions = events.values.compactMap { if case .downloading(let f) = $0 { f } else { nil } }
        #expect(fractions.last == 1.0)
        #expect(fractions.count > 2, "download progress should be reported")

        // Every page exists and decodes as a 1024×1656 image.
        for page in [1, 2, 121, 302, 604] {
            let source = try #require(CGImageSourceCreateWithURL(api.pageImageURL(page) as CFURL, nil))
            let props = try #require(CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any])
            #expect(props[kCGImagePropertyPixelWidth] as? Int == 1024)
            #expect(props[kCGImagePropertyPixelHeight] as? Int == 1656)
        }
        let installed = try FileManager.default.contentsOfDirectory(atPath: api.pageImageURL(1).deletingLastPathComponent().path)
        #expect(installed.filter { $0.hasSuffix(".png") }.count == 604)
        print("REAL_ARCHIVE seconds=\(Int(seconds)) progressEvents=\(fractions.count)")
    }
}

/// Installs an archive produced by `tools/package_pages.sh` with its checksum pinned.
/// Opt-in: `TAAHUD_PACKAGED_ARCHIVE=dist/pages/images_1024-v1.zip TAAHUD_PACKAGED_SHA=<sha> ./run-tests.sh --filter PackagedArchiveTests`
@Suite(.enabled(if: ProcessInfo.processInfo.environment["TAAHUD_PACKAGED_ARCHIVE"] != nil))
struct PackagedArchiveTests {

    @Test func installsThePackagedArchiveWithPinnedChecksum() async throws {
        let env = ProcessInfo.processInfo.environment
        let archive = URL(fileURLWithPath: try #require(env["TAAHUD_PACKAGED_ARCHIVE"]))
        let sha = try #require(env["TAAHUD_PACKAGED_SHA"])
        let storage = FileManager.default.temporaryDirectory
            .appendingPathComponent("TaahudQuranPackaged-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: storage) }

        let api = QuranAPI(config: QuranConfig(pagesArchiveURL: archive, pagesArchiveSHA256: sha, storageDirectory: storage))
        try await api.ensurePagesInstalled()
        #expect(api.arePagesInstalled)
        let source = try #require(CGImageSourceCreateWithURL(api.pageImageURL(604) as CFURL, nil))
        #expect(CGImageSourceGetCount(source) == 1)
    }
}
