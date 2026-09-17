import Foundation

/// Same rules as `ArabicNormalizer.kt` on Android, so search behaves identically on both platforms.
public enum ArabicNormalizer {

    public static func normalize(_ input: String) -> String {
        guard !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }
        var scalars = String.UnicodeScalarView()
        scalars.reserveCapacity(input.unicodeScalars.count)
        for scalar in input.unicodeScalars {
            if isDiacritic(scalar) || scalar == "\u{0640}" { continue }  // harakat, Quranic marks, tatweel
            switch scalar {
            case "أ", "إ", "آ", "ٱ": scalars.append("ا")
            case "ى", "ئ": scalars.append("ي")
            case "ؤ": scalars.append("و")
            case "﴿", "﴾", "،", "؛", "؟": scalars.append(" ")
            default: scalars.append(scalar)
            }
        }
        return String(scalars)
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }

    /// First run of 1–4 digits (Western or Arabic-Indic), e.g. "صفحة ٥٠" → 50.
    public static func extractPageNumber(_ input: String) -> Int? {
        var digits = ""
        for scalar in input.unicodeScalars {
            let value: UInt32?
            switch scalar.value {
            case 0x30...0x39: value = scalar.value - 0x30
            case 0x660...0x669: value = scalar.value - 0x660
            default: value = nil
            }
            if let value {
                digits.append(String(value))
                if digits.count == 4 { break }
            } else if !digits.isEmpty {
                break
            }
        }
        return digits.isEmpty ? nil : Int(digits)
    }

    private static func isDiacritic(_ s: Unicode.Scalar) -> Bool {
        switch s.value {
        case 0x0610...0x061A, 0x064B...0x065F, 0x0670, 0x06D6...0x06ED: return true
        default: return false
        }
    }
}
