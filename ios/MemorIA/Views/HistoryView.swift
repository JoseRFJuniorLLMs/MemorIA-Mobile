import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        List {
            if let adherence = model.adherence {
                Section {
                    CardContainer {
                        HStack {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Adesão")
                                    .font(.headline)
                                Text("Últimos 30 dias")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(adherence.adherenceRate)
                                .font(.largeTitle.weight(.bold))
                                .foregroundStyle(BrandColors.teal)
                        }
                        HStack(spacing: 12) {
                            metric("Tomadas", value: adherence.taken, color: .green)
                            metric("Não tomadas", value: adherence.missed, color: .red)
                            metric("Total", value: adherence.total, color: .secondary)
                        }
                        .padding(.top, 10)
                    }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }
            }

            Section("Registros recentes") {
                if model.history.isEmpty {
                    ContentUnavailableView("Sem registros", systemImage: "clock", description: Text("As doses registradas aparecerão aqui."))
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(model.history) { entry in
                        HStack(spacing: 14) {
                            Image(systemName: icon(for: entry.status))
                                .foregroundStyle(color(for: entry.status))
                                .font(.title3)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(entry.medicationName)
                                    .font(.headline)
                                Text("\(entry.scheduleTime) · \(entry.dosage)")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            StatusPill(status: entry.status)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .background(BrandColors.background)
        .navigationTitle("Histórico")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { Task { try? await model.refreshData() } } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .accessibilityLabel("Atualizar histórico")
            }
        }
    }

    private func metric(_ title: String, value: Int, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(value)")
                .font(.title3.weight(.bold))
                .foregroundStyle(color)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func icon(for status: String) -> String {
        switch status { case "taken": return "checkmark.circle.fill"; case "missed": return "xmark.circle.fill"; default: return "clock.arrow.circlepath" }
    }

    private func color(for status: String) -> Color {
        switch status { case "taken": return .green; case "missed": return .red; default: return .orange }
    }
}
