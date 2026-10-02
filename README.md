# autoTranslation

Android Studio / IntelliJ IDEA plugin for automatically translating Android `strings.xml` resources.

## V2 highlights

- Detects target locales dynamically from `res/values-*` folders.
- No hard-coded language list.
- Supports locales such as `values-vi`, `values-th`, `values-pt-rBR`, and BCP-47 folders such as `values-b+zh+Hans`.
- Translates only missing strings or refreshes all strings.
- Skips `translatable="false"`.
- Protects Android placeholders such as `%s`, `%d`, `%1$s`, `%1$d`, and `%%`.
- Keeps XML/Android escaping valid after translation.

## Usage

1. Open an Android project.
2. Add locale folders under `res/`, for example `values-vi`, `values-fr`, or `values-th`.
3. Right-click the base `res/values/strings.xml`.
4. Choose **Auto Translate Strings**.
5. Choose whether to translate only missing keys or refresh all strings.

## Build

Use a local Gradle installation or import the project into Android Studio / IntelliJ IDEA:

```bash
gradle build
```

## Tech

- Kotlin
- IntelliJ Platform SDK
- Google Translate endpoint behind a pluggable `TranslationProvider`
