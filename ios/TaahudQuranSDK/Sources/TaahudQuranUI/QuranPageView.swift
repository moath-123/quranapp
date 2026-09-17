#if os(iOS)
import SwiftUI
import TaahudQuranCore
import UIKit

/// Madani mushaf pager: one page image per screen, right-to-left paging (swipe right = next page),
/// ayah highlight overlay, and tap / long-press callbacks. Rendering only — menus, bookmarks and
/// wird tracking belong to the host app (decision D5).
///
/// Call `QuranAPI.ensurePagesInstalled()` before showing this view.
public struct QuranPageView: View {
    private let api: QuranAPI
    @Binding private var page: Int
    private let highlightedAyahIds: Set<Int>
    private let onAyahTap: (Ayah) -> Void
    private let onAyahLongPress: (Ayah) -> Void
    private let onEmptyTap: () -> Void

    public init(
        api: QuranAPI,
        page: Binding<Int>,
        highlightedAyahIds: Set<Int> = [],
        onAyahTap: @escaping (Ayah) -> Void = { _ in },
        onAyahLongPress: @escaping (Ayah) -> Void = { _ in },
        onEmptyTap: @escaping () -> Void = {}
    ) {
        self.api = api
        _page = page
        self.highlightedAyahIds = highlightedAyahIds
        self.onAyahTap = onAyahTap
        self.onAyahLongPress = onAyahLongPress
        self.onEmptyTap = onEmptyTap
    }

    public var body: some View {
        TabView(selection: $page) {
            ForEach(1...604, id: \.self) { number in
                QuranSinglePageView(
                    api: api,
                    page: number,
                    highlightedAyahIds: highlightedAyahIds,
                    onAyahTap: onAyahTap,
                    onAyahLongPress: onAyahLongPress,
                    onEmptyTap: onEmptyTap
                )
                .tag(number)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        // Arabic mushaf order: page 1 on the right, next pages to the left.
        .environment(\.layoutDirection, .rightToLeft)
    }
}

/// A single page: image + highlight overlay + touch handling.
public struct QuranSinglePageView: View {
    let api: QuranAPI
    let page: Int
    let highlightedAyahIds: Set<Int>
    let onAyahTap: (Ayah) -> Void
    let onAyahLongPress: (Ayah) -> Void
    let onEmptyTap: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @State private var image: UIImage?
    @State private var rects: [AyahRect] = []
    @State private var pressedAyahId: Int?
    @State private var loadError: String?

    public init(
        api: QuranAPI, page: Int, highlightedAyahIds: Set<Int> = [],
        onAyahTap: @escaping (Ayah) -> Void = { _ in },
        onAyahLongPress: @escaping (Ayah) -> Void = { _ in },
        onEmptyTap: @escaping () -> Void = {}
    ) {
        self.api = api
        self.page = page
        self.highlightedAyahIds = highlightedAyahIds
        self.onAyahTap = onAyahTap
        self.onAyahLongPress = onAyahLongPress
        self.onEmptyTap = onEmptyTap
    }

    private static let imageSize = CGSize(width: QuranAPI.pageImageSize.width, height: QuranAPI.pageImageSize.height)

    public var body: some View {
        GeometryReader { proxy in
            let scale = min(proxy.size.width / Self.imageSize.width, proxy.size.height / Self.imageSize.height)
            let fitted = CGSize(width: Self.imageSize.width * scale, height: Self.imageSize.height * scale)
            ZStack {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .interpolation(.high)
                        .modifier(NightModeModifier(enabled: colorScheme == .dark))
                } else if let loadError {
                    Text(loadError).font(.footnote).foregroundColor(.secondary).padding()
                } else {
                    ProgressView()
                }
                highlightLayer(scale: scale)
                TouchCatcher(
                    onTap: { point in handleTap(at: point, scale: scale, long: false) },
                    onLongPress: { point in handleTap(at: point, scale: scale, long: true) },
                    onPressChanged: { point in pressedAyahId = point.flatMap { ayahId(at: $0, scale: scale) } }
                )
            }
            .frame(width: fitted.width, height: fitted.height)
            .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
            .environment(\.layoutDirection, .leftToRight) // image coordinates are LTR
        }
        .task(id: page) { await load() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("صفحة \(page)"))
    }

    private func highlightLayer(scale: CGFloat) -> some View {
        Canvas { context, _ in
            for r in rects {
                let color: Color?
                if r.ayahId == pressedAyahId {
                    color = Color.orange.opacity(0.25)
                } else if highlightedAyahIds.contains(r.ayahId) {
                    color = Color.yellow.opacity(0.4)
                } else {
                    color = nil
                }
                guard let color else { continue }
                let rect = CGRect(
                    x: CGFloat(r.minX - 6) * scale, y: CGFloat(r.minY - 4) * scale,
                    width: CGFloat(r.width + 12) * scale, height: CGFloat(r.height + 8) * scale)
                context.fill(Path(roundedRect: rect, cornerRadius: 12 * scale), with: .color(color))
            }
        }
        .allowsHitTesting(false)
    }

    private func ayahId(at point: CGPoint, scale: CGFloat) -> Int? {
        let x = Double(point.x / scale), y = Double(point.y / scale)
        var best: (id: Int, distance: Double)?
        for r in rects where y >= Double(r.minY) - 4 && y <= Double(r.maxY) + 4 {
            let d = x < Double(r.minX) ? Double(r.minX) - x : x > Double(r.maxX) ? x - Double(r.maxX) : 0
            if d < 18, d < (best?.distance ?? .infinity) { best = (r.ayahId, d) }
        }
        return best?.id
    }

    private func handleTap(at point: CGPoint, scale: CGFloat, long: Bool) {
        pressedAyahId = nil
        guard let id = ayahId(at: point, scale: scale) else {
            if !long { onEmptyTap() }
            return
        }
        Task {
            guard let ayah = try? await api.getAyahById(id) else { return }
            await MainActor.run {
                if long {
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    onAyahLongPress(ayah)
                } else {
                    onAyahTap(ayah)
                }
            }
        }
    }

    private func load() async {
        loadError = nil
        let url = api.pageImageURL(page)
        let page = self.page
        let loadedImage = await Task.detached(priority: .userInitiated) { () -> UIImage? in
            guard let image = UIImage(contentsOfFile: url.path) else { return nil }
            return image.preparingForDisplay() ?? image
        }.value
        guard !Task.isCancelled else { return }
        if let loadedImage {
            image = loadedImage
        } else {
            loadError = "صورة الصفحة \(page) غير متوفرة"
        }
        rects = (try? await api.ayahRects(page: page)) ?? []
    }
}

/// Dark mode: invert the black-on-transparent page image and warm it slightly.
private struct NightModeModifier: ViewModifier {
    let enabled: Bool
    func body(content: Content) -> some View {
        if enabled {
            content.colorInvert().colorMultiply(Color(red: 0.95, green: 0.9, blue: 0.82))
        } else {
            content
        }
    }
}

/// UIKit recognizers give tap locations on iOS 15 and cooperate with the pager's pan gesture.
private struct TouchCatcher: UIViewRepresentable {
    let onTap: (CGPoint) -> Void
    let onLongPress: (CGPoint) -> Void
    let onPressChanged: (CGPoint?) -> Void

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .clear
        let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.tapped(_:)))
        let long = UILongPressGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.longPressed(_:)))
        long.minimumPressDuration = 0.45
        tap.require(toFail: long)
        [tap, long].forEach {
            $0.delegate = context.coordinator
            view.addGestureRecognizer($0)
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.parent = self
    }

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var parent: TouchCatcher

        init(parent: TouchCatcher) { self.parent = parent }

        @objc func tapped(_ recognizer: UITapGestureRecognizer) {
            parent.onTap(recognizer.location(in: recognizer.view))
        }

        @objc func longPressed(_ recognizer: UILongPressGestureRecognizer) {
            switch recognizer.state {
            case .began:
                parent.onPressChanged(recognizer.location(in: recognizer.view))
                parent.onLongPress(recognizer.location(in: recognizer.view))
            case .ended, .cancelled, .failed:
                parent.onPressChanged(nil)
            default:
                break
            }
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer
        ) -> Bool {
            // Let the page swipe (the pager's pan) keep working.
            other is UIPanGestureRecognizer
        }
    }
}
#endif
