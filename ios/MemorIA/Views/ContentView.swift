import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ZStack {
            if model.isAuthenticated {
                MainTabView()
            } else {
                AuthView()
            }
            LoadingOverlay(isVisible: model.isLoading)
        }
        .background(BrandColors.background)
        .alert("MemorIA", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { model.errorMessage = nil }
        } message: {
            Text(model.errorMessage ?? "")
        }
        .alert("MemorIA", isPresented: Binding(
            get: { model.noticeMessage != nil },
            set: { if !$0 { model.noticeMessage = nil } }
        )) {
            Button("OK", role: .cancel) { model.noticeMessage = nil }
        } message: {
            Text(model.noticeMessage ?? "")
        }
    }
}
