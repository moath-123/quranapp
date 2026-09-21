import Foundation
import Testing
@testable import TaahudQuranCore

/// The events hosts rely on for pilot metrics (docs/pilot-checklist.md).
struct QuranEventTests {
    let installer: PagesInstallerTests

    init() throws {
        installer = try PagesInstallerTests()
    }

    func recordingConfig(archive: URL, retries: Int = 0, log: EventRecorder) -> QuranConfig {
        var config = installer.config(archive: archive)
        config.downloadRetries = retries
        config.onEvent = { log.append($0) }
        return config
    }

    @Test func dataLoadIsReported() async throws {
        let log = EventRecorder()
        let api = QuranAPI(config: QuranConfig(onEvent: { log.append($0) }))
        try await api.initialize()
        try await api.initialize() // loaded once
        let loads = log.values.filter { if case .dataLoaded = $0 { true } else { false } }
        #expect(loads.count == 1)
        #expect(log.values.first?.name == "quran_data_loaded")
    }

    @Test func successfulInstallReportsStartAndResult() async throws {
        let log = EventRecorder()
        let api = QuranAPI(config: recordingConfig(archive: try installer.makeArchive(named: "p.zip"), log: log))
        try await api.ensurePagesInstalled()

        #expect(log.values.count == 2)
        #expect(log.values[0] == .pagesInstallStarted(version: 1, host: "local"))
        guard case .pagesInstalled(let version, let seconds, let bytes, let attempts) = log.values[1] else {
            Issue.record("expected pagesInstalled, got \(log.values)")
            return
        }
        #expect(version == 1 && attempts == 1 && bytes > 0 && seconds >= 0)
        #expect(log.values[1].parameters["attempts"] == "1")

        try await api.ensurePagesInstalled()
        #expect(log.values.last == .pagesAlreadyInstalled)
    }

    @Test func retriesAndFailureAreReported() async throws {
        let log = EventRecorder()
        let missing = installer.root.appendingPathComponent("missing.zip")
        let api = QuranAPI(config: recordingConfig(archive: missing, retries: 1, log: log))
        await #expect(throws: QuranError.self) { try await api.ensurePagesInstalled() }

        #expect(log.values.map(\.name) == [
            "quran_pages_install_started", "quran_pages_download_retry", "quran_pages_install_failed",
        ])
        guard case .pagesInstallFailed(_, let error, _, let attempts) = log.values.last else { return }
        #expect(error.code == "download_failed")
        #expect(attempts == 2)
    }

    @Test func checksumFailureHasItsOwnCode() async throws {
        let log = EventRecorder()
        var config = recordingConfig(archive: try installer.makeArchive(named: "c.zip"), log: log)
        config.pagesArchiveSHA256 = String(repeating: "a", count: 64)
        let api = QuranAPI(config: config)
        await #expect(throws: QuranError.self) { try await api.ensurePagesInstalled() }
        #expect(log.values.last?.parameters["error"] == "checksum_mismatch")
    }
}

final class EventRecorder: @unchecked Sendable {
    private let lock = NSLock()
    private var storage: [QuranEvent] = []
    var values: [QuranEvent] { lock.lock(); defer { lock.unlock() }; return storage }
    func append(_ event: QuranEvent) { lock.lock(); storage.append(event); lock.unlock() }
}
