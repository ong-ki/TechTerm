<div align="center">
  <img src="docs/icon.png" width="76" height="76" alt="TechTerms App Icon" />
  <h1>TechTerms</h1>
  <p>Offline English–Burmese Tech Dictionary for Android · မြန်မာနည်းပညာအဘိဓာန်</p>
  <p>
    <a href="https://github.com/ong-ki/TechTerm/releases/latest">
      <img src="https://img.shields.io/badge/Download-APK%20v1.0.1-3F51B5?logo=android&logoColor=white" alt="Download APK v1.0.1" />
    </a>
    <img src="https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white" alt="Android API 26+" />
    <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
    <a href="LICENSE">
      <img src="https://img.shields.io/badge/License-MIT-green" alt="License MIT" />
    </a>
  </p>
</div>

An offline Burmese tech dictionary and Android learning app built with Kotlin and Jetpack Compose. Browse 249 Myanmar programming and computer science terms across 15 categories with bilingual explanations, syntax-highlighted code snippets, and daily reminders.

## Screenshots

| Home | Browse | Term Detail | Settings |
| :---: | :---: | :---: | :---: |
| ![Home screen with Lessons wave graph and Term of the Day](docs/screenshots/home.png) | ![Browse screen with search and category filters](docs/screenshots/browse.png) | ![Term detail bottom sheet with Burmese meaning and Kotlin snippet](docs/screenshots/term_detail.png) | ![Settings screen with themes, notifications, and About](docs/screenshots/settings.png) |

## Features

- **100% Offline** — 249 terms across 15 CS & engineering categories bundled in Room SQLite.
- **Bilingual (မြန်မာ + English)** — Clear definitions, real-world examples, and developer usage notes with `Noto Sans Myanmar`.
- **Syntax-Highlighted Snippets** — Practical Kotlin, Bash, SQL, HTTP, Git, and Docker examples with one-tap copy.
- **Interactive Wave Graph** — Visualizes term counts per category on Home with quick-jump filtering.
- **Instant Search** — Filter by category, difficulty, **Has code**, or keyword across terms, meanings, and snippets.
- **Daily Reminders & Themes** — Scheduled WorkManager notifications plus System, Light, Paper, and Dark modes.

## Download

Grab the signed [`app-release.apk`](https://github.com/ong-ki/TechTerm/releases/latest) (Android 8.0 / API 26+) from the latest GitHub release.

## Tech Stack & Build

Built with **Kotlin 2.0**, **Jetpack Compose (Material 3)**, **Room**, **DataStore**, and **WorkManager** (`minSdk 26`, `targetSdk 35`).

```bash
git clone https://github.com/ong-ki/TechTerm.git && cd TechTerm
./gradlew installDebug
```

<details>
<summary>Project Structure</summary>

```text
app/src/main/
├── assets/terms.json                 # Bundled bilingual dataset (249 terms)
└── java/com/techterms/app/
    ├── data/
    │   ├── datastore/                # Notification & theme preferences
    │   ├── db/                       # Room entity, DAO, migrations, database
    │   └── repository/               # TermRepository (seeding & queries)
    ├── notification/                 # DailyTermWorker, BootReceiver, scheduler
    └── ui/
        ├── browse/                   # Search, category chips, term list
        ├── detail/                   # TermDetailBottomSheet & SyntaxHighlighter
        ├── home/                     # HomeScreen, WaveGraph, ThemeModeSelector
        ├── navigation/               # TechTermsNavGraph
        ├── settings/                 # SettingsScreen (Theme, Notification, About)
        └── theme/                    # Material 3 themes, PaperGrain, typography
```

</details>

## Contributing & License

Contributions to expand Myanmar IT terminology in `app/src/main/assets/terms.json` are welcome via pull request. Licensed under the [MIT License](LICENSE).
