import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var serverURL = ""

    var body: some View {
        Form {
            Section("Servidor MemorIA") {
                TextField("https://servidor", text: $serverURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                Text("O app acrescenta /api automaticamente. Use HTTPS em produção.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Button("Salvar endereço") {
                    model.updateBaseURL(serverURL)
                    serverURL = model.baseURL
                    Task { await model.checkServer() }
                }
                Button("Testar conexão") {
                    Task { await model.checkServer() }
                }
            }

            Section("Sessão") {
                LabeledContent("Conta", value: model.user?.name ?? "—")
                LabeledContent("CPF", value: model.user?.cpfMasked.isEmpty == false ? model.user?.cpfMasked ?? "—" : "—")
            }

            Section {
                Button("Voltar ao servidor padrão") {
                    model.updateBaseURL(APIClient.defaultBaseURL)
                    serverURL = model.baseURL
                }
            }
        }
        .navigationTitle("Ajustes")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { serverURL = model.baseURL }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Pronto") { dismiss() }
            }
        }
    }
}
