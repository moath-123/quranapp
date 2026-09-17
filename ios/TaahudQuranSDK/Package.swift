// swift-tools-version:6.0

import PackageDescription

let package = Package(
    name: "TaahudQuranSDK",
    defaultLocalization: "ar",
    platforms: [
        .iOS(.v15),
        // macOS is only here so the core can be built and tested with `swift test` without Xcode.
        .macOS(.v12),
    ],
    products: [
        .library(name: "TaahudQuranSDK", targets: ["TaahudQuranCore", "TaahudQuranUI"]),
    ],
    dependencies: [
        .package(url: "https://github.com/weichsel/ZIPFoundation.git", from: "0.9.19"),
    ],
    targets: [
        .target(
            name: "TaahudQuranCore",
            dependencies: [
                .product(name: "ZIPFoundation", package: "ZIPFoundation"),
            ],
            resources: [
                .copy("Resources/quran.json"),
                .copy("Resources/ayahinfo_1024.db"),
            ]
        ),
        .target(
            name: "TaahudQuranUI",
            dependencies: ["TaahudQuranCore"]
        ),
        .testTarget(
            name: "TaahudQuranCoreTests",
            dependencies: [
                "TaahudQuranCore",
                .product(name: "ZIPFoundation", package: "ZIPFoundation"),
            ]
        ),
    ],
    swiftLanguageModes: [.v5]
)
