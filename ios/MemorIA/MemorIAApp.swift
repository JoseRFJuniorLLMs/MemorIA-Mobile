import SwiftUI

@main
struct MemorIAApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(model)
                .tint(BrandColors.teal)
        }
    }
}
