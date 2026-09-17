import Foundation
import Testing
import ZIPFoundation
@testable import TaahudQuranCore

/// Installer behaviour with local archives (no network): success, idempotency,
/// checksum rejection, incomplete archives, and keeping a previous install intact.
struct PagesInstallerTests {
    let root: URL

    init() throws {
        root = FileManager.default.temporaryDirectory
            .appendingPathComponent("TaahudQuranTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
    }

    /// Builds a zip shaped like Quran.com's images_1024.zip (pages inside `width_1024/`).
    func makeArchive(named name: String, skipping skipped: Set<Int> = [], marker: String = "v1") throws -> URL {
        let url = root.appendingPathComponent(name)
        let archive = try Archive(url: url, accessMode: .create)
        try archive.addEntry(
            with: "width_1024/", type: .directory, uncompressedSize: Int64(0),
            provider: { (_: Int64, _: Int) -> Data in Data() })
        for page in 1...604 where !skipped.contains(page) {
            let data = Data("\(marker)-page-\(page)".utf8)
            try archive.addEntry(
                with: String(format: "width_1024/page%03d.png", page), type: .file,
                uncompressedSize: Int64(data.count),
                provider: { (position: Int64, size: Int) -> Data in data.subdata(in: Int(position)..<Int(position) + size) })
        }
        return url
    }

    func config(archive: URL, sha: String? = nil, version: Int = 1) -> QuranConfig {
        QuranConfig(
            pagesArchiveURL: archive, pagesArchiveSHA256: sha,
            storageDirectory: root.appendingPathComponent("storage", isDirectory: true),
            pagesVersion: version, downloadRetries: 0)
    }

    func contents(_ url: URL) throws -> String { String(decoding: try Data(contentsOf: url), as: UTF8.self) }

    @Test func installsAllPagesAndReportsProgress() async throws {
        let api = QuranAPI(config: config(archive: try makeArchive(named: "pages.zip")))
        #expect(!api.arePagesInstalled)

        let events = EventLog()
        try await api.ensurePagesInstalled { events.append($0) }

        #expect(api.arePagesInstalled)
        #expect(try contents(api.pageImageURL(1)) == "v1-page-1")
        #expect(try contents(api.pageImageURL(604)) == "v1-page-604")
        #expect(events.values.last == .installed)
        #expect(events.values.contains(.extracting))
        let leftovers = try FileManager.default.contentsOfDirectory(atPath: api.config.storageDirectory.path)
        #expect(!leftovers.contains { $0.hasPrefix("tmp-") })
    }

    @Test func secondCallDoesNothing() async throws {
        let archive = try makeArchive(named: "pages.zip")
        let api = QuranAPI(config: config(archive: archive))
        try await api.ensurePagesInstalled()
        try FileManager.default.removeItem(at: archive) // would fail if it tried to install again
        let events = EventLog()
        try await api.ensurePagesInstalled { events.append($0) }
        #expect(events.values == [.installed])
    }

    @Test func matchingChecksumIsAccepted() async throws {
        let archive = try makeArchive(named: "pages.zip")
        let sha = try PagesInstaller.sha256(of: archive)
        let api = QuranAPI(config: config(archive: archive, sha: sha.uppercased()))
        let events = EventLog()
        try await api.ensurePagesInstalled { events.append($0) }
        #expect(api.arePagesInstalled)
        #expect(events.values.contains(.verifying))
    }

    @Test func wrongChecksumIsRejectedAndNothingIsInstalled() async throws {
        let api = QuranAPI(config: config(archive: try makeArchive(named: "pages.zip"), sha: String(repeating: "0", count: 64)))
        await #expect(throws: QuranError.self) { try await api.ensurePagesInstalled() }
        #expect(!api.arePagesInstalled)
        #expect(!FileManager.default.fileExists(atPath: api.pageImageURL(1).path))
    }

    @Test func incompleteArchiveIsRejected() async throws {
        let api = QuranAPI(config: config(archive: try makeArchive(named: "partial.zip", skipping: [300, 604])))
        do {
            try await api.ensurePagesInstalled()
            Issue.record("expected incompleteArchive")
        } catch let error as QuranError {
            #expect(error == .incompleteArchive(missingPages: [300, 604]))
        }
        #expect(!api.arePagesInstalled)
    }

    @Test func corruptArchiveIsRejected() async throws {
        let bogus = root.appendingPathComponent("bogus.zip")
        try Data("not a zip".utf8).write(to: bogus)
        let api = QuranAPI(config: config(archive: bogus))
        await #expect(throws: (any Error).self) { try await api.ensurePagesInstalled() }
        #expect(!api.arePagesInstalled)
    }

    @Test func failedUpgradeKeepsThePreviousVersion() async throws {
        let v1 = QuranAPI(config: config(archive: try makeArchive(named: "v1.zip"), version: 1))
        try await v1.ensurePagesInstalled()

        let v2 = QuranAPI(config: config(archive: try makeArchive(named: "v2.zip", skipping: [7], marker: "v2"), version: 2))
        await #expect(throws: QuranError.self) { try await v2.ensurePagesInstalled() }
        #expect(!v2.arePagesInstalled)
        #expect(v1.arePagesInstalled)
        #expect(try contents(v1.pageImageURL(7)) == "v1-page-7")
    }

    @Test func successfulUpgradeReplacesTheOldVersion() async throws {
        let v1 = QuranAPI(config: config(archive: try makeArchive(named: "v1.zip"), version: 1))
        try await v1.ensurePagesInstalled()
        let v2 = QuranAPI(config: config(archive: try makeArchive(named: "v2.zip", marker: "v2"), version: 2))
        try await v2.ensurePagesInstalled()

        #expect(try contents(v2.pageImageURL(7)) == "v2-page-7")
        #expect(!v1.arePagesInstalled) // old version cleaned up
    }

    @Test func missingLocalArchiveFailsCleanly() async throws {
        let api = QuranAPI(config: config(archive: root.appendingPathComponent("nope.zip")))
        await #expect(throws: QuranError.self) { try await api.ensurePagesInstalled() }
        #expect(!api.arePagesInstalled)
    }
}

final class EventLog: @unchecked Sendable {
    private let lock = NSLock()
    private var storage: [PagesInstallProgress] = []
    var values: [PagesInstallProgress] { lock.lock(); defer { lock.unlock() }; return storage }
    func append(_ value: PagesInstallProgress) { lock.lock(); storage.append(value); lock.unlock() }
}
