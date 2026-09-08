import SwiftUI

struct MoreView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        List {
            Section {
                HStack(spacing: 14) {
                    Image(systemName: "person.crop.circle.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(BrandColors.teal)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(model.user?.name ?? "Usuário")
                            .font(.headline)
                        Text(model.user?.email ?? "")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
            }

            Section("Configurações") {
                NavigationLink { WhatsAppView() } label: {
                    Label("WhatsApp e cuidadores", systemImage: "message.fill")
                }
                NavigationLink { SettingsView() } label: {
                    Label("Servidor e sessão", systemImage: "gearshape.fill")
                }
                NavigationLink { PrivacyView() } label: {
                    Label("Privacidade e LGPD", systemImage: "lock.shield.fill")
                }
            }

            Section {
                Button(role: .destructive) { model.logout() } label: {
                    Label("Sair da conta", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Mais")
    }
}
