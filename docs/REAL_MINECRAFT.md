# Real Minecraft Java on RemomLauncher

## What "real Minecraft" means

| Piece | Status in Remom |
|-------|-----------------|
| Official version list (Mojang manifest) | ✅ Working |
| Official client JAR from Mojang CDN | ✅ Working (SHA-1 verified) |
| Official libraries | ✅ Working |
| Official assets (textures, sounds, lang) | ✅ Working |
| Profile + mods folders | ✅ Working |
| Modrinth mods/RP/shaders into profile | ✅ Working |
| Launch config (classpath, args, RAM) | ✅ Working |
| **Mobile JVM that executes the client** | ❌ Needs Amethyst/Mojo-style runtime |
| **OpenGL → GLES translation** | ❌ Same |
| **Touch controls overlay** | ⏳ UI later |

When you press **PLAY**, Remom downloads **real official Minecraft Java** files to:

```
Android/data/.../files/remom/
  versions/<id>/<id>.jar      ← real client
  versions/<id>/<id>.json
  libraries/...               ← real libs
  assets/objects/...          ← real textures/sounds
  instances/<profile>/mods/
```

These are the **same files** the official PC launcher uses.

## Why the game window does not open yet

Minecraft Java is a desktop Java app. On Android it needs:

1. **OpenJDK for ARM** (not Android's ART)
2. **LWJGL** rebuilt for Android
3. **GL translation** (desktop OpenGL calls → OpenGL ES)
4. A process host that starts `java -cp ... net.minecraft.client.main.Main ...`

That stack is what **Pojav / Amethyst / Mojo / FoldCraft** spent years building.
Remom's design: finish the launcher + installer (done), then **plug in** that runtime via `RuntimeBridge`.

## Honest path to "playable"

1. ✅ Real Mojang downloads (this repo)
2. Fork or submodule **Amethyst-Android** / **MojoLauncher** runtime modules
3. Implement `RuntimeBridge.startGame()` with their JRE + renderer
4. Point their game dir at Remom's `instances/` + `versions/`
5. Ship one APK that uses Remom UI + their engine

There is **no shortcut** that invents a new JVM+GL engine in a few days and runs 1.21 smoothly.

## What you can test today

1. Create profile → pick real version from Mojang list
2. Press PLAY → watch progress (client + libs + assets download)
3. Check device storage under app files for real `.jar` and asset objects
4. Logcat: `GameInstaller`, `AssetDownloader`, `RuntimeBridge` (stub message)

## Bottom line

- **Real Minecraft Java files: YES** (official Mojang)
- **Game actually running on screen: needs runtime integration** (next major phase)
