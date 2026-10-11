# RemomLauncher (REMONLAUNCHER JAVA)

**App Name:** RemomLauncher  
**Platform:** Android (Mobile Minecraft: Java Edition Launcher)  
**Theme:** Blue horizontal modern UI  
**License:** LGPL-3.0

> A feature-rich Minecraft: Java Edition launcher for Android, designed with a clean horizontal layout, deep integration with Modrinth & CurseForge, profile-based mod management, custom controls, Microsoft + Offline accounts, and performance-focused runtime support.

Inspired by **PojavLauncher / Amethyst** (core runtime) and **Battly Launcher** (UX & management features).

---

## Current Status (11 Oct 2026)

**Phase 0 – Scaffold COMPLETE**

- Full Android project (Gradle + Kotlin + Jetpack Compose)
- **Horizontal NavigationRail** layout (landscape-first, blue dark theme)
- Screens: Home • Profiles • Mods • Accounts • Settings
- Modrinth API client (Retrofit + models) ready
- RAM slider, Java runtime, Renderer, JVM args in Settings
- Offline account create UI
- Placeholder profiles & installed mods list

See [docs/ROADMAP.md](docs/ROADMAP.md) for the full phased plan.

**Next:** Local storage for profiles + real Modrinth search & download into profile folders.

---

## Vision & Goals

- Smooth Java Edition experience on modern Android devices
- Horizontal-first UI (landscape optimized)
- Direct download of **Mods / Resource Packs / Shaders / Texture Packs** from **Modrinth API** and **CurseForge API** straight into the correct instance folders
- Profile system where each profile shows its own mods list with Delete / Move options
- Skin management similar to Battly
- Pre-loaded, fully editable on-screen controls + import/export + add custom buttons
- Offline account + Microsoft account login
- RAM customization, Java runtime selection, renderer options, and all major Battly-style settings
- Support for almost all Java versions (Vanilla) + Fabric / Forge / NeoForge / OptiFine / Quilt
- Optimized engine layer (based on proven mobile OpenGL translation + OpenJDK ports)
- Server hosting / local multiplayer helpers

**Important Reality Check:**  
A full custom Java + OpenGL engine from absolute zero is a multi-year team effort. RemomLauncher starts by leveraging the mature runtime stack from the open-source community (Pojav/Amethyst/Mojo lineage + MobileGL / GL4ES / ANGLE) and then layers a completely new blue-themed horizontal UI + modern content management on top.

---

## How to Build (once Android Studio is set up)

```bash
git clone https://github.com/anjalifredy-ai/REMONLAUNCHER-JAVA.git
cd REMONLAUNCHER-JAVA
# Open in Android Studio → Sync Gradle → Run on device/emulator (landscape preferred)
```

Note: Launcher icons are still placeholder. Add `mipmap` resources before release.

---

## Project Structure

```
app/
├── src/main/java/com/remom/launcher/
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── theme/          # Blue dark theme
│   │   ├── navigation/     # Horizontal NavigationRail
│   │   └── screens/        # Home, Profiles, Mods, Accounts, Settings
│   └── data/
│       ├── NetworkModule.kt
│       └── modrinth/       # ModrinthApi + models
docs/ROADMAP.md
```

---

## Credits & Upstream

- **PojavLauncherTeam / AngelAuraMC (Amethyst)** – Core mobile Java runtime foundation
- **Battly Launcher** – UX inspiration
- **Modrinth** – Public API for content
- OpenJDK, LWJGL, MobileGL, GL4ES, ANGLE and related projects

---

## Disclaimer

RemomLauncher is **not** affiliated with Mojang Studios, Microsoft, CurseForge, or Modrinth.  
Minecraft is a trademark of Mojang Studios. Use at your own risk.

---

**Made for the community by Remon Francis**  
GitHub: [anjalifredy-ai/REMONLAUNCHER-JAVA](https://github.com/anjalifredy-ai/REMONLAUNCHER-JAVA)
