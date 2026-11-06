# SmartIsland 7.6.0 Community — entrega validada

## APK

- Arquivo: `SmartIsland-7.6.0-community-debug.apk`.
- Pacote: `com.agupta07505.smartisland`.
- Versão: 7.6.0, código 9. Android mínimo: 8.0 (API 26); target API 36.
- Tamanho: 26.837.576 bytes.
- SHA-256: `1d8b50f733a791b4b110b1578b31e6f5f8714f26a8c1ed45027104158ca3fc40`.
- Assinatura APK v2 verificada. Certificado debug SHA-256: `903fefc3237b6e55044459414a764a54d5b8776f5b0a8b680ed9f97f47e2ee8a`.

## Validação automática concluída

- `:app:testDebugUnitTest`: 1.836 testes, zero falhas, erros ou ignorados.
- `:app:assembleDebug`: BUILD SUCCESSFUL.
- `:app:lintDebug`: BUILD SUCCESSFUL; relatório XML sem erros nem avisos.
- `git diff --check`: sem problemas.
- O Gradle ainda emite aviso de funcionalidades obsoletas para a futura versão 10; isso não é um aviso do lint Android.

## Correções desta finalização

- Reorganizado o bloco de posição em `SmartIslandSettingsRepository`: valores inválidos de posição não interrompem a gravação das configurações de pet e skins.
- Ajustada a construção do comando shell de `POST_NOTIFICATIONS`. O chamador solicita essa concessão somente a partir da API 33.
- Adicionado teste garantindo a ausência da concessão quando `includeRuntimeNotifications=false`.
- Substituído estado genérico por `mutableLongStateOf` no cronômetro recolhido.

## Skins e documentação

- Seis skins pré-instaladas: Eclipse, Titanium, Saturn, Obsidian, Black Hole e Lightspeed.
- São recriações locais, não cópias oficiais nem reproduções comprovadamente idênticas do catálogo https://vision.kitostudio.pro/.
- Não foi confirmada licença para redistribuir assets do site. A entrega não deve ser anunciada como contendo as skins oficiais do Vision.
- Referência do formato: `docs/skins/README.md`.
- Tutorial de criação e importação: `docs/skins/TUTORIAL.md`.
- Prompt reutilizável para IA: `docs/skins/PROMPT-IA.md`.
- A importação usa arquivos JSON declarativos; não instala código, imagens ou novos renderizadores. Não há botão dedicado de exportação: preserve o JSON original para compartilhar.

## Limites e publicação

- Os testes automáticos não substituem validação desta APK no celular: verificar pet, música, notificações, chamadas e importação de JSON.
- O ZIP de código-fonte acompanha o estado desta entrega e preserva a licença GPL-3.0, sem SDK, caches, keystores ou credenciais.
- Não houve merge de PR nem publicação de release. A branch remota criada ainda não representa integralmente esta integração; o código completo está no ZIP.
