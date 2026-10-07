# Êxodus

An open-source Android file manager for local file operations, ZIP creation, and system sharing, with no advertising.

**Stack:** Kotlin, Android Views/Material components, coroutines, Java file APIs, `ZipOutputStream`, and `FileProvider`.

**Status:** version 1.0.0; [APK release](https://github.com/lughlammas/exodus/releases/tag/v1.0.0) available. No automated test suite or CI workflow is included; build and device behavior were not retested during this documentation pass.

## File workflows

- Browse local folders and show names, sizes and modification dates.
- Create folders, copy paths, rename and delete with confirmation.
- Create a ZIP from a folder and share it through Android's system chooser.
- Open files with system intents; use a dark Material interface.

Operations are local. The manifest requests storage access, including “All files access” on Android 11+, and does not request internet or location access. Available folders depend on Android storage permissions and platform restrictions.

## Install

Download `Exodus.apk` from [release v1.0.0](https://github.com/lughlammas/exodus/releases/tag/v1.0.0), open it, and allow installation from that source if Android prompts you. Grant storage access for the folders you intend to manage.

## Build

Requires JDK 17 and Android SDK 35. Minimum Android API: 26.

```bash
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. Configure the SDK path in a local, untracked `local.properties` file. Debug output: `app/build/outputs/apk/debug/app-debug.apk`.

Application ID: `com.lughlammas.exodus`.

## Authorship and license

Originally attributed to Lab lughlammas — Guilherme / Gui; maintained by Guilherme Cavalcanti within **ARBOCK LABS**, an independent software and applied-AI lab currently being structured. Historical attribution is retained in [AUTHORS](AUTHORS).

[MIT license](LICENSE) · [Portfolio](https://lughlammas.github.io).
