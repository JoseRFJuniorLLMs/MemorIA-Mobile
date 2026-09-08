import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView() }
                .tabItem { Label("Início", systemImage: "house.fill") }
            NavigationStack { MedicationsView() }
                .tabItem { Label("Remédios", systemImage: "pills.fill") }
            NavigationStack { HealthView() }
                .tabItem { Label("Saúde", systemImage: "heart.text.square.fill") }
            NavigationStack { HistoryView() }
                .tabItem { Label("Histórico", systemImage: "clock.arrow.circlepath") }
            NavigationStack { MoreView() }
                .tabItem { Label("Mais", systemImage: "ellipsis.circle.fill") }
        }
        .tint(BrandColors.teal)
    }
}
