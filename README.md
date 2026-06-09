# SERENARE

Aplicativo Android nativo de autorregulacao emocional e sensorial, implementado em Kotlin com Jetpack Compose, MVVM, Hilt, StateFlow, DataStore, armazenamento criptografado auxiliar, Media3/ExoPlayer e AudioTrack procedural.

## Estrutura

- `app/src/main/java/com/serenare/core`: audio, haptic, feedback sensorial e componentes Compose.
- `app/src/main/java/com/serenare/domain`: modelos e casos de uso.
- `app/src/main/java/com/serenare/data`: persistencia local, repositorios, Gemini com fallback e conteudo local.
- `app/src/main/java/com/serenare/feature`: ViewModel e telas de modulos, biblioteca, historico e perfil.

## Configuracao

1. Instale JDK 17 e Android SDK.
2. Copie `local.properties.example` para `local.properties`.
3. Configure a chave Gemini sem versiona-la:

```properties
GEMINI_API_KEY=sua_chave
```

4. Para assets locais, adicione em `app/src/main/res/raw/`:

```text
breath_in.ogg
breath_out.ogg
success.ogg
transition.ogg
grounding_tone.ogg
rain_short.ogg
ocean_short.ogg
forest_short.ogg
wind_light.ogg
```

O app ja possui geracao procedural por `AudioTrack` para operar offline quando esses arquivos ou URLs remotas nao estiverem disponiveis.

## Build

Com JDK 17 e Android SDK disponiveis:

```powershell
gradle :app:assembleDebug
gradle :app:assembleRelease
```

Tambem ha workflow em `.github/workflows/android-release.yml` para gerar APK debug via GitHub Actions.

## Privacidade

- A chave Gemini nao e hardcoded no codigo-fonte.
- O app usa fallback local quando Gemini esta indisponivel.
- Dados de sessao sao persistidos localmente via DataStore.
- Contatos e preferencias sensiveis possuem suporte a `EncryptedSharedPreferences`.
- Chamadas de crise nao sao enviadas a analytics.
