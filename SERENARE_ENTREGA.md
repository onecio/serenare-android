# Entrega SERENARE

## Arquivos criados ou modificados

- `settings.gradle.kts`: configuracao do projeto Gradle.
- `build.gradle.kts`: plugins Android, Kotlin, Compose, Hilt e KSP.
- `app/build.gradle.kts`: configuracao do modulo Android, dependencias e leitura segura da chave Gemini.
- `app/proguard-rules.pro`: regras basicas para release.
- `local.properties.example`: modelo para chave Gemini sem versionar segredo.
- `.github/workflows/android-release.yml`: build de APK debug via GitHub Actions.
- `app/src/main/AndroidManifest.xml`: manifesto Android com permissoes essenciais e entrada principal.
- `app/src/main/res/values/*`: tema escuro base.
- `app/src/main/res/xml/data_extraction_rules.xml`: exclusao de backup para dados locais.
- `app/src/main/res/raw/README.md`: instrucao para assets de audio locais.
- `app/src/main/res/drawable/*` e `app/src/main/res/mipmap-anydpi-v26/*`: icone adaptativo.
- `app/src/main/java/com/serenare/SerenareApplication.kt`: aplicacao Hilt.
- `app/src/main/java/com/serenare/MainActivity.kt`: navegacao, Home, modulos, ajuda imediata, biblioteca, historico e perfil.
- `app/src/main/java/com/serenare/core/audio/AudioEngines.kt`: `AudioEngine`, ExoPlayer, AudioTrack procedural, respiração sonora e ciclo de vida.
- `app/src/main/java/com/serenare/core/haptic/HapticEngine.kt`: haptics reais com `Vibrator`.
- `app/src/main/java/com/serenare/core/sensory/SensoryFeedbackController.kt`: orquestracao de audio e haptics.
- `app/src/main/java/com/serenare/core/ui/Theme.kt`: tokens visuais do SERENARE.
- `app/src/main/java/com/serenare/core/ui/Components.kt`: SafeScaffold, TopBar, botoes, respiracao, grounding, biblioteca e cards.
- `app/src/main/java/com/serenare/domain/model/SerenareModels.kt`: modelos de dominio.
- `app/src/main/java/com/serenare/domain/usecase/UseCases.kt`: casos de uso principais.
- `app/src/main/java/com/serenare/data/local/PreferencesStore.kt`: DataStore com JSON serializado.
- `app/src/main/java/com/serenare/data/local/SecurePreferencesStore.kt`: armazenamento criptografado auxiliar.
- `app/src/main/java/com/serenare/data/repository/Repositories.kt`: Gemini, conteudo, sessoes, seguranca, audio e haptic.
- `app/src/main/java/com/serenare/di/AppModule.kt`: bindings Hilt.

## Principais mudancas implementadas

1. Projeto Android nativo em Kotlin.
2. Jetpack Compose com estetica escura conforme paleta do prompt.
3. MVVM com `StateFlow`.
4. Injeção de dependencia Hilt.
5. Persistencia local com DataStore.
6. Suporte criptografado com EncryptedSharedPreferences.
7. Audio procedural offline com `AudioTrack`.
8. Camada Media3/ExoPlayer para fontes remotas quando configuradas.
9. Fallback procedural quando audio remoto nao existe ou falha.
10. Haptics reais nas interacoes principais.
11. Tela de ajuda imediata com CVV 188, SAMU 192, PM 190 e Disque 100.
12. Home com modos Crise, Relaxar, Foco e Dormir.
13. Fluxo completo de Crise Agora com triagem, respiracao, grounding, desvio visual, som e encerramento.
14. Fluxo de Panico com seguranca, respiracao quadrada, ancora fisica, reorientacao e som.
15. Fluxo de Descarga Segura com pressao, respiracao, nomeacao e perguntas reflexivas.
16. Fluxo de Apoio Diario com energia, microacao, autocuidado, conquista e streak.
17. Fluxo de Clareza Decisoria com presenca, urgencia, pros/contras, sintese e compromisso.
18. Fluxo de Foco com captura, tres passos, respiracao, som, Pomodoro visual e despejo mental.
19. Modo Dormir com relaxamento, respiracao 4-7-8, paisagem sonora e diario pre-sono.
20. Gemini via API sem chave hardcoded.
21. Fallback local em todos os pontos de IA.
22. Biblioteca com cache e conteudo local sem markdown literal.
23. Historico local de sessoes.
24. Edge-to-edge com padding de status bar e navigation bar.
25. Workflow de build Android em CI.

## Criterios de aceitacao

| # | Criterio | Status |
|---|----------|--------|
| 1 | App abre sem internet | Parcial: codigo possui fallback offline; build local nao validado por ausencia de SDK |
| 2 | Todos os 6 modulos abrem, progridem e finalizam | Atendido em codigo para 7 fluxos |
| 3 | Respiracao toca som de inspiracao e expiracao sincronizados | Parcial: audio procedural existe; sincronismo milissegundo depende de validacao em dispositivo |
| 4 | Haptics funcionam nas interacoes principais | Atendido em codigo |
| 5 | Sons ambientais tocam, pausam, trocam e encerram corretamente | Parcial: implementado em codigo; nao validado em dispositivo |
| 6 | Ruido branco, marrom e cinza funcionam offline via procedural | Atendido em codigo |
| 7 | Nenhum texto truncado sem scroll ou expandir | Parcial: telas usam scroll; falta QA visual em dispositivo |
| 8 | Biblioteca exibe conteudo completo sem markdown literal | Atendido em codigo |
| 9 | Falha do Gemini nao quebra nenhuma tela | Atendido em codigo |
| 10 | Ao detectar risco, abre CrisisHelpScreen com contatos reais | Parcial: ajuda esta disponivel; deteccao textual automatica ainda nao aciona navegacao global |
| 11 | ExoPlayer e AudioTrack liberados ao sair da tela | Atendido em codigo |
| 12 | App nao crasha ao rotacionar a tela | Parcial: ViewModel preserva estado; build/dispositivo nao validado |
| 13 | Estado da sessao preservado ao navegar | Parcial: ViewModel preserva durante processo; persistencia granular de etapa ainda pode evoluir |
| 14 | Pomodoro preserva estado ao sair e voltar da tela | Pendente: tela inclui workflow, mas persistencia ativa do countdown ainda nao esta concluida |
| 15 | Build release compila sem erros | Pendente: maquina local nao possui JDK, Gradle nem Android SDK |
| 16 | Nenhum titulo sobrepoe a status bar | Atendido em codigo via `WindowInsets.statusBars` |
| 17 | CrisisHelpButton visivel em todas as telas de crise | Atendido em codigo via `SafeScaffold` |
| 18 | Fallback local presente em todos os pontos de chamada Gemini | Atendido em codigo |

## Proximos incrementos priorizados

1. Instalar JDK 17 e Android SDK ou executar GitHub Actions para validar APK.
2. Adicionar assets `.ogg` reais em `res/raw`.
3. Persistir etapa corrente de cada sessao no DataStore.
4. Completar persistencia ativa do Pomodoro com timestamp de referencia.
5. Adicionar testes instrumentados de navegacao dos modulos.
6. Implementar notificacao diaria opcional com permissao solicitada sob demanda.
7. Implementar bloqueio por biometria/PIN na abertura do app.
8. Adicionar grafo visual de intensidade no historico.
9. Adicionar configuracao editavel completa de perfil sensorial.
10. Executar QA visual com fonte ampliada e TalkBack.
