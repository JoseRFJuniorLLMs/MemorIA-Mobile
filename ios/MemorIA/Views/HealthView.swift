import SwiftUI

struct HealthView: View {
    @EnvironmentObject private var model: AppModel
    @State private var systolic = ""
    @State private var diastolic = ""
    @State private var heartRate = ""
    @State private var spo2 = ""
    @State private var glucose = ""

    var body: some View {
        Form {
            Section {
                Text("Registre uma medição para acompanhar sua saúde e incluí-la no relatório enviado pelo MemorIA.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Section("Pressão arterial") {
                HStack {
                    TextField("Sistólica", text: $systolic).keyboardType(.numberPad)
                    Text("/").foregroundStyle(.secondary)
                    TextField("Diastólica", text: $diastolic).keyboardType(.numberPad)
                    Text("mmHg").foregroundStyle(.secondary)
                }
            }

            Section("Outras medições") {
                TextField("Frequência cardíaca (bpm)", text: $heartRate).keyboardType(.numberPad)
                TextField("Saturação (SpO₂ %)", text: $spo2).keyboardType(.decimalPad)
                TextField("Glicemia (mg/dL)", text: $glucose).keyboardType(.decimalPad)
            }

            Section {
                PrimaryButton(title: "Sincronizar medição", systemImage: "arrow.up.circle") {
                    Task { await save() }
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                .listRowBackground(Color.clear)
            }

            Section {
                Label("O registro é apenas informativo e não substitui avaliação médica.", systemImage: "info.circle")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Saúde")
        .background(BrandColors.background)
    }

    private func save() async {
        await model.saveHealth(
            systolic: Int(systolic),
            diastolic: Int(diastolic),
            heartRate: Int(heartRate),
            spo2: Double(spo2.replacingOccurrences(of: ",", with: ".")),
            glucose: Double(glucose.replacingOccurrences(of: ",", with: "."))
        )
    }
}
