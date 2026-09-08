import SwiftUI

struct MedicationEditorView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let medication: Medication?

    @State private var name = ""
    @State private var dosage = ""
    @State private var frequency = "daily"
    @State private var times: [String] = ["08:00"]
    @State private var instructions = ""
    @State private var stock = 0
    @State private var active = true

    private var editingID: String? { medication?.id }

    var body: some View {
        Form {
            Section("Medicamento") {
                TextField("Nome", text: $name)
                TextField("Dose (ex.: 1 comprimido)", text: $dosage)
                Picker("Frequência", selection: $frequency) {
                    Text("Todos os dias").tag("daily")
                    Text("Dias alternados").tag("alternate")
                    Text("Semanal").tag("weekly")
                }
                Stepper("Estoque: \(stock)", value: $stock, in: 0...999)
                Toggle("Medicamento ativo", isOn: $active)
            }

            Section {
                ForEach(times.indices, id: \.self) { index in
                    HStack {
                        TextField("HH:MM", text: binding(for: index))
                            .keyboardType(.numbersAndPunctuation)
                        Spacer()
                        if times.count > 1 {
                            Button(role: .destructive) { times.remove(at: index) } label: {
                                Image(systemName: "minus.circle.fill")
                            }
                            .buttonStyle(.borderless)
                        }
                    }
                }
                Button { times.append("20:00") } label: {
                    Label("Adicionar horário", systemImage: "plus.circle")
                }
            } header: {
                Text("Horários")
            } footer: {
                Text("Use o formato 24 horas, por exemplo 08:00 ou 20:00.")
            }

            Section("Orientações") {
                TextField("Observações (opcional)", text: $instructions, axis: .vertical)
                    .lineLimit(2...5)
            }
        }
        .navigationTitle(editingID == nil ? "Novo medicamento" : "Editar medicamento")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancelar") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Salvar") { Task { await save() } }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || dosage.isEmpty || times.isEmpty)
            }
        }
        .onAppear { loadMedication() }
    }

    private func binding(for index: Int) -> Binding<String> {
        Binding(
            get: { times[index] },
            set: { times[index] = $0 }
        )
    }

    private func loadMedication() {
        guard let medication else { return }
        name = medication.name
        dosage = medication.dosage
        frequency = medication.frequency
        times = medication.times.isEmpty ? ["08:00"] : medication.times
        instructions = medication.instructions ?? ""
        stock = medication.stock
        active = medication.active
    }

    private func save() async {
        let value = Medication(
            id: editingID,
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            dosage: dosage.trimmingCharacters(in: .whitespacesAndNewlines),
            frequency: frequency,
            times: times.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty },
            instructions: instructions.isEmpty ? nil : instructions,
            stock: stock,
            active: active
        )
        await model.saveMedication(value, existingID: editingID)
        if model.errorMessage == nil { dismiss() }
    }
}
