import os
import SwiftUI
import TaahudQuranCore

@main
struct TaahudQuranExampleApp: App {
    @StateObject private var model = ExampleModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .environment(\.layoutDirection, .rightToLeft)
                .task { await model.start() }
        }
    }
}

/// Server config, same shape as `GET /api/quran/app-config` in server/laravel-kit.
struct AppConfig: Codable, Equatable {
    struct Pages: Codable, Equatable {
        var url: URL
        var sha256: String?
        var version: Int
    }

    var flags: [String: Bool]
    var pages: Pages?

    var newMushafEnabled: Bool { flags["new_mushaf"] ?? false }

    /// Used when no config URL is set (demo mode): new mushaf on, Quran.com images.
    static let demo = AppConfig(flags: ["new_mushaf": true], pages: nil)
}

/// App state for the example. Launch arguments (for testing):
///   -configURL https://…/api/quran/app-config   fetch flags + page-image settings from the server
///   -resetPages YES            delete installed page images first
///   -pagesArchive /path.zip    install from a local archive instead of Quran.com
///   -startPage 50              open the mushaf on a given page
///   -autoInstall YES           start the download immediately
@MainActor
final class ExampleModel: ObservableObject {
    enum InstallState: Equatable {
        case checking
        case needed
        case downloading(Double)
        case preparing
        case ready
        case failed(String)
    }

    private(set) var quran: QuranAPI!
    @Published var appConfig: AppConfig?
    @Published var installState: InstallState = .checking
    @Published var surahs: [Surah] = []
    @Published var juz: [Juz] = []
    @Published var dataError: String?
    @Published var events: [(date: Date, name: String, detail: String)] = []

    private let defaults = UserDefaults.standard
    private let logger = Logger(subsystem: "com.taahud.quranexample", category: "quran")

    var startPage: Int {
        let arg = defaults.integer(forKey: "startPage")
        if arg > 0 { return arg }
        let saved = defaults.integer(forKey: "lastPage")
        return saved > 0 ? saved : 1
    }

    func savePage(_ page: Int) { defaults.set(page, forKey: "lastPage") }

    func start() async {
        let config = await loadAppConfig()
        appConfig = config
        quran = QuranAPI(config: makeQuranConfig(from: config))
        guard config.newMushafEnabled else { return } // legacy mushaf; nothing to load
        do {
            try await quran.initialize()
            surahs = try await quran.getSurahs()
            juz = try await quran.getJuzList()
        } catch {
            dataError = error.localizedDescription
            return
        }
        installState = quran.arePagesInstalled ? .ready : .needed
        if installState == .needed, defaults.bool(forKey: "autoInstall") {
            await install()
        }
    }

    /// Remote config with a cached fallback. With a config URL but no network and no cache, the new mushaf
    /// stays off (safe default); without a config URL the demo config is used.
    private func loadAppConfig() async -> AppConfig {
        guard let raw = defaults.string(forKey: "configURL"), let url = URL(string: raw) else { return .demo }
        do {
            var request = URLRequest(url: url, timeoutInterval: 8)
            request.setValue("application/json", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let config = try JSONDecoder().decode(AppConfig.self, from: data)
            defaults.set(data, forKey: "cachedAppConfig")
            record(name: "app_config_loaded", detail: "new_mushaf=\(config.newMushafEnabled)")
            return config
        } catch {
            record(name: "app_config_failed", detail: error.localizedDescription)
            if let cached = defaults.data(forKey: "cachedAppConfig"),
               let config = try? JSONDecoder().decode(AppConfig.self, from: cached) {
                return config
            }
            return AppConfig(flags: ["new_mushaf": false], pages: nil)
        }
    }

    private func makeQuranConfig(from appConfig: AppConfig) -> QuranConfig {
        var config = QuranConfig()
        if let pages = appConfig.pages {
            config.pagesArchiveURL = pages.url
            config.pagesArchiveSHA256 = pages.sha256
            config.pagesVersion = pages.version
        }
        if let archive = defaults.string(forKey: "pagesArchive") {
            config.pagesArchiveURL = URL(fileURLWithPath: archive)
        }
        if defaults.bool(forKey: "resetPages") {
            try? FileManager.default.removeItem(at: config.storageDirectory)
        }
        // Forward SDK events to the app's analytics. Here: the unified log + an in-app list (About screen).
        config.onEvent = { [weak self] event in
            let detail = event.parameters.sorted { $0.key < $1.key }.map { "\($0.key)=\($0.value)" }.joined(separator: " ")
            Task { @MainActor in self?.record(name: event.name, detail: detail) }
        }
        return config
    }

    private func record(name: String, detail: String) {
        logger.info("\(name, privacy: .public) \(detail, privacy: .public)")
        events.insert((Date(), name, detail), at: 0)
        if events.count > 100 { events.removeLast() }
    }

    func install() async {
        installState = .downloading(0)
        do {
            try await quran.ensurePagesInstalled { progress in
                Task { @MainActor [weak self] in
                    guard let self, self.installState != .ready else { return }
                    switch progress {
                    case .downloading(let fraction): self.installState = .downloading(fraction)
                    case .verifying, .extracting: self.installState = .preparing
                    case .installed: break
                    }
                }
            }
            installState = .ready
        } catch {
            installState = .failed(error.localizedDescription)
        }
    }

    /// Surah shown in the header for a page (the last surah that starts on or before it).
    func surahName(forPage page: Int) -> String {
        surahs.last(where: { $0.startPage <= page })?.nameAr ?? ""
    }

    func juzNumber(forPage page: Int) -> Int {
        juz.last(where: { $0.startPage <= page })?.number ?? 1
    }
}

struct RootView: View {
    @EnvironmentObject private var model: ExampleModel

    var body: some View {
        Group {
            if let error = model.dataError {
                Text(error).foregroundColor(.red).padding()
            } else if let config = model.appConfig, !config.newMushafEnabled {
                // Feature flag off (or server unreachable with no cached config): keep the current mushaf.
                LegacyMushafView()
            } else if model.installState == .ready {
                MushafScreen()
            } else if model.appConfig == nil {
                ProgressView()
            } else {
                InstallView()
            }
        }
    }
}

/// Stand-in for Taahud's current mushaf — what users see when `new_mushaf` is off.
struct LegacyMushafView: View {
    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "book")
                .font(.system(size: 48, weight: .light))
                .foregroundColor(.secondary)
            Text("المصحف الحالي").font(.title2.bold())
            Text("المفتاح new_mushaf مغلق من الخادم، فيبقى المستخدم على المصحف الحالي دون أي تغيير.")
                .font(.callout)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .accessibilityIdentifier("legacyMushaf")
    }
}
