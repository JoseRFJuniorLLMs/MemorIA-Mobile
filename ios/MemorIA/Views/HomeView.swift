import SwiftUI

struct ScheduledDose: Identifiable {
    let medication: Medication
    let time: String

    var id: String { "\(medication.id ?? medication.name)-\(time)" }
}

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showingAdd = false

    private var doses: [ScheduledDose] {
        model.medications
            .filter { $0.active }
            .flatMap { medication in medication.times.map { ScheduledDose(medication: medication, time: $0) } }
            .sorted { $0.time < $1.time }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(greeting)
                        .font(.title.weight(.bold))
                    Text(Date.now.formatted(.dateTime.weekday(.wide).day().month(.wide)))
                        .foregroundStyle(.secondary)
                }

                CardContainer {
                    HStack(spacing: 16) {
                        Image(systemName: "bell.badge.fill")
                            .font(.title)
                            .foregroundStyle(BrandColors.orange)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Lembretes pelo WhatsApp")
                                .font(.headline)
                            Text("Mantenha seu contato e os cuidadores atualizados em Mais › WhatsApp.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                SectionTitle(title: "Agenda de hoje", actionTitle: "Adicionar") { showingAdd = true }

                if doses.isEmpty {
                    CardContainer {
                        VStack(spacing: 12) {
                            Image(systemName: "calendar.badge.plus")
                                .font(.system(size: 34))
                                .foregroundStyle(BrandColors.teal)
                            Text("Nenhum medicamento cadastrado")
                                .font(.headline)
                            Text("Cadastre seu primeiro medicamento para acompanhar as doses do dia.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                            Button("Cadastrar medicamento") { showingAdd = true }
                                .buttonStyle(.borderedProminent)
                                .tint(BrandColors.teal)
                        }
                        .frame(maxWidth: .infinity)
                    }
                } else {
                    ForEach(doses) { dose in
                        DoseCard(dose: dose)
                    }
                }

                if let adherence = model.adherence {
                    CardContainer {
                        HStack {
                            VStack(alignment: .leading, spacing: 5) {
                                Text("Adesão nos últimos 30 dias")
                                    .font(.headline)
                                Text("\(adherence.taken) tomadas de \(adherence.total) doses")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(adherence.adherenceRate)
                                .font(.title2.weight(.bold))
                                .foregroundStyle(BrandColors.teal)
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(BrandColors.background)
        .navigationTitle("Início")
        .toolbarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingAdd) {
            NavigationStack { MedicationEditorView(medication: nil) }
        }
    }

    private var greeting: String {
        let name = model.user?.name.split(separator: " ").first.map(String.init) ?? ""
        return name.isEmpty ? "Olá!" : "Olá, \(name)!"
    }
}

private struct DoseCard: View {
    @EnvironmentObject private var model: AppModel
    let dose: ScheduledDose

    private var alreadyTaken: Bool {
        model.history.contains {
            $0.medicationId == dose.medication.id &&
                $0.scheduleTime == dose.time &&
                $0.status == "taken" &&
                ($0.scheduledFor?.prefix(10) == todayPrefix)
        }
    }

    private var todayPrefix: Substring {
        ISO8601DateFormatter().string(from: Date()).prefix(10)
    }

    var body: some View {
        CardContainer {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(dose.time)
                            .font(.title2.weight(.bold))
                            .foregroundStyle(BrandColors.teal)
                        Text(dose.medication.name)
                            .font(.headline)
                        Text(dose.medication.dosage)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    if alreadyTaken {
                        Label("Tomada", systemImage: "checkmark.circle.fill")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.green)
                    }
                }
                if let instructions = dose.medication.instructions, !instructions.isEmpty {
                    Text(instructions)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                if !alreadyTaken {
                    HStack(spacing: 10) {
                        Button("Tomei") { Task { await model.record(dose.medication, at: dose.time, status: "taken") } }
                            .buttonStyle(.borderedProminent)
                            .tint(BrandColors.teal)
                        Button("Adiar") { Task { await model.record(dose.medication, at: dose.time, status: "snoozed") } }
                            .buttonStyle(.bordered)
                        Button("Não tomei") { Task { await model.record(dose.medication, at: dose.time, status: "missed") } }
                            .buttonStyle(.bordered)
                            .tint(.red)
                    }
                    .controlSize(.large)
                }
            }
        }
    }
}
