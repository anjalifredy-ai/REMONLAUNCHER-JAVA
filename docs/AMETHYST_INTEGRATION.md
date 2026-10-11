# Amethyst Java Runtime → Remom playable

## Reality check (important)

**AngelAuraMC/Amethyst-Android** is the playable engine:
- OpenJDK 8 / 17 / 21 (ARM)
- MobileGlues / GL4ES / ANGLE / Zink
- LWJGL natives, OpenAL, SDL, controls
- Forge headless installer, Fabric, worlds, multiplayer servers

Size: **~7,500+ commits**, native AARs (LWJGL ~16MB, ANGLE ~9MB, MobileGlues ~8MB, …), full `app_pojavlauncher` module.

You **cannot** paste that into Remom file-by-file in chat and get a working world tomorrow.
You **can** make Minecraft playable by **merging projects** on a PC with Android Studio.

---

## Fastest path to PLAYABLE (recommended)

### Option A — Fork Amethyst, put Remom UI on top (fastest playable)

1. PC pe:
```bash
git clone --recursive https://github.com/AngelAuraMC/Amethyst-Android.git
cd Amethyst-Android
```
2. Build once (needs network + JDK):
```bash
./gradlew :app_pojavlauncher:assembleDebug
```
3. Install APK → **world + servers already work** (Amethyst engine).
4. Then replace Amethyst UI screens with Remom Compose (profiles, Modrinth, blue theme) **without** removing `JREUtils`, `MainActivity` game surface, natives.

This is how almost every “custom launcher” gets playable: **engine first, skin later**.

### Option B — Remom app + Amethyst as library module (harder)

1. Add Amethyst as git submodule:
```bash
git submodule add -b v3_openjdk https://github.com/AngelAuraMC/Amethyst-Android.git third_party/amethyst
```
2. In `settings.gradle.kts` include `:app_pojavlauncher` (and its native deps).
3. Copy/adapt launch path:
   - Amethyst: `JREUtils.launchJavaVM(activity, runtime, …)`
   - Remom: `AmethystRuntimeBridge.startGame()` → same call with Remom `LaunchConfig` paths
4. Align directories:
   - Amethyst game dir ↔ Remom `files/remom/instances/<profile>`
   - versions/libraries/assets shared or symlinked

LGPL-3.0: keep Amethyst attribution + allow relinking.

---

## What Remom already gives the engine

| Remom piece | Amethyst needs |
|-------------|----------------|
| Official client JAR + libs + assets | Yes |
| Profile gameDir + mods/ | Yes |
| Fabric profile JSON | Yes |
| RAM / JVM args | Passed into JREUtils |
| GameActivity Surface | Amethyst uses own GL surface — prefer their MainActivity/game activity |

---

## Critical Amethyst pieces (do not rewrite)

| Module / lib | Role |
|--------------|------|
| `app_pojavlauncher` | UI + launch + controls |
| `jre_lwjgl3glfw` | LWJGL/GLFW Android |
| `libs/MobileGlues-release.aar` | Modern GL path |
| `libs/lwjgl-*-natives-release.aar` | Natives |
| `libs/angle-release.aar` | ANGLE renderer |
| `libs/openal-soft-release.aar` | Sound |
| `forge_installer` component | Forge install |
| OpenJDK multiarch builds | Actual JVM |

Launch core (Pojav lineage):
`JREUtils.launchJavaVM(...)` after classpath + args built.

---

## Optimization tips (once engine runs)

1. **Renderer:** MobileGlues or Zink on Vulkan devices; GL4ES on older.
2. **RAM:** 1–2 GB on mid phones; avoid 4GB+ on 4GB devices.
3. **JVM:** `-XX:+UseG1GC -XX:MaxGCPauseMillis=50` (tune per Java version).
4. **Mods:** fewer heavy client mods; Sodium/Iris via Fabric when supported.
5. **Servers:** same as PC — join IP in multiplayer; no extra “server hosting” inside phone unless you run a separate server jar (heavy).

---

## Remom code hooks

- `RuntimeBridge` / `AmethystRuntimeBridge` — swap implementation when module linked
- `LaunchConfig` — already has classpath, jvmArgs, gameArgs, gameDir
- `GameInstaller` — keep feeding official jars into shared version folder

Until submodule is built on a machine, PLAY opens `GameActivity` (window only).

---

## Bottom line

| Goal | How |
|------|-----|
| Play world + servers **this week** | Install/build **Amethyst APK**, use it |
| Remom branding + Modrinth UI + playable | Fork Amethyst → graft Remom UI |
| Pure Remom APK with engine inside | Submodule + weeks of Gradle/native merge |

There is no smaller “custom engine” that matches Amethyst for Forge/Fabric/1.21/worlds.
