# rBerry

**rBerry** (shown as *RetroBerry* on the loading screen) is an Android app by
**Noven** that plays classic keypad-phone Java (J2ME) games inside a retro
handheld-style interface.

- Original games built into the app (starting with Snake)
- A **+** card to import your own `.jar` / `.jad` games from your device
- Powered by the open-source [J2ME Loader](https://github.com/nikita36078/J2ME-Loader) engine

> rBerry does not include or distribute commercial games. Only import games you
> own or have the right to use.

## Status

Early development. See the latest debug build under
[Releases](https://github.com/tontonvs/rBerry/releases).

## Building

Builds run in GitHub Actions (see `.github/workflows/build.yml`) and publish a
debug APK to the `latest-build` pre-release on every push to `master`.
Local builds need JDK 21, the Android SDK and NDK, and the submodules
(`git submodule update --init`).

## License and credits

Licensed under the Apache License, Version 2.0 (see `LICENSE`).
Based on J2ME Loader, Copyright 2017-2026 Nikita Shakarun. See `NOTICE` and
the in-app **About > Licenses** screen for all third-party credits.
