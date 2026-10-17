# JS / TS Node IDE — Android MVP

Android-only Kotlin + Jetpack Compose starter for a phone-first JavaScript/TypeScript development environment.

## What works in this MVP

- Dark mobile IDE layout with tabs and an expandable project explorer.
- Creates a starter project in the app's private files directory (`files/projects/JSProject`).
- Opens, edits, and saves local project files; switching tabs saves the previous file.
- Creates new files.
- Imports a text/code file using Android's document picker and exports the current file using Android's save dialog.
- A real `/system/bin/sh` process running in the project directory, with command input and output.
- Basic visual syntax colouring for common JavaScript/TypeScript tokens.
- GitHub Actions workflow to build a debug APK in the cloud.

## Important limitations — this is not yet the final IDE

- The original CodeAssist editor engine from `2legram.zip` is **not integrated yet**. The current editor is a functional Compose text editor with lightweight syntax colouring, not the original canvas-based editor.
- Android's shell is not Node.js. The app does **not** bundle Node.js/npm yet; `node` and `npm` commands will fail until a compatible runtime is added.
- No TypeScript Language Server/LSP is connected yet, so package-aware completion, diagnostics, go-to-definition, refactoring, and formatting are not available yet.
- Files are stored in app-private storage. Import/export currently works per file; project-folder import/export is still to be added.
- ARMv7/armeabi-v7a device compatibility has not been verified by a physical-device test. The UI itself has no ABI-specific native dependency, but the future Node runtime will.
- The APK has not been built in this environment; use the cloud workflow below to get a build result.

## Build a debug APK with GitHub Actions

1. Create a GitHub repository and upload the contents of this folder to its root.
2. Open the repository's **Actions** tab and enable workflows if GitHub asks.
3. Run **Android debug APK** (or push to `main`).
4. Open the successful workflow run and download the `js-ts-node-ide-debug-apk` artifact.
5. Extract the artifact ZIP and install the APK on your phone. Android may ask you to allow installation from that source.

The workflow uses JDK 17 and Gradle 8.13. The project uses Android Gradle Plugin 8.13.2, Kotlin 2.2.21, and compile SDK 36.

## Current project structure

- `app/src/main/java/com/pyrzz/jside/MainActivity.kt` — current UI, file operations, syntax colouring, and shell bridge.
- `app/src/main/AndroidManifest.xml` — Android application entry point.
- `.github/workflows/android-build.yml` — cloud debug APK build.
- `docs/ORIGINAL-PROJECT-ANALYSIS.md` — findings from the supplied editor modules and integration blockers.
- `docs/ROADMAP.md` — staged plan and acceptance criteria.

## Next milestones

1. Get a successful APK build and verify installation on the user's phone.
2. Integrate the original editor while excluding `BlockEditor.kt` and block/Sketchware-specific systems.
3. Implement whole-project import/export and persistent project selection.
4. Add a verified Node.js runtime for Android ARMv7, plus npm. Do not assume a runtime works until its ABI and Android requirements are checked.
5. Connect TypeScript Language Server through a process bridge, then implement completion, diagnostics, navigation, formatting, and package-aware support.
6. Add settings, keyboard shortcuts, themes, diagnostics/problems panel, and tests.
