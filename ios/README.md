# MemorIA para iOS

Cliente iOS nativo em SwiftUI que usa o mesmo backend REST do app Android/PWA.
O projeto cobre autenticação, agenda de doses, CRUD de medicamentos, histórico
com adesão, medições de saúde, WhatsApp/cuidadores, privacidade LGPD e ajustes do
servidor.

## Abrir e executar

Requer macOS com Xcode 15 ou superior. Abra `MemorIA.xcodeproj`, escolha um
simulador iOS 17+ ou um iPhone, configure o Team em **Signing & Capabilities** e
execute (`⌘R`). O bundle id padrão é `com.memoria.mobile.ios`.

O endereço padrão é `https://35.247.217.66.nip.io`, igual ao Android. Para um
backend local, altere o endereço em **Mais → Servidor e sessão**. O app remove um
`/api` final e acrescenta o prefixo automaticamente; no simulador, um servidor
local pode ser acessado por `http://127.0.0.1:3001`.

## Estrutura

- `MemorIA/APIClient.swift` — cliente `URLSession`, envelope `{ success, data }`, JWT e mensagens de erro.
- `MemorIA/AppModel.swift` — estado de sessão, sincronização e operações de domínio.
- `MemorIA/Models.swift` — DTOs compatíveis com `ApiService.kt`.
- `MemorIA/Views/` — telas SwiftUI e componentes acessíveis.
- `MemorIA/KeychainStore.swift` — armazenamento seguro do token JWT.

O build do iOS precisa ser feito em macOS/Xcode; este repositório também pode ser
editado no Windows, mas não há toolchain Apple disponível aqui para compilar o
target.
