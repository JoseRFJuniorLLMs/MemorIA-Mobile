import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var model: AppModel
    @State private var isRegister = false
    @State private var cpf = ""
    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var acceptedPrivacy = false
    @State private var showingForgotPassword = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 10) {
                        Image(systemName: "cross.case.fill")
                            .font(.system(size: 54))
                            .foregroundStyle(BrandColors.teal)
                        Text("MemorIA")
                            .font(.largeTitle.weight(.bold))
                        Text("Seu lembrete de medicamentos, simples e seguro.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.top, 26)

                    Picker("Acesso", selection: $isRegister) {
                        Text("Entrar").tag(false)
                        Text("Criar conta").tag(true)
                    }
                    .pickerStyle(.segmented)

                    CardContainer {
                        VStack(spacing: 16) {
                            if isRegister {
                                TextField("Nome completo", text: $name)
                                    .textContentType(.name)
                                TextField("E-mail (opcional)", text: $email)
                                    .textContentType(.emailAddress)
                                    .keyboardType(.emailAddress)
                                TextField("Telefone (opcional)", text: $phone)
                                    .keyboardType(.phonePad)
                            }

                            TextField("CPF", text: $cpf)
                                .textContentType(.username)
                                .keyboardType(.numberPad)
                            SecureField("Senha", text: $password)
                                .textContentType(isRegister ? .newPassword : .password)
                            if isRegister {
                                SecureField("Confirme a senha", text: $confirmPassword)
                                    .textContentType(.newPassword)
                                Toggle(isOn: $acceptedPrivacy) {
                                    Text("Aceito a política de privacidade e o uso dos meus dados para os lembretes.")
                                        .font(.footnote)
                                }
                                .tint(BrandColors.teal)
                            }

                            PrimaryButton(
                                title: isRegister ? "Criar conta" : "Entrar",
                                systemImage: isRegister ? "person.badge.plus" : "arrow.right"
                            ) {
                                Task { await submit() }
                            }
                            .disabled(!canSubmit)

                            if !isRegister {
                                Button("Esqueci minha senha") {
                                    showingForgotPassword = true
                                }
                                .font(.subheadline)
                            }
                        }
                    }

                    Text("O MemorIA é uma ferramenta auxiliar e não substitui orientação médica.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(BrandColors.background)
            .navigationBarHidden(true)
            .sheet(isPresented: $showingForgotPassword) {
                NavigationStack { ForgotPasswordView() }
            }
        }
    }

    private var canSubmit: Bool {
        guard !cpf.digitsOnly.isEmpty, !password.isEmpty else { return false }
        if !isRegister { return true }
        return !name.trimmingCharacters(in: .whitespaces).isEmpty &&
            password == confirmPassword && acceptedPrivacy
    }

    private func submit() async {
        if isRegister {
            await model.register(
                cpf: cpf,
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                password: password,
                email: email.isEmpty ? nil : email,
                phone: phone.isEmpty ? nil : phone
            )
        } else {
            await model.login(cpf: cpf, password: password)
        }
    }
}
