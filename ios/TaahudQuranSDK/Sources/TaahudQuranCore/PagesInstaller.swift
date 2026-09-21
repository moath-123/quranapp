import CryptoKit
import Foundation
import ZIPFoundation

public enum PagesInstallProgress: Equatable, Sendable {
    case downloading(fraction: Double)
    case verifying
    case extracting
    case installed
}

/// Downloads and installs the mushaf page images atomically:
/// download to a temp dir → optional SHA-256 check → unzip → verify all 604 pages →
/// move into `pages-v{N}` → write `installed-v{N}`. Any failure removes the temp files and
/// leaves a previous installation untouched (same success-marker idea as quran-ios).
actor PagesInstaller {
    static let pageCount = 604

    private let config: QuranConfig
    private let session: URLSession
    private let fileManager = FileManager.default

    init(config: QuranConfig, session: URLSession = .shared) {
        self.config = config
        self.session = session
    }

    nonisolated var pagesDirectory: URL {
        config.storageDirectory.appendingPathComponent("pages-v\(config.pagesVersion)", isDirectory: true)
    }

    nonisolated var markerURL: URL {
        config.storageDirectory.appendingPathComponent("installed-v\(config.pagesVersion)")
    }

    nonisolated func pageURL(_ page: Int) -> URL {
        pagesDirectory.appendingPathComponent(String(format: "page%03d.png", locale: Locale(identifier: "en_US_POSIX"), page))
    }

    nonisolated var isInstalled: Bool {
        let fm = FileManager.default
        return fm.fileExists(atPath: markerURL.path)
            && fm.fileExists(atPath: pageURL(1).path)
            && fm.fileExists(atPath: pageURL(Self.pageCount).path)
    }

    func install(progress: (@Sendable (PagesInstallProgress) -> Void)?) async throws {
        if isInstalled {
            config.onEvent?(.pagesAlreadyInstalled)
            progress?(.installed)
            return
        }
        let version = config.pagesVersion
        config.onEvent?(.pagesInstallStarted(version: version, host: config.pagesArchiveURL.host ?? "local"))
        let started = Date()
        attempts = 0
        do {
            let bytes = try await performInstall(progress: progress)
            config.onEvent?(.pagesInstalled(
                version: version, seconds: Date().timeIntervalSince(started), downloadBytes: bytes, attempts: attempts))
        } catch {
            if !(error is CancellationError) {
                let reported = (error as? QuranError) ?? .downloadFailed(error.localizedDescription)
                config.onEvent?(.pagesInstallFailed(
                    version: version, error: reported, seconds: Date().timeIntervalSince(started), attempts: attempts))
            }
            throw error
        }
    }

    private var attempts = 0

    /// Returns the downloaded archive size in bytes.
    private func performInstall(progress: (@Sendable (PagesInstallProgress) -> Void)?) async throws -> Int64 {
        try fileManager.createDirectory(at: config.storageDirectory, withIntermediateDirectories: true)
        let work = config.storageDirectory.appendingPathComponent("tmp-\(UUID().uuidString)", isDirectory: true)
        try fileManager.createDirectory(at: work, withIntermediateDirectories: true)
        defer { try? fileManager.removeItem(at: work) }

        let archive = work.appendingPathComponent("pages.zip")
        try await downloadWithRetries(to: archive) { progress?(.downloading(fraction: $0)) }
        let bytes = (try? fileManager.attributesOfItem(atPath: archive.path)[.size] as? Int64) ?? 0

        if let expected = config.pagesArchiveSHA256?.lowercased() {
            progress?(.verifying)
            let actual = try Self.sha256(of: archive)
            guard actual == expected else { throw QuranError.checksumMismatch(expected: expected, actual: actual) }
        }

        progress?(.extracting)
        let extracted = work.appendingPathComponent("extracted", isDirectory: true)
        do {
            try fileManager.unzipItem(at: archive, to: extracted)
        } catch {
            throw QuranError.downloadFailed("Archive could not be extracted: \(error.localizedDescription)")
        }
        let source = try locatePagesFolder(in: extracted)
        let missing = (1...Self.pageCount).filter {
            !fileManager.fileExists(atPath: source.appendingPathComponent(String(format: "page%03d.png", $0)).path)
        }
        guard missing.isEmpty else { throw QuranError.incompleteArchive(missingPages: missing) }

        // Swap into place, then mark as installed. The marker is written last, so a crash before it
        // simply causes a clean re-install next time.
        if fileManager.fileExists(atPath: pagesDirectory.path) {
            try fileManager.removeItem(at: pagesDirectory)
        }
        try fileManager.moveItem(at: source, to: pagesDirectory)
        try Data("\(Date())".utf8).write(to: markerURL, options: .atomic)
        try excludeFromBackup(config.storageDirectory)
        removeOlderVersions()
        progress?(.installed)
        return bytes
    }

    // MARK: - Download

    private func downloadWithRetries(to destination: URL, progress: @escaping @Sendable (Double) -> Void) async throws {
        var attempt = 0
        while true {
            attempts += 1
            do {
                try await downloadOnce(config.pagesArchiveURL, to: destination, progress: progress)
                return
            } catch {
                if error is CancellationError || attempt >= config.downloadRetries { throw error }
                attempt += 1
                config.onEvent?(.pagesDownloadRetry(attempt: attempt, error: error.localizedDescription))
                try await Task.sleep(nanoseconds: UInt64(attempt) * 1_000_000_000)
            }
        }
    }

    private func downloadOnce(_ url: URL, to destination: URL, progress: @escaping @Sendable (Double) -> Void) async throws {
        try? fileManager.removeItem(at: destination)
        if url.isFileURL {
            // Bundled or pre-downloaded archive.
            guard fileManager.fileExists(atPath: url.path) else {
                throw QuranError.downloadFailed("No archive at \(url.path)")
            }
            try fileManager.copyItem(at: url, to: destination)
            progress(1)
            return
        }

        let session = self.session
        let box = DownloadBox()
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                let task = session.downloadTask(with: url) { location, response, error in
                    defer { box.observation = nil }
                    if let error {
                        continuation.resume(throwing: (error as? URLError)?.code == .cancelled
                            ? CancellationError() : QuranError.downloadFailed(error.localizedDescription))
                        return
                    }
                    if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                        continuation.resume(throwing: QuranError.downloadFailed("HTTP \(http.statusCode)"))
                        return
                    }
                    guard let location else {
                        continuation.resume(throwing: QuranError.downloadFailed("No file received"))
                        return
                    }
                    do {
                        // The temporary file is deleted when this handler returns, so move it now.
                        try FileManager.default.moveItem(at: location, to: destination)
                        continuation.resume()
                    } catch {
                        continuation.resume(throwing: error)
                    }
                }
                box.task = task
                box.observation = task.progress.observe(\.fractionCompleted) { p, _ in progress(p.fractionCompleted) }
                task.resume()
            }
        } onCancel: {
            box.task?.cancel()
        }
    }

    private final class DownloadBox: @unchecked Sendable {
        var task: URLSessionDownloadTask?
        var observation: NSKeyValueObservation?
    }

    // MARK: - Helpers

    static func sha256(of file: URL) throws -> String {
        let handle = try FileHandle(forReadingFrom: file)
        defer { try? handle.close() }
        var hasher = SHA256()
        while true {
            let chunk = handle.readData(ofLength: 1 << 20)
            if chunk.isEmpty { break }
            hasher.update(data: chunk)
        }
        return hasher.finalize().map { String(format: "%02x", $0) }.joined()
    }

    /// The archive from Quran.com keeps pages in `width_1024/`; accept any folder that has page001.png.
    private func locatePagesFolder(in root: URL) throws -> URL {
        if fileManager.fileExists(atPath: root.appendingPathComponent("page001.png").path) { return root }
        let enumerator = fileManager.enumerator(at: root, includingPropertiesForKeys: nil)
        while let url = enumerator?.nextObject() as? URL {
            if url.lastPathComponent == "page001.png" { return url.deletingLastPathComponent() }
        }
        throw QuranError.incompleteArchive(missingPages: Array(1...Self.pageCount))
    }

    private func excludeFromBackup(_ url: URL) throws {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var url = url
        try url.setResourceValues(values)
    }

    private func removeOlderVersions() {
        let items = (try? fileManager.contentsOfDirectory(atPath: config.storageDirectory.path)) ?? []
        for name in items {
            let isOldPages = name.hasPrefix("pages-v") && name != pagesDirectory.lastPathComponent
            let isOldMarker = name.hasPrefix("installed-v") && name != markerURL.lastPathComponent
            if isOldPages || isOldMarker {
                try? fileManager.removeItem(at: config.storageDirectory.appendingPathComponent(name))
            }
        }
    }
}
