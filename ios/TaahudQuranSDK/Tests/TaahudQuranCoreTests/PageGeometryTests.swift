import Testing
@testable import TaahudQuranCore

struct PageGeometryTests {
    let api = QuranAPI()

    @Test func everyAyahHasAHighlightOnItsOwnPage() async throws {
        var missing: [Int] = []
        for page in 1...604 {
            let ids = Set(try await api.ayahRects(page: page).map(\.ayahId))
            for ayah in try await api.getAyahsByPage(page) where !ids.contains(ayah.id) {
                missing.append(ayah.id)
            }
        }
        #expect(missing.isEmpty, "ayahs without a rectangle on their page: \(missing.prefix(10))")
    }

    @Test func rectanglesStayInsideTheImage() async throws {
        for page in [1, 2, 50, 121, 604] {
            for r in try await api.ayahRects(page: page) {
                #expect(r.minX >= 0 && r.maxX <= 1024 && r.minY >= 0 && r.maxY <= 1656)
                #expect(r.width > 0 && r.height > 0)
            }
        }
    }

    @Test func tappingTheMiddleOfAnAyahFindsIt() async throws {
        for page in [2, 50, 300] {
            for r in try await api.ayahRects(page: page) {
                let ayah = try await api.ayahAt(page: page, x: Double(r.minX + r.maxX) / 2, y: Double(r.minY + r.maxY) / 2)
                #expect(ayah?.id == r.ayahId, "page \(page)")
                #expect(ayah?.pageNumber == page)
            }
        }
    }

    @Test func tappingEmptySpaceFindsNothing() async throws {
        #expect(try await api.ayahAt(page: 2, x: 512, y: 1500) == nil)   // blank bottom of page 2
        #expect(try await api.ayahAt(page: 50, x: -100, y: 400) == nil)
    }

    @Test func pageTwoMatchesTheWebDemoRectangles() async throws {
        // Same numbers as web-demo/page-rects.js for page 2 (both come from ayahinfo_1024.db).
        let rects = try await api.ayahRects(page: 2)
        #expect(rects.count == 9)
        #expect(Set(rects.map(\.ayahId)) == Set(8...12))
    }
}
