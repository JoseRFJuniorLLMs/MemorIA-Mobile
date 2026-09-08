import SwiftUI

struct WhatsAppView: View {
    @EnvironmentObject private var model: AppModel
    @State private var phone = ""
    @State private var caregivers: [Caregiver] = []

    var body: some View {
        Form {
            Section {
                Text("O servidor envia os lembretes pelo WhatsApp, mesmo quando o app está fechado. Informe o telefone com DDD.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                TextField("Seu WhatsApp", text: $phone)
                    .keyboardType(.phonePad)
            } header: {
                Text("Paciente")
            }

            Section {
                ForEach(caregivers.indices, id: \.self) { index in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            TextField("Nome", text: binding(for: index, keyPath: \.name))
                            Button(role: .destructive) { caregivers.remove(at: index) } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.borderless)
                        }
                        TextField("Telefone", text: binding(for: index, keyPath: \.phone))
                            .keyboardType(.phonePad)
                        TextField("Parentesco (ex.: filha)", text: binding(for: index, keyPath: \.relation))
                    }
                    .padding(.vertical, 5)
                }
                Button { caregivers.append(Caregiver(name: "", phone: "", relation: "")) } label: {
                    Label("Adicionar cuidador", systemImage: "person.badge.plus")
                }
            } header: {
                Text("Cuidadores")
            }

            Section {
                PrimaryButton(title: "Salvar contatos", systemImage: "checkmark") {
                    Task {
                        await model.saveWhatsApp(
                            phone: phone,
                            caregivers: caregivers.filter { !$0.name.isEmpty && !$0.phone.isEmpty }
                        )
                    }
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                .listRowBackground(Color.clear)
            }

            if !phone.digitsOnly.isEmpty {
                Section {
                    Link(destination: URL(string: "https://wa.me/\(phone.digitsOnly)")!) {
                        Label("Abrir conversa no WhatsApp", systemImage: "arrow.up.right.square")
                    }
                }
            }
        }
        .navigationTitle("WhatsApp")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            phone = model.user?.phone ?? ""
            caregivers = model.user?.caregivers ?? []
        }
    }

    private func binding(for index: Int, keyPath: WritableKeyPath<Caregiver, String>) -> Binding<String> {
        Binding(
            get: { caregivers[index][keyPath: keyPath] },
            set: { caregivers[index][keyPath: keyPath] = $0 }
        )
    }
}
