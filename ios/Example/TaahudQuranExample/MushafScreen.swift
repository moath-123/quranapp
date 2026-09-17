import SwiftUI
import TaahudQuranCore
import TaahudQuranUI

/// The reader: full-screen mushaf, floating header, tap an ayah to select it,
/// long-press for a menu, tap empty space for focus mode.
struct MushafScreen: View {
    @EnvironmentObject private var model: ExampleModel
    @State private var page = 1
    @State private var didSetStartPage = false
    @State private var selected: Ayah?
    @State private var menuAyah: Ayah?
    @State private var showChrome = true
    @State private var showIndex = false
    @State private var toast: String?

    var body: some View {
        ZStack {
            Color(uiColor: .systemBackground).ignoresSafeArea()

            QuranPageView(
                api: model.quran,
                page: $page,
                highlightedAyahIds: selected.map { [$0.id] } ?? [],
                onAyahTap: { ayah in
                    selected = (selected?.id == ayah.id) ? nil : ayah
                },
                onAyahLongPress: { ayah in
                    selected = ayah
                    menuAyah = ayah
                },
                onEmptyTap: {
                    selected = nil
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.9)) { showChrome.toggle() }
                }
            )
            .ignoresSafeArea()

            VStack {
                if showChrome {
                    header.transition(.move(edge: .top).combined(with: .opacity))
                }
                Spacer()
                if let toast {
                    Text(toast)
                        .font(.footnote.bold())
                        .padding(.horizontal, 16).padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: Capsule())
                        .transition(.opacity)
                }
                if showChrome, let selected {
                    ayahCard(selected).transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
        }
        .onAppear {
            guard !didSetStartPage else { return }
            didSetStartPage = true
            page = model.startPage
        }
        .onChange(of: page) { newValue in
            model.savePage(newValue)
        }
        .sheet(isPresented: $showIndex) {
            SurahIndexView { target in
                page = target
                showIndex = false
            }
            .environmentObject(model)
        }
        .confirmationDialog(
            menuAyah.map { "سورة \(model.surahs[$0.surahNumber - 1].nameAr)، الآية \($0.ayahNumber)" } ?? "",
            isPresented: Binding(get: { menuAyah != nil }, set: { if !$0 { menuAyah = nil } }),
            titleVisibility: .visible
        ) {
            if let ayah = menuAyah {
                Button("نسخ الآية") {
                    UIPasteboard.general.string = ayah.textUthmani
                    flash("تم النسخ")
                }
                Button("تسجيل ورد من هنا") { flash("مثال: الورد يُسجَّل في التطبيق") }
            }
            Button("إلغاء", role: .cancel) {}
        }
        .accessibilityIdentifier("mushafScreen")
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { showIndex = true } label: {
                Image(systemName: "list.bullet")
                    .font(.system(size: 17, weight: .semibold))
                    .frame(width: 44, height: 44)
                    .background(.ultraThinMaterial, in: Circle())
            }
            .accessibilityIdentifier("indexButton")

            HStack(spacing: 8) {
                Text(model.surahName(forPage: page)).font(.headline)
                Circle().frame(width: 4, height: 4).foregroundColor(.secondary)
                Text("الجزء \(model.juzNumber(forPage: page))").font(.subheadline).foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(.ultraThinMaterial, in: Capsule())

            Text("\(page)")
                .font(.headline.monospacedDigit())
                .foregroundColor(.brown)
                .frame(minWidth: 44, minHeight: 44)
                .padding(.horizontal, 4)
                .background(.ultraThinMaterial, in: Capsule())
                .accessibilityIdentifier("pageLabel")
        }
    }

    private func ayahCard(_ ayah: Ayah) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("سورة \(model.surahs[ayah.surahNumber - 1].nameAr) · الآية \(ayah.ayahNumber)")
                .font(.subheadline.bold())
                .accessibilityIdentifier("selectedAyah")
            Text(ayah.textUthmani)
                .font(.system(size: 18))
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private func flash(_ message: String) {
        withAnimation { toast = message }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            withAnimation { toast = nil }
        }
    }
}

struct SurahIndexView: View {
    @EnvironmentObject private var model: ExampleModel
    let onSelect: (Int) -> Void

    var body: some View {
        NavigationView {
            List(model.surahs, id: \.number) { surah in
                Button { onSelect(surah.startPage) } label: {
                    HStack {
                        Text("\(surah.number)").foregroundColor(.brown).frame(width: 32)
                        VStack(alignment: .leading) {
                            Text("سورة \(surah.nameAr)").font(.headline)
                            Text("\(surah.ayahCount) آية").font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Text("\(surah.startPage)").foregroundColor(.secondary)
                    }
                }
                .foregroundColor(.primary)
            }
            .navigationTitle("الفهرس")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
