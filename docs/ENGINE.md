# RemomLauncher Engine Architecture

## Goal
Run Minecraft: Java Edition inside the Android app with:
- Vanilla / Fabric / Forge / NeoForge / OptiFine / Quilt
- Smooth play, proper controls, mods from profile folders

## Two layers

### 1. Launcher-side engine (WHAT WE BUILD NOW) ✅
| Component | Role |
|-----------|------|
| `GamePaths` | Directory layout (versions, libraries, assets, instances, natives, runtime) |
| `GameInstaller` | Download version JSON + client JAR + libraries (SHA-1 verified) |
| `LaunchBuilder` | Build classpath, JVM args, game args |
| `LaunchConfig` | Full descriptor passed to runtime |
| `GameEngine` | Facade: prepare → launch |
| `RuntimeBridge` | Interface to native backend |

**Status:** Implemented and wired to PLAY button.  
Pressing PLAY downloads the version (if needed) and builds a complete launch config.  
Logs appear in Logcat under `RuntimeBridge` / `GameEngine`.

### 2. Runtime backend (JNI / native) — NEXT
To actually *run* the game we need code that already exists in open source:

| Piece | Source projects |
|-------|-----------------|
| Mobile OpenJDK (ARM64) | Amethyst / Pojav OpenJDK builds |
| OpenGL → GLES translation | MobileGL, GL4ES, ANGLE |
| LWJGL Android natives | Amethyst / Mojo |
| Touch → mouse/keyboard | Control systems in Amethyst / FoldCraft |
| Process + classpath launch | Same backends |

**Plan:** Integrate Amethyst-Android or MojoLauncher as a submodule / library, then implement `RuntimeBridge` with real `startGame()` instead of `StubRuntimeBridge`.

We do **not** rewrite the entire JVM + GL stack from zero — that is multi-year work already done by those projects.

## Launch flow

```
User taps PLAY
    → ViewModel.play(activity)
        → GameEngine.launch()
            → GameInstaller.prepare(versionId)   // if not ready
            → LaunchBuilder.build(config)
            → RuntimeBridge.startGame(config)    // stub today, real later
```

## What works today
1. Official version list from Mojang
2. Download client JAR + libraries into `filesDir/remom/`
3. Build full JVM + game argument lists
4. Profile mods / resource packs / shaders already in instance folders
5. PLAY triggers the full pipeline (ends at stub with clear log message)

## Next concrete steps
1. Add assets download (asset index + objects)
2. Submodule or dependency on Amethyst runtime pieces
3. Replace `StubRuntimeBridge` with real JNI bridge
4. Surface / GameActivity for rendering
5. Control overlay on top of game surface
