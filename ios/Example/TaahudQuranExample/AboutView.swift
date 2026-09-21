import SwiftUI

/// Attribution (required, see NOTICE) + the SDK event log used for pilot metrics.
struct AboutView: View {
    @EnvironmentObject private var model: ExampleModel

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        return f
    }()

    var body: some View {
        List {
            Section("المصادر") {
                VStack(alignment: .leading, spacing: 6) {
                    Text("صور صفحات مصحف المدينة ومواقع الآيات").font(.subheadline.bold())
                    Text("من Quran.com — مؤسسة Quran Foundation").font(.subheadline)
                    Link("quran.com", destination: URL(string: "https://quran.com")!).font(.footnote)
                }
                .accessibilityIdentifier("attribution")
                VStack(alignment: .leading, spacing: 4) {
                    Text("بيانات الصفحات والأجزاء المرجعية").font(.subheadline.bold())
                    Text("quran-ios (Apache-2.0)").font(.footnote).foregroundColor(.secondary)
                }
            }

            Section("الإعدادات من الخادم") {
                row("new_mushaf", model.appConfig?.newMushafEnabled == true ? "مفعّل" : "مغلق")
                row("مصدر الصور", model.appConfig?.pages?.url.host ?? "Quran.com (افتراضي)")
                row("نسخة الصور", "\(model.appConfig?.pages?.version ?? 1)")
            }

            Section("سجل أحداث الـSDK") {
                if model.events.isEmpty {
                    Text("لا توجد أحداث بعد").foregroundColor(.secondary)
                }
                ForEach(Array(model.events.enumerated()), id: \.offset) { _, event in
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text(event.name).font(.footnote.monospaced().bold())
                            Spacer()
                            Text(Self.timeFormatter.string(from: event.date)).font(.caption2).foregroundColor(.secondary)
                        }
                        if !event.detail.isEmpty {
                            Text(event.detail).font(.caption.monospaced()).foregroundColor(.secondary)
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                }
            }
            .accessibilityIdentifier("eventLog")
        }
        .navigationTitle("عن التطبيق")
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value).foregroundColor(.secondary)
        }
    }
}
