import Foundation

/// Events the SDK reports so the host app can forward them to its analytics / crash reporting
/// (pilot metrics in docs/pilot-checklist.md). Set `QuranConfig.onEvent`; nothing is sent anywhere by the SDK.
public enum QuranEvent: Equatable, Sendable {
    /// Bundled data loaded and validated.
    case dataLoaded(milliseconds: Int)
    /// Bundled data failed validation (should never happen in a released build).
    case dataFailed(QuranError)
    /// Page images were already installed; nothing to do.
    case pagesAlreadyInstalled
    /// A page-image installation started (first launch, or a new `pagesVersion`).
    case pagesInstallStarted(version: Int, host: String)
    /// A download attempt failed and will be retried.
    case pagesDownloadRetry(attempt: Int, error: String)
    /// Installation finished. `downloadBytes` is the archive size.
    case pagesInstalled(version: Int, seconds: Double, downloadBytes: Int64, attempts: Int)
    /// Installation failed; the device is left clean and the host can offer a retry.
    case pagesInstallFailed(version: Int, error: QuranError, seconds: Double, attempts: Int)

    /// Stable name for analytics dashboards.
    public var name: String {
        switch self {
        case .dataLoaded: return "quran_data_loaded"
        case .dataFailed: return "quran_data_failed"
        case .pagesAlreadyInstalled: return "quran_pages_already_installed"
        case .pagesInstallStarted: return "quran_pages_install_started"
        case .pagesDownloadRetry: return "quran_pages_download_retry"
        case .pagesInstalled: return "quran_pages_installed"
        case .pagesInstallFailed: return "quran_pages_install_failed"
        }
    }

    /// Flat key/value parameters (strings and numbers only) for analytics SDKs.
    public var parameters: [String: String] {
        switch self {
        case .dataLoaded(let ms):
            return ["ms": "\(ms)"]
        case .dataFailed(let error):
            return ["error": error.code]
        case .pagesAlreadyInstalled:
            return [:]
        case .pagesInstallStarted(let version, let host):
            return ["version": "\(version)", "host": host]
        case .pagesDownloadRetry(let attempt, let error):
            return ["attempt": "\(attempt)", "error": error]
        case .pagesInstalled(let version, let seconds, let bytes, let attempts):
            return ["version": "\(version)", "seconds": String(format: "%.1f", seconds),
                    "bytes": "\(bytes)", "attempts": "\(attempts)"]
        case .pagesInstallFailed(let version, let error, let seconds, let attempts):
            return ["version": "\(version)", "error": error.code, "seconds": String(format: "%.1f", seconds),
                    "attempts": "\(attempts)"]
        }
    }
}

extension QuranError {
    /// Short stable code for dashboards (`download_failed`, `checksum_mismatch`, …).
    public var code: String {
        switch self {
        case .invalidData: return "invalid_data"
        case .missingResource: return "missing_resource"
        case .pagesNotInstalled: return "pages_not_installed"
        case .checksumMismatch: return "checksum_mismatch"
        case .incompleteArchive: return "incomplete_archive"
        case .downloadFailed: return "download_failed"
        case .database: return "database"
        }
    }
}
