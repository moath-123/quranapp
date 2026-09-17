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

/// App state for the example. Launch arguments (for testing):
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

    let quran: QuranAPI
    @Published var installState: InstallState = .checking
    @Published var surahs: [Surah] = []
    @Published var juz: [Juz] = []
    @Published var dataError: String?

    private let defaults = UserDefaults.standard

    init() {
        var config = QuranConfig()
        if let archive = UserDefaults.standard.string(forKey: "pagesArchive") {
            config.pagesArchiveURL = URL(fileURLWithPath: archive)
        }
        if UserDefaults.standard.bool(forKey: "resetPages") {
            try? FileManager.default.removeItem(at: config.storageDirectory)
        }
        quran = QuranAPI(config: config)
    }

    var startPage: Int {
        let arg = defaults.integer(forKey: "startPage")
        if arg > 0 { return arg }
        let saved = defaults.integer(forKey: "lastPage")
        return saved > 0 ? saved : 1
    }

    func savePage(_ page: Int) { defaults.set(page, forKey: "lastPage") }

    func start() async {
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
            } else if model.installState == .ready {
                MushafScreen()
            } else {
                InstallView()
            }
        }
    }
}
