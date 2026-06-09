# Guia reproduzivel para gerar APK Android nativo com GitHub Actions

Este documento descreve, de forma reproduzivel, como o APK do SERENARE foi gerado e como aplicar o mesmo processo a outros projetos Android nativos.

## 1. Resultado obtido no SERENARE

O APK foi gerado em ambiente de CI, usando GitHub Actions, porque a maquina local nao possuia JDK, Gradle nem Android SDK configurados.

Dados finais:

- Repositorio: `https://github.com/onecio/serenare-android`
- Workflow: `.github/workflows/android-release.yml`
- Execucao bem-sucedida: `https://github.com/onecio/serenare-android/actions/runs/27180383704`
- Release: `https://github.com/onecio/serenare-android/releases/tag/v0.1.0-debug`
- APK: `https://github.com/onecio/serenare-android/releases/download/v0.1.0-debug/serenare-debug.apk`
- Arquivo local: `dist/serenare-debug.apk`
- SHA-256: `3CDEF05E9C05014A8750C0DCBFDE5B5338ED9EE9439C4F70F405FDDBACD28851`

## 2. Pre-requisitos

Para reproduzir este processo, e necessario:

- Conta GitHub autenticada no `gh`.
- Git instalado.
- Projeto Android nativo com Gradle.
- Workflow GitHub Actions para compilar o APK.
- Opcionalmente, secrets configurados no repositorio, como `GEMINI_API_KEY`.

Validacao da autenticacao:

```powershell
gh auth status
```

Se o projeto ainda nao for um repositorio Git:

```powershell
git init -b main
```

## 3. Estrutura minima de um projeto Android nativo

Um projeto Android nativo deve conter, no minimo:

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/com/exemplo/app/MainActivity.kt
    res/values/
```

Para Jetpack Compose com Kotlin, a base recomendada inclui:

- `com.android.application`
- `org.jetbrains.kotlin.android`
- `org.jetbrains.kotlin.plugin.compose`
- `androidx.activity:activity-compose`
- Compose BOM
- `androidx.compose.material3:material3`

## 4. Configuracao essencial do Gradle

O arquivo `gradle.properties` deve habilitar AndroidX:

```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
kotlin.code.style=official
```

Sem `android.useAndroidX=true`, builds com dependencias AndroidX falham no GitHub Actions.

## 5. Protecao de segredos e artefatos

Crie `.gitignore` com, no minimo:

```gitignore
.gradle/
.idea/
build/
**/build/
local.properties
*.jks
*.keystore
*.apk
*.aab
captures/
```

Nunca versionar:

- `local.properties` com chaves reais.
- Keystores.
- APKs gerados localmente, salvo quando houver motivo explicito para distribuicao.

## 6. Workflow GitHub Actions usado

Arquivo:

```text
.github/workflows/android-release.yml
```

Conteudo base:

```yaml
name: Android Release Build

on:
  workflow_dispatch:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.11.1

      - name: Create local properties
        run: |
          echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" > local.properties

      - name: Build debug APK
        run: gradle :app:assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

Para projetos sem chave externa, a etapa `Create local properties` pode ser removida ou ajustada.

## 7. Publicacao inicial no GitHub

No diretorio do projeto:

```powershell
git status --short
git add .
git commit -m "Initial Android app"
gh repo create onecio/nome-do-projeto --public --source . --remote origin --push
```

Se o repositorio remoto ja existir:

```powershell
git remote add origin https://github.com/onecio/nome-do-projeto.git
git push -u origin main
```

## 8. Acompanhar a build

Listar execucoes recentes:

```powershell
gh run list --repo onecio/nome-do-projeto --limit 5
```

Acompanhar uma execucao:

```powershell
gh run watch ID_DA_EXECUCAO --repo onecio/nome-do-projeto --exit-status
```

Se falhar:

```powershell
gh run view ID_DA_EXECUCAO --repo onecio/nome-do-projeto --log-failed
```

Corrigir o erro, criar novo commit e enviar:

```powershell
git add .
git commit -m "Fix Android CI build"
git push
```

## 9. Baixar o APK gerado

Depois da build bem-sucedida:

```powershell
New-Item -ItemType Directory -Force -Path .\dist | Out-Null
gh run download ID_DA_EXECUCAO --repo onecio/nome-do-projeto --name app-debug-apk --dir .\dist
```

Renomear o APK:

```powershell
Move-Item -LiteralPath .\dist\app-debug.apk -Destination .\dist\nome-do-projeto-debug.apk -Force
```

Gerar hash SHA-256:

```powershell
Get-FileHash -Algorithm SHA256 .\dist\nome-do-projeto-debug.apk
```

## 10. Verificacao basica do APK

Um APK e um arquivo ZIP. Para confirmar estrutura minima:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$path = (Resolve-Path .\dist\nome-do-projeto-debug.apk).Path
$zip = [System.IO.Compression.ZipFile]::OpenRead($path)
$zip.Entries | Where-Object { $_.FullName -in @('AndroidManifest.xml','classes.dex','resources.arsc') } | Select-Object FullName,Length
$zip.Dispose()
```

A presenca de `AndroidManifest.xml`, `classes.dex` e `resources.arsc` confirma que o pacote possui a estrutura Android esperada.

## 11. Criar uma Release com o APK

```powershell
gh release create v0.1.0-debug .\dist\nome-do-projeto-debug.apk `
  --repo onecio/nome-do-projeto `
  --title "Nome do Projeto Android APK v0.1.0-debug" `
  --notes "APK debug gerado pelo GitHub Actions. SHA-256: HASH_AQUI."
```

Consultar a Release:

```powershell
gh release view v0.1.0-debug --repo onecio/nome-do-projeto --json url,assets
```

O campo `assets.url` fornece o link direto para download do APK.

## 12. Instalar no smartphone Android

1. Abrir o link direto do APK no navegador do smartphone.
2. Baixar o arquivo `.apk`.
3. Autorizar instalacao de apps desconhecidos para o navegador ou gerenciador de arquivos.
4. Abrir o APK.
5. Confirmar `Instalar`.

Para APK debug, o Android pode exibir alertas. Isso e esperado fora da Play Store.

## 13. Erros comuns e correcoes

### Falha: AndroidX nao habilitado

Erro:

```text
android.useAndroidX property is not enabled
```

Correcao:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

### Falha: arquivo invalido em `res/raw`

Erro:

```text
File-based resource names must contain only lowercase a-z, 0-9, or underscore
```

Correcao:

- Remover arquivos como `README.md` de `res/raw`.
- Usar apenas nomes como `audio_intro.ogg`.
- Manter documentacao fora de `res/`, por exemplo em `docs/`.

### Falha: APK nao encontrado no download

Verificar o nome real do artefato:

```powershell
gh run view ID_DA_EXECUCAO --repo onecio/nome-do-projeto
```

Depois baixar com o nome exato configurado em `upload-artifact`.

## 14. Fluxo completo resumido

```powershell
gh auth status
git init -b main
git add .
git commit -m "Initial Android app"
gh repo create onecio/nome-do-projeto --public --source . --remote origin --push
gh run list --repo onecio/nome-do-projeto --limit 5
gh run watch ID_DA_EXECUCAO --repo onecio/nome-do-projeto --exit-status
New-Item -ItemType Directory -Force -Path .\dist | Out-Null
gh run download ID_DA_EXECUCAO --repo onecio/nome-do-projeto --name app-debug-apk --dir .\dist
Move-Item .\dist\app-debug.apk .\dist\nome-do-projeto-debug.apk -Force
Get-FileHash -Algorithm SHA256 .\dist\nome-do-projeto-debug.apk
gh release create v0.1.0-debug .\dist\nome-do-projeto-debug.apk --repo onecio/nome-do-projeto --title "APK debug" --notes "APK gerado via GitHub Actions."
```

## 15. Para APK de producao

O fluxo acima gera APK debug, adequado para teste e instalacao direta. Para producao, adicionar:

- Keystore protegida por GitHub Secrets.
- `signingConfig` de release.
- `gradle :app:assembleRelease`.
- Preferencialmente gerar `.aab` para Play Store com `gradle :app:bundleRelease`.
- Controle de versao com `versionCode` e `versionName`.

Nunca publicar keystore, alias, senha ou arquivos sensiveis no repositorio.
