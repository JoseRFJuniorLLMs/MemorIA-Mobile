import SwiftUI
import UniformTypeIdentifiers

struct AccountExportDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .data] }
    var data: Data

    init(data: Data = Data()) { self.data = data }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

struct PrivacyView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showingDeleteConfirmation = false
    @State private var showingExporter = false

    var body: some View {
        Form {
            Section("Seus dados") {
                Text("Você pode revogar o consentimento ou excluir sua conta a qualquer momento. A exclusão remove os dados do servidor e encerra a sessão neste aparelho.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Section {
                Toggle("Consentimento LGPD", isOn: Binding(
                    get: { model.user?.consentDate != nil },
                    set: { value in Task { await updateConsent(value) } }
                ))
                .tint(BrandColors.teal)
            }

            Section {
                Button {
                    Task {
                        await model.exportAccountData()
                        if model.exportedData != nil { showingExporter = true }
                    }
                } label: {
                    Label("Exportar meus dados", systemImage: "square.and.arrow.down")
                }
                Button("Excluir minha conta", role: .destructive) {
                    showingDeleteConfirmation = true
                }
            }
        }
        .navigationTitle("Privacidade")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("Excluir a conta e todos os dados?", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("Excluir definitivamente", role: .destructive) { Task { await model.deleteAccount() } }
            Button("Cancelar", role: .cancel) { }
        } message: {
            Text("Essa ação não pode ser desfeita.")
        }
        .fileExporter(
            isPresented: $showingExporter,
            document: AccountExportDocument(data: model.exportedData ?? Data()),
            contentType: .json,
            defaultFilename: "memoria-dados.json"
        ) { _ in }
    }

    private func updateConsent(_ granted: Bool) async {
        await model.setConsent(granted)
    }
}
