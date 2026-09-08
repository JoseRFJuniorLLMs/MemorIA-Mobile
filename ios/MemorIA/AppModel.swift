import Foundation
import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    @Published private(set) var isAuthenticated = false
    @Published private(set) var user: User?
    @Published private(set) var medications: [Medication] = []
    @Published private(set) var history: [HistoryEntry] = []
    @Published private(set) var adherence: Adherence?
    @Published private(set) var exportedData: Data?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var noticeMessage: String?
    @Published var baseURL: String {
        didSet {
            let normalized = APIClient.normalizeRoot(baseURL)
            UserDefaults.standard.set(normalized, forKey: Keys.baseURL)
            api.setBaseRoot(normalized)
        }
    }

    private let api: APIClient

    init() {
        let savedURL = UserDefaults.standard.string(forKey: Keys.baseURL) ?? APIClient.defaultBaseURL
        baseURL = APIClient.normalizeRoot(savedURL)
        api = APIClient(baseRoot: baseURL)
        if let token = KeychainStore.token {
            api.setToken(token)
            isAuthenticated = true
            Task { await bootstrap() }
        }
    }

    func bootstrap() async {
        guard isAuthenticated else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            user = try await api.me()
            try await refreshData()
        } catch {
            if case APIError.unauthorized = error { logout() }
            else { errorMessage = error.localizedDescription }
        }
    }

    func login(cpf: String, password: String) async {
        await perform {
            let auth = try await self.api.login(cpf: cpf, password: password)
            try self.accept(auth)
            try await self.refreshData()
        }
    }

    func register(cpf: String, name: String, password: String, email: String?, phone: String?) async {
        await perform {
            let auth = try await self.api.register(cpf: cpf, name: name, password: password, email: email, phone: phone)
            try self.accept(auth)
            try await self.refreshData()
        }
    }

    func forgotPassword(cpf: String, email: String) async {
        await perform {
            try await self.api.forgotPassword(cpf: cpf, email: email)
            self.noticeMessage = "Se os dados estiverem corretos, enviaremos as instruções para o seu e-mail."
        }
    }

    func logout() {
        KeychainStore.delete()
        api.setToken(nil)
        user = nil
        medications = []
        history = []
        adherence = nil
        isAuthenticated = false
    }

    func refreshData() async throws {
        medications = try await api.medications()
        history = try await api.history()
        adherence = try await api.adherence()
    }

    func saveMedication(_ medication: Medication, existingID: String?) async {
        await perform {
            let request = MedicationRequest(
                name: medication.name,
                dosage: medication.dosage,
                frequency: medication.frequency,
                times: medication.times,
                weekDays: medication.weekDays,
                instructions: medication.instructions,
                stock: medication.stock,
                active: medication.active,
                supplier: medication.supplier
            )
            if let existingID {
                _ = try await self.api.updateMedication(id: existingID, request: request)
            } else {
                _ = try await self.api.createMedication(request)
            }
            try await self.refreshData()
            self.noticeMessage = existingID == nil ? "Medicamento adicionado." : "Medicamento atualizado."
        }
    }

    func deleteMedication(_ medication: Medication) async {
        guard let id = medication.id else { return }
        await perform {
            try await self.api.deleteMedication(id: id)
            self.medications.removeAll { $0.id == id }
            self.noticeMessage = "Medicamento removido."
        }
    }

    func record(_ medication: Medication, at time: String, status: String) async {
        guard let id = medication.id else { return }
        await perform {
            let now = ISO8601DateFormatter().string(from: Date())
            let request = HistoryRequest(
                medicationId: id,
                medicationName: medication.name,
                dosage: medication.dosage,
                scheduleTime: time,
                status: status,
                scheduledFor: now,
                takenAt: status == "taken" ? now : nil,
                notes: nil
            )
            try await self.api.addHistory(request)
            try await self.refreshData()
            self.noticeMessage = status == "taken" ? "Dose marcada como tomada." : "Dose registrada."
        }
    }

    func saveWhatsApp(phone: String, caregivers: [Caregiver]) async {
        await perform {
            let updated = try await self.api.updateProfile(ProfileUpdateRequest(
                name: nil, email: nil, phone: phone, city: nil, state: nil,
                caregivers: caregivers, healthVitalSigns: nil, medicalConsultations: nil
            ))
            self.user = updated
            self.noticeMessage = "Contato do WhatsApp atualizado."
        }
    }

    func setConsent(_ granted: Bool) async {
        await perform {
            self.user = try await self.api.updateConsent(granted: granted)
        }
    }

    func saveHealth(systolic: Int?, diastolic: Int?, heartRate: Int?, spo2: Double?, glucose: Double?) async {
        await perform {
            let payload = VitalSignsPayload(
                date: ISO8601DateFormatter().string(from: Date()),
                systolic: systolic,
                diastolic: diastolic,
                heartRate: heartRate,
                spo2: spo2,
                glucose: glucose,
                glucoseContext: "random",
                hba1c: nil,
                temperature: nil
            )
            let updated = try await self.api.updateProfile(ProfileUpdateRequest(
                name: nil, email: nil, phone: nil, city: nil, state: nil,
                caregivers: nil, healthVitalSigns: [payload], medicalConsultations: nil
            ))
            self.user = updated
            self.noticeMessage = "Medição de saúde sincronizada."
        }
    }

    func checkServer() async {
        await perform {
            try await self.api.checkHealth()
            self.noticeMessage = "Servidor MemorIA disponível."
        }
    }

    func deleteAccount() async {
        await perform {
            try await self.api.deleteAccount()
            self.logout()
        }
    }

    func exportAccountData() async {
        await perform {
            self.exportedData = try await self.api.exportData()
        }
    }

    func clearMessages() {
        errorMessage = nil
        noticeMessage = nil
    }

    func updateBaseURL(_ value: String) {
        baseURL = APIClient.normalizeRoot(value)
    }

    private func accept(_ auth: AuthData) throws {
        guard let token = auth.token else { throw APIError.decoding }
        KeychainStore.token = token
        api.setToken(token)
        user = auth.user
        isAuthenticated = true
    }

    private func perform(_ operation: @escaping () async throws -> Void) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do { try await operation() }
        catch { errorMessage = error.localizedDescription }
    }

    private enum Keys {
        static let baseURL = "memoria.api.baseURL"
    }
}
