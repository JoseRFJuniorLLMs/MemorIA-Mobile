import Foundation

enum APIError: LocalizedError {
    case invalidURL
    case server(String)
    case unauthorized
    case decoding
    case network(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "O endereço do servidor não é válido."
        case .server(let message):
            return message
        case .unauthorized:
            return "Sessão inválida ou credenciais incorretas."
        case .decoding:
            return "O servidor respondeu num formato inesperado."
        case .network:
            return "Falha de rede. Verifique a conexão e tente novamente."
        }
    }
}

final class APIClient {
    static let defaultBaseURL = "https://35.247.217.66.nip.io"

    private let session: URLSession
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private var token: String?

    private(set) var baseRoot: String

    init(baseRoot: String = APIClient.defaultBaseURL, session: URLSession = .shared) {
        self.baseRoot = Self.normalizeRoot(baseRoot)
        self.session = session
    }

    func setToken(_ token: String?) { self.token = token }

    func setBaseRoot(_ value: String) { baseRoot = Self.normalizeRoot(value) }

    static func normalizeRoot(_ raw: String) -> String {
        var value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty { value = defaultBaseURL }
        while value.hasSuffix("/") { value.removeLast() }
        if value.lowercased().hasSuffix("/api") {
            value = String(value.dropLast(4)).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        }
        return value
    }

    func login(cpf: String, password: String) async throws -> AuthData {
        try await envelope("auth/login", method: "POST", body: LoginRequest(cpf: cpf.digitsOnly, password: password))
    }

    func register(cpf: String, name: String, password: String, email: String?, phone: String?) async throws -> AuthData {
        try await envelope(
            "auth/register",
            method: "POST",
            body: RegisterRequest(cpf: cpf.digitsOnly, name: name, password: password, email: email, phone: phone, lgpdConsent: true)
        )
    }

    func forgotPassword(cpf: String, email: String) async throws {
        try await simple("auth/forgot-password", method: "POST", body: ["cpf": cpf.digitsOnly, "email": email])
    }

    func me() async throws -> User {
        let result: UserData = try await envelope("auth/me")
        guard let user = result.user else { throw APIError.decoding }
        return user
    }

    func updateProfile(_ request: ProfileUpdateRequest) async throws -> User {
        let result: UserData = try await envelope("auth/profile", method: "PUT", body: request)
        guard let user = result.user else { throw APIError.decoding }
        return user
    }

    func medications() async throws -> [Medication] {
        let result: MedicationListData = try await envelope("medications")
        return result.medications
    }

    func createMedication(_ request: MedicationRequest) async throws -> Medication {
        let result: MedicationData = try await envelope("medications", method: "POST", body: request)
        guard let medication = result.medication else { throw APIError.decoding }
        return medication
    }

    func updateMedication(id: String, request: MedicationRequest) async throws -> Medication {
        let result: MedicationData = try await envelope("medications/\(id)", method: "PUT", body: request)
        guard let medication = result.medication else { throw APIError.decoding }
        return medication
    }

    func deleteMedication(id: String) async throws {
        try await simple("medications/\(id)", method: "DELETE")
    }

    func history(limit: Int = 100) async throws -> [HistoryEntry] {
        let result: HistoryListData = try await envelope("history?limit=\(limit)")
        return result.history
    }

    func addHistory(_ request: HistoryRequest) async throws {
        try await simple("history", method: "POST", body: request)
    }

    func adherence(days: Int = 30) async throws -> Adherence {
        try await envelope("history/adherence?days=\(days)")
    }

    func updateConsent(granted: Bool) async throws -> User {
        let result: UserData = try await envelope("auth/consent", method: "PUT", body: ["consent": granted])
        guard let user = result.user else { throw APIError.decoding }
        return user
    }

    func exportData() async throws -> Data {
        try await requestData(path: "auth/export-data", method: "GET", bodyData: nil)
    }

    func deleteAccount() async throws {
        try await simple("auth/account", method: "DELETE")
    }

    func checkHealth() async throws {
        let data = try await requestData(path: "health", method: "GET", bodyData: nil, apiPrefix: false)
        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            object["status"] as? String == "ok"
        else { throw APIError.server("O endereço respondeu, mas não parece ser o servidor MemorIA.") }
    }

    private func envelope<Value: Decodable>(_ path: String, method: String = "GET") async throws -> Value {
        try await envelope(path, method: method, bodyData: nil)
    }

    private func envelope<Value: Decodable, Body: Encodable>(_ path: String, method: String, body: Body) async throws -> Value {
        try await envelope(path, method: method, bodyData: try encoder.encode(body))
    }

    private func envelope<Value: Decodable>(_ path: String, method: String, bodyData: Data?) async throws -> Value {
        let data = try await requestData(path: path, method: method, bodyData: bodyData)
        do {
            let response = try decoder.decode(APIEnvelope<Value>.self, from: data)
            guard response.success, let value = response.data else {
                throw APIError.server(response.message ?? response.error ?? "Não foi possível concluir a operação.")
            }
            return value
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.decoding
        }
    }

    private func simple(_ path: String, method: String) async throws {
        try await simple(path, method: method, bodyData: nil)
    }

    private func simple<Body: Encodable>(_ path: String, method: String, body: Body) async throws {
        try await simple(path, method: method, bodyData: try encoder.encode(body))
    }

    private func simple(_ path: String, method: String, bodyData: Data?) async throws {
        let data = try await requestData(path: path, method: method, bodyData: bodyData)
        do {
            let response = try decoder.decode(SimpleResponse.self, from: data)
            guard response.success else {
                throw APIError.server(response.message ?? response.error ?? "Não foi possível concluir a operação.")
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.decoding
        }
    }

    private func requestData(path: String, method: String, bodyData: Data?, apiPrefix: Bool = true) async throws -> Data {
        let root = Self.normalizeRoot(baseRoot)
        let rawURL = apiPrefix ? "\(root)/api/\(path)" : "\(root)/\(path)"
        guard let url = URL(string: rawURL) else { throw APIError.invalidURL }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 25
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if bodyData != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if let token, !token.isEmpty { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        request.httpBody = bodyData

        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw APIError.network(URLError(.badServerResponse)) }
            guard (200..<300).contains(http.statusCode) else {
                if http.statusCode == 401 { throw APIError.unauthorized }
                let message = (try? decoder.decode(SimpleResponse.self, from: data)).flatMap { $0.message ?? $0.error }
                throw APIError.server(message ?? "Erro na requisição (HTTP \(http.statusCode)).")
            }
            return data
        } catch let error as APIError {
            throw error
        } catch let error as URLError where error.code == .timedOut {
            throw APIError.server("O servidor demorou demais a responder. Verifique a internet e o endereço em “Servidor”.")
        } catch {
            throw APIError.network(error)
        }
    }
}
