<div align="center">

# MemorIA Mobile 📱💊

**Lembrete de medicamentos para idosos — cliente Android nativo**

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](https://developer.android.com/tools/releases/platforms)
[![targetSdk](https://img.shields.io/badge/targetSdk-34-blue)](https://developer.android.com/tools/releases/platforms)
[![version](https://img.shields.io/badge/version-1.0.0-orange)](#)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

</div>

App **Android nativo** (Kotlin + Jetpack Compose, Material 3) do **MemorIA** — um
lembrete de medicamentos pensado para **idosos**. Consome **o mesmo backend** que
o frontend web (PWA), replicando as funções centrais, com **um diferencial: os
avisos vão pelo WhatsApp**, entregues pelo servidor de forma confiável — mesmo
com o telemóvel do idoso desligado ou o app fechado.

> Este repositório é apenas o **cliente móvel nativo**. O backend (Node.js /
> Express) e o frontend web (PWA) vivem noutro repositório e **não são alterados
> por este projeto**.

> ⚠️ O MemorIA é uma ferramenta auxiliar e **não substitui orientação médica**.

---

## 📑 Índice

- [O que o app faz](#-o-que-o-app-faz-paridade-com-o-frontend-web)
- [O diferencial — WhatsApp](#-o-diferencial--notificações-pelo-whatsapp)
- [Stack técnica](#️-stack-técnica)
- [Arquitetura](#️-arquitetura)
- [Como compilar e rodar](#️-como-compilar-e-rodar)
- [Versão iOS](#-versão-ios)
- [Contrato de API](#-contrato-de-api-referência)
- [Escopo desta versão](#-escopo-desta-versão)
- [Contribuir](#-contribuir)
- [Licença](#-licença)

---

## ✨ O que o app faz (paridade com o frontend web)

| Recurso | Tela | Endpoint(s) do backend |
|---|---|---|
| **Login / Cadastro** (CPF + senha, LGPD) | `LoginScreen`, `RegisterScreen` | `POST /auth/login`, `POST /auth/register` |
| **Agenda de hoje** + marcar dose (tomei / adiar / não tomei) | `MedicationsScreen` | `GET /medications`, `POST /history` |
| **CRUD de medicamentos** (nome, dose, frequência, horários, dias, estoque) | `MedicationEditScreen` | `POST/PUT/DELETE /medications` |
| **Histórico + adesão** (30 dias) | `HistoryScreen` | `GET /history`, `GET /history/adherence` |
| **Avisos por WhatsApp** (nº do paciente + cuidadores) | `WhatsAppScreen` | `PUT /auth/profile` |
| **Ajustes** (perfil, URL do servidor, testar conexão, sair) | `SettingsScreen` | `GET /auth/me`, `GET /health` |

**Acessibilidade** é requisito, não enfeite: tipografia maior, alvos de toque
generosos e Material 3 com bom contraste — o público-alvo são pessoas idosas.

---

## 📲 O diferencial — notificações pelo WhatsApp

O frontend web usa **notificações locais** no dispositivo (Capacitor). Aqui, o
canal de aviso é o **WhatsApp**, entregue pelo **servidor**:

1. Na tela **WhatsApp**, regista-se o **número do paciente** e os **cuidadores**
   (nome, telefone, parentesco) — persistidos via `PUT /auth/profile`.
2. O serviço de lembretes do backend envia:
   - o **lembrete de cada dose** ao WhatsApp do paciente, no horário;
   - o **alerta de dose perdida** aos cuidadores, se a dose não for confirmada
     dentro da tolerância configurada.
3. Como a entrega é **server-side**, funciona mesmo com o telemóvel do idoso
   **desligado** — que é exatamente a proposta de valor do MemorIA.

O app **não** depende de notificações locais para os lembretes; configura o canal
e confia no servidor. Há também um atalho *"Abrir conversa no WhatsApp"* via
`https://wa.me/…`.

---

## 🛠️ Stack técnica

| Camada | Tecnologia |
|---|---|
| **Linguagem** | Kotlin 2.0.20 |
| **UI** | Jetpack Compose · Material 3 · Navigation Compose |
| **Estado** | `ViewModel` + `StateFlow`, coletado com `collectAsStateWithLifecycle` |
| **Rede** | Retrofit + OkHttp · **Moshi (codegen via KSP)** · interceptor JWT |
| **Persistência local** | DataStore (token JWT + URL do backend) |
| **DI** | Manual (`AppGraph`) — sem Hilt, um único repositório |
| **Build** | Gradle (Kotlin DSL) · AGP 8.5 · version catalog (`libs.versions.toml`) |
| **SDK** | `compileSdk`/`targetSdk` 34 · `minSdk` 26 (Android 8.0) |

---

## 🏗️ Arquitetura

```
com.memoria.mobile
├── MemoriaApp / MainActivity        # Application (grafo de DI) + host Compose
├── di/AppGraph                      # DI manual (sem Hilt) — 1 repositório
├── data/
│   ├── remote/                      # Retrofit ApiService, DTOs (Moshi codegen),
│   │                                #   ApiProvider (rebuild ao trocar de URL),
│   │                                #   SessionState (token volátil p/ interceptor)
│   ├── local/PreferencesStore       # DataStore: token JWT + URL do backend
│   └── MemoriaRepository            # orquestra API + sessão; ApiResult<T>
└── ui/
    ├── theme/                       # Material 3, tipografia acessível
    ├── nav/                         # NavHost + bottom bar (4 abas)
    ├── auth/ meds/ history/ whatsapp/ settings/   # telas + ViewModels (StateFlow)
    └── common/                      # componentes, helper de ViewModel, extensões
```

- **Base URL:** igual ao web — o app acrescenta `/api` sozinho. Padrão em
  `DEFAULT_API_BASE_URL` (BuildConfig), **sobrescrevível em Ajustes** em runtime.
- **Token JWT:** injetado por um interceptor a partir de um `SessionState`
  volátil; persistido no DataStore entre sessões.
- **Segurança de rede:** HTTPS por padrão; cleartext permitido **apenas** para
  `10.0.2.2`/`localhost` (dev no emulador) via `network_security_config.xml`.

---

## ▶️ Como compilar e rodar

### Pré-requisitos
- **Android Studio** (Ladybug 2024.2+), que já traz o JDK e o Android SDK.
- **JDK 17** para o Gradle na linha de comando.
  ⚠️ AGP 8.5 / Gradle 8.9 podem **não** funcionar com JDK 25+ — aponte
  `JAVA_HOME` para um JDK 17 se compilar fora do Studio.

### Pelo Android Studio (recomendado)
```bash
git clone https://github.com/valdenilsonrocha-web/MemorIA-androide.git
```
1. `File › Open` na pasta clonada. Na primeira sincronização, o Studio **gera o
   Gradle wrapper** e baixa as dependências.
2. Confirme a URL do backend:
   - padrão em `app/build.gradle.kts` → `DEFAULT_API_BASE_URL`;
   - **ou** troque em runtime em **Ajustes › Servidor** (ex.: emulador a apontar
     ao backend local → `http://10.0.2.2:3001`).
3. **Run › app** num emulador ou dispositivo.

### Pela linha de comando
```bash
# Gera o wrapper uma vez, se não abrir pelo Studio:
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug            # APK em app/build/outputs/apk/debug/
```

### Apontar ao backend local (dev)
Com o backend do MemorIA a correr localmente (`http://localhost:3001`), no app
(emulador) use **Ajustes › Servidor** → `http://10.0.2.2:3001`.
O `10.0.2.2` é como o emulador Android alcança o `localhost` da máquina.

##  Versão iOS

O cliente iOS nativo em SwiftUI está em [`ios/`](ios/README.md). Ele usa o
mesmo contrato REST e backend, com autenticação, agenda de doses, medicamentos,
histórico/adesão, saúde, WhatsApp/cuidadores, privacidade LGPD e ajustes de
servidor. Abra [`ios/MemorIA.xcodeproj`](ios/MemorIA.xcodeproj) no Xcode 15+ em
um Mac e execute em um simulador ou iPhone com iOS 17+.

---

## 🔌 Contrato de API (referência)

- **Base:** `<servidor>/api`
- **Auth:** JWT em `Authorization: Bearer <token>`
- **Envelope de resposta:** `{ success, message, data }`

Espelha o `apiService.js` do frontend web. Ver `data/remote/ApiService.kt` para a
lista completa de endpoints e DTOs.

---

## 📌 Escopo desta versão

**Incluído:** autenticação, medicamentos (agenda + CRUD + marcação de dose),
histórico/adesão, configuração de WhatsApp (paciente + cuidadores), ajustes de
servidor.

**Fora do escopo v1** (existem no web, podem entrar depois):
pagamento/assinatura, receitas (*prescriptions*), assistente de voz, painel admin
e modo offline com sincronização. A arquitetura (`MemoriaRepository` +
`ApiService`) já está pronta para recebê-los.

---

## 🤝 Contribuir

1. Faça um *fork* e crie um branch: `git checkout -b feat/minha-melhoria`.
2. Siga o estilo existente (uma tela = `Screen` + `ViewModel` com `StateFlow`).
3. Garanta que compila: `./gradlew assembleDebug`.
4. Abra um *Pull Request* descrevendo a mudança.

---

## 📄 Licença

**MIT** — ver [LICENSE](LICENSE).
