import SwiftUI

struct MedicationsView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showingNew = false
    @State private var editing: Medication?

    var body: some View {
        List {
            if model.medications.isEmpty {
                ContentUnavailableView(
                    "Nenhum medicamento",
                    systemImage: "pills",
                    description: Text("Adicione um medicamento para começar sua agenda.")
                )
                .listRowBackground(Color.clear)
            } else {
                ForEach(model.medications) { medication in
                    Button { editing = medication } label: {
                        MedicationRow(medication: medication)
                    }
                    .buttonStyle(.plain)
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            Task { await model.deleteMedication(medication) }
                        } label: {
                            Label("Excluir", systemImage: "trash")
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .background(BrandColors.background)
        .navigationTitle("Medicamentos")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingNew = true } label: {
                    Label("Adicionar", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $showingNew) {
            NavigationStack { MedicationEditorView(medication: nil) }
        }
        .sheet(item: $editing) { medication in
            NavigationStack { MedicationEditorView(medication: medication) }
        }
    }
}

private struct MedicationRow: View {
    let medication: Medication

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "pills.fill")
                .font(.title3)
                .foregroundStyle(BrandColors.teal)
                .frame(width: 38, height: 38)
                .background(BrandColors.teal.opacity(0.12), in: Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(medication.name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                Text("\(medication.dosage) · \(medication.times.joined(separator: ", "))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if let instructions = medication.instructions, !instructions.isEmpty {
                    Text(instructions)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }
}
