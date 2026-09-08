import SwiftUI

enum BrandColors {
    static let teal = Color(red: 0.02, green: 0.45, blue: 0.43)
    static let tealDark = Color(red: 0.01, green: 0.29, blue: 0.29)
    static let orange = Color(red: 0.94, green: 0.45, blue: 0.18)
    static let background = Color(uiColor: .systemGroupedBackground)
}

struct CardContainer<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(uiColor: .secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct PrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if let systemImage {
                    Label(title, systemImage: systemImage)
                } else {
                    Text(title)
                }
            }
            .font(.headline)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 52)
        }
        .buttonStyle(.borderedProminent)
        .tint(BrandColors.teal)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct SectionTitle: View {
    let title: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack {
            Text(title)
                .font(.title3.weight(.semibold))
            Spacer()
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.semibold))
            }
        }
    }
}

struct StatusPill: View {
    let status: String

    private var color: Color {
        switch status.lowercased() {
        case "taken": return .green
        case "missed": return .red
        case "snoozed": return .orange
        default: return .secondary
        }
    }

    private var label: String {
        switch status.lowercased() {
        case "taken": return "Tomada"
        case "missed": return "Não tomada"
        case "snoozed": return "Adiada"
        default: return status.capitalized
        }
    }

    var body: some View {
        Text(label)
            .font(.caption.weight(.semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(color.opacity(0.12), in: Capsule())
    }
}

struct LoadingOverlay: View {
    let isVisible: Bool

    var body: some View {
        if isVisible {
            ZStack {
                Color.black.opacity(0.08).ignoresSafeArea()
                ProgressView()
                    .controlSize(.large)
                    .padding(24)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
            }
            .transition(.opacity)
        }
    }
}
