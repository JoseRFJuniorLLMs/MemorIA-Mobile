import SwiftUI

struct ForgotPasswordView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var cpf = ""
    @State private var email = ""

    var body: some View {
        Form {
            Section {
                Text("Informe o CPF e o e-mail cadastrados para receber as instruções de recuperação.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                TextField("CPF", text: $cpf)
                    .keyboardType(.numberPad)
                TextField("E-mail", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
            }

            Section {
                PrimaryButton(title: "Enviar instruções", systemImage: "paperplane.fill") {
                    Task {
                        await model.forgotPassword(cpf: cpf, email: email)
                        if model.errorMessage == nil { dismiss() }
                    }
                }
                .disabled(cpf.digitsOnly.isEmpty || email.isEmpty)
                .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("Recuperar senha")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancelar") { dismiss() }
            }
        }
    }
}
