import Foundation

/// SDK settings. Defaults are safe; hosts override what they need (e.g. their own image server).
public struct QuranConfig: Sendable {
    /// Zip containing `width_1024/page001.png` … `page604.png` (Madani 1405 edition).
    public var pagesArchiveURL: URL
    /// Lower-case hex SHA-256 of the archive. When set, any other file is rejected.
    public var pagesArchiveSHA256: String?
    /// Where page images are installed. Excluded from iCloud backup.
    public var storageDirectory: URL
    /// Bump when the archive contents change to force a re-install.
    public var pagesVersion: Int
    /// Extra download attempts after the first failure (with increasing delay).
    public var downloadRetries: Int
    /// Receives SDK events for the host's analytics / crash reporting. Called on a background thread.
    public var onEvent: (@Sendable (QuranEvent) -> Void)?

    public init(
        pagesArchiveURL: URL = URL(string: "https://files.quran.app/hafs/madani/zips/images_1024.zip")!,
        pagesArchiveSHA256: String? = nil,
        storageDirectory: URL = QuranConfig.defaultStorageDirectory,
        pagesVersion: Int = 1,
        downloadRetries: Int = 3,
        onEvent: (@Sendable (QuranEvent) -> Void)? = nil
    ) {
        self.pagesArchiveURL = pagesArchiveURL
        self.pagesArchiveSHA256 = pagesArchiveSHA256
        self.storageDirectory = storageDirectory
        self.pagesVersion = pagesVersion
        self.downloadRetries = downloadRetries
        self.onEvent = onEvent
    }

    public static var defaultStorageDirectory: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("TaahudQuran", isDirectory: true)
    }
}
