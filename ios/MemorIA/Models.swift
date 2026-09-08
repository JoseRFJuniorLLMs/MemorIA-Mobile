import Foundation

struct APIEnvelope<Value: Decodable>: Decodable {
    let success: Bool
    let message: String?
    let count: Int?
    let data: Value?
    let error: String?
}

struct SimpleResponse: Decodable {
    let success: Bool
    let message: String?
    let error: String?
}

struct LoginRequest: Encodable {
    let cpf: String
    let password: String
}

struct RegisterRequest: Encodable {
    let cpf: String
    let name: String
    let password: String
    let email: String?
    let phone: String?
    let lgpdConsent: Bool
}

struct AuthData: Decodable {
    let user: User?
    let token: String?
}

struct UserData: Decodable {
    let user: User?
}

struct User: Codable, Identifiable {
    let id: String?
    var name: String
    var email: String
    var phone: String
    var cpfMasked: String
    var city: String
    var state: String
    var healthUnit: String
    var isActive: Bool
    var isAdmin: Bool
    var caregivers: [Caregiver]
    var subscriptionStatus: String?
    var isPremium: Bool
    var consentDate: String?

    init(
        id: String? = nil,
        name: String = "",
        email: String = "",
        phone: String = "",
        cpfMasked: String = "",
        city: String = "",
        state: String = "",
        healthUnit: String = "",
        isActive: Bool = true,
        isAdmin: Bool = false,
        caregivers: [Caregiver] = [],
        subscriptionStatus: String? = nil,
        isPremium: Bool = false,
        consentDate: String? = nil
    ) {
        self.id = id
        self.name = name
        self.email = email
        self.phone = phone
        self.cpfMasked = cpfMasked
        self.city = city
        self.state = state
        self.healthUnit = healthUnit
        self.isActive = isActive
        self.isAdmin = isAdmin
        self.caregivers = caregivers
        self.subscriptionStatus = subscriptionStatus
        self.isPremium = isPremium
        self.consentDate = consentDate
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id)
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        email = try c.decodeIfPresent(String.self, forKey: .email) ?? ""
        phone = try c.decodeIfPresent(String.self, forKey: .phone) ?? ""
        cpfMasked = try c.decodeIfPresent(String.self, forKey: .cpfMasked) ?? ""
        city = try c.decodeIfPresent(String.self, forKey: .city) ?? ""
        state = try c.decodeIfPresent(String.self, forKey: .state) ?? ""
        healthUnit = try c.decodeIfPresent(String.self, forKey: .healthUnit) ?? ""
        isActive = try c.decodeIfPresent(Bool.self, forKey: .isActive) ?? true
        isAdmin = try c.decodeIfPresent(Bool.self, forKey: .isAdmin) ?? false
        caregivers = try c.decodeIfPresent([Caregiver].self, forKey: .caregivers) ?? []
        subscriptionStatus = try c.decodeIfPresent(String.self, forKey: .subscriptionStatus)
        isPremium = try c.decodeIfPresent(Bool.self, forKey: .isPremium) ?? false
        consentDate = try c.decodeIfPresent(String.self, forKey: .consentDate)
    }
}

struct Caregiver: Codable, Identifiable, Hashable {
    var id: String { "\(name)-\(phone)" }
    var name: String
    var phone: String
    var relation: String
}

struct ProfileUpdateRequest: Encodable {
    let name: String?
    let email: String?
    let phone: String?
    let city: String?
    let state: String?
    let caregivers: [Caregiver]?
    let healthVitalSigns: [VitalSignsPayload]?
    let medicalConsultations: [ConsultationPayload]?

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encodeIfPresent(name, forKey: .name)
        try c.encodeIfPresent(email, forKey: .email)
        try c.encodeIfPresent(phone, forKey: .phone)
        try c.encodeIfPresent(city, forKey: .city)
        try c.encodeIfPresent(state, forKey: .state)
        try c.encodeIfPresent(caregivers, forKey: .caregivers)
        try c.encodeIfPresent(healthVitalSigns, forKey: .healthVitalSigns)
        try c.encodeIfPresent(medicalConsultations, forKey: .medicalConsultations)
    }

    private enum CodingKeys: String, CodingKey {
        case name, email, phone, city, state, caregivers, healthVitalSigns, medicalConsultations
    }
}

struct MedicationListData: Decodable {
    let medications: [Medication]

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        medications = try c.decodeIfPresent([Medication].self, forKey: .medications) ?? []
    }
}

struct MedicationData: Decodable {
    let medication: Medication?
}

struct Medication: Codable, Identifiable, Hashable {
    let id: String?
    var name: String
    var dosage: String
    var frequency: String
    var times: [String]
    var weekDays: [Int]?
    var instructions: String?
    var stock: Int
    var active: Bool
    var supplier: Supplier?

    init(
        id: String? = nil,
        name: String = "",
        dosage: String = "",
        frequency: String = "daily",
        times: [String] = [],
        weekDays: [Int]? = nil,
        instructions: String? = nil,
        stock: Int = 0,
        active: Bool = true,
        supplier: Supplier? = nil
    ) {
        self.id = id
        self.name = name
        self.dosage = dosage
        self.frequency = frequency
        self.times = times
        self.weekDays = weekDays
        self.instructions = instructions
        self.stock = stock
        self.active = active
        self.supplier = supplier
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id)
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        dosage = try c.decodeIfPresent(String.self, forKey: .dosage) ?? ""
        frequency = try c.decodeIfPresent(String.self, forKey: .frequency) ?? "daily"
        times = try c.decodeIfPresent([String].self, forKey: .times) ?? []
        weekDays = try c.decodeIfPresent([Int].self, forKey: .weekDays)
        instructions = try c.decodeIfPresent(String.self, forKey: .instructions)
        stock = try c.decodeIfPresent(Int.self, forKey: .stock) ?? 0
        active = try c.decodeIfPresent(Bool.self, forKey: .active) ?? true
        supplier = try c.decodeIfPresent(Supplier.self, forKey: .supplier)
    }
}

struct Supplier: Codable, Hashable {
    var name: String
    var phone: String
}

struct MedicationRequest: Encodable {
    let name: String
    let dosage: String
    let frequency: String
    let times: [String]
    let weekDays: [Int]?
    let instructions: String?
    let stock: Int
    let active: Bool
    let supplier: Supplier?
}

struct HistoryListData: Decodable {
    let history: [HistoryEntry]

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        history = try c.decodeIfPresent([HistoryEntry].self, forKey: .history) ?? []
    }
}

struct HistoryEntry: Codable, Identifiable, Hashable {
    let id: String?
    let medicationId: String?
    let medicationName: String
    let dosage: String
    let scheduleTime: String
    let status: String
    let takenAt: String?
    let scheduledFor: String?
    let notes: String?

    init(
        id: String? = nil,
        medicationId: String? = nil,
        medicationName: String = "",
        dosage: String = "",
        scheduleTime: String = "",
        status: String = "",
        takenAt: String? = nil,
        scheduledFor: String? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.medicationId = medicationId
        self.medicationName = medicationName
        self.dosage = dosage
        self.scheduleTime = scheduleTime
        self.status = status
        self.takenAt = takenAt
        self.scheduledFor = scheduledFor
        self.notes = notes
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id)
        medicationId = try c.decodeIfPresent(String.self, forKey: .medicationId)
        medicationName = try c.decodeIfPresent(String.self, forKey: .medicationName) ?? ""
        dosage = try c.decodeIfPresent(String.self, forKey: .dosage) ?? ""
        scheduleTime = try c.decodeIfPresent(String.self, forKey: .scheduleTime) ?? ""
        status = try c.decodeIfPresent(String.self, forKey: .status) ?? ""
        takenAt = try c.decodeIfPresent(String.self, forKey: .takenAt)
        scheduledFor = try c.decodeIfPresent(String.self, forKey: .scheduledFor)
        notes = try c.decodeIfPresent(String.self, forKey: .notes)
    }
}

struct HistoryRequest: Encodable {
    let medicationId: String
    let medicationName: String
    let dosage: String
    let scheduleTime: String
    let status: String
    let scheduledFor: String
    let takenAt: String?
    let notes: String?
}

struct Adherence: Decodable {
    let period: String
    let total: Int
    let taken: Int
    let missed: Int
    let adherenceRate: String

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        period = try c.decodeIfPresent(String.self, forKey: .period) ?? ""
        total = try c.decodeIfPresent(Int.self, forKey: .total) ?? 0
        taken = try c.decodeIfPresent(Int.self, forKey: .taken) ?? 0
        missed = try c.decodeIfPresent(Int.self, forKey: .missed) ?? 0
        adherenceRate = try c.decodeIfPresent(String.self, forKey: .adherenceRate) ?? "0%"
    }
}

struct VitalSignsPayload: Encodable {
    let date: String
    var systolic: Int?
    var diastolic: Int?
    var heartRate: Int?
    var spo2: Double?
    var glucose: Double?
    var glucoseContext: String
    var hba1c: Double?
    var temperature: Double?
}

struct ConsultationPayload: Encodable, Identifiable {
    let id: String
    let dateTime: String
    let professional: String
    let location: String
    let notes: String
}

extension String {
    var digitsOnly: String { filter { $0.isNumber } }
}
