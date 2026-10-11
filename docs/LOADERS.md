# Mod loaders — separate paths

Remom treats each loader as its own install pipeline (chhota, clear):

| Loader | How install works | Status |
|--------|-------------------|--------|
| **Vanilla** | GameInstaller only (Mojang client + libs + assets) | ✅ |
| **Fabric** | `meta.fabricmc.net` profile JSON + loader/intermediary jars | ✅ code |
| **Quilt** | `meta.quiltmc.org` (same idea as Fabric) | 🟡 meta touch |
| **Forge** | Maven installer + processors (heavy) | 🔴 needs Amethyst-style headless |
| **NeoForge** | maven.neoforged.net | 🔴 same |
| **OptiFine** | OptiFine jar / OptiFabric | 🟡 note only |

## Fabric (working installer path)

```
GET https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json
→ save as versions/{id}/{id}.json
→ download libraries from maven.fabricmc.net
```

Profile `versionId` should become the Fabric id (e.g. `1.21.3-fabric-0.16.9`).
Vanilla `inheritsFrom` version must already be installed via GameInstaller.

## Forge / NeoForge

Forge does not ship a simple profile JSON like Fabric.
It runs an installer that:
1. Downloads installer jar
2. Runs processors (deobf, patch)
3. Writes a version JSON + libraries

On Android this is usually done by **Amethyst’s headless mod installer** (already supports Forge/Fabric).
Remom’s UI can still select “Forge” on a profile; full auto-install lands when runtime is integrated.

## Game window

`GameActivity` = real Android window + `SurfaceView`.
PLAY opens this window after downloads.
Pixels of Minecraft appear only after OpenJDK + GL bind to that Surface
(Amethyst-Android: https://github.com/AngelAuraMC/Amethyst-Android).
