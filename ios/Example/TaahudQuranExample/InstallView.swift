import SwiftUI

/// First launch: download the mushaf page images (QuranAPI.ensurePagesInstalled).
struct InstallView: View {
    @EnvironmentObject private var model: ExampleModel

    var body: some View {
        VStack(spacing: 22) {
            Spacer()
            Image(systemName: "book.closed")
                .font(.system(size: 52, weight: .light))
                .foregroundColor(.brown)
            Text("مصحف المدينة")
                .font(.largeTitle.bold())
            Text("يحتاج المصحف تنزيل صور الصفحات مرة واحدة (حوالي ٦٠ ميجا)، ثم يعمل بدون إنترنت.")
                .font(.callout)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            status
                .frame(height: 70)
                .padding(.horizontal, 40)

            Spacer()
        }
        .padding()
    }

    @ViewBuilder
    private var status: some View {
        switch model.installState {
        case .checking:
            ProgressView()
        case .needed:
            primaryButton("تنزيل صفحات المصحف")
        case .downloading(let fraction):
            VStack(spacing: 8) {
                ProgressView(value: fraction)
                    .tint(.brown)
                Text("جاري التنزيل \(Int(fraction * 100))٪")
                    .font(.footnote.monospacedDigit())
                    .foregroundColor(.secondary)
                    .accessibilityIdentifier("installProgress")
            }
        case .preparing:
            VStack(spacing: 8) {
                ProgressView()
                Text("جاري تجهيز الصفحات…").font(.footnote).foregroundColor(.secondary)
            }
        case .failed(let message):
            VStack(spacing: 10) {
                Text("تعذّر التنزيل: \(message)")
                    .font(.footnote)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .accessibilityIdentifier("installError")
                primaryButton("إعادة المحاولة")
            }
        case .ready:
            EmptyView()
        }
    }

    private func primaryButton(_ title: String) -> some View {
        Button {
            Task { await model.install() }
        } label: {
            Text(title)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Capsule().fill(Color.brown))
                .foregroundColor(.white)
        }
        .accessibilityIdentifier("installButton")
    }
}
