# RemomLauncher (REMONLAUNCHER JAVA)

**App Name:** RemomLauncher  
**Platform:** Android (Mobile Minecraft: Java Edition Launcher)  
**Theme:** Blue horizontal modern UI  
**License:** LGPL-3.0 (planned, matching upstreams)

> A feature-rich Minecraft: Java Edition launcher for Android, designed with a clean horizontal layout, deep integration with Modrinth & CurseForge, profile-based mod management, custom controls, Microsoft + Offline accounts, and performance-focused runtime support.

Inspired by **PojavLauncher / Amethyst** (core runtime) and **Battly Launcher** (UX & management features).

---

## Vision & Goals

- Smooth Java Edition experience on modern Android devices
- Horizontal-first UI (landscape optimized, with portrait support)
- Direct download of **Mods / Resource Packs / Shaders / Texture Packs** from **Modrinth API** and **CurseForge API** straight into the correct instance folders
- Profile system where each profile shows its own mods list with Delete / Move options
- Skin management similar to Battly
- Pre-loaded, fully editable on-screen controls + import/export + add custom buttons
- Offline account + Microsoft account login
- RAM customization, Java runtime selection, renderer options, and all major Battly-style settings
- Support for almost all Java versions (Vanilla) + Fabric / Forge / NeoForge / OptiFine / Quilt
- Optimized engine layer (based on proven mobile OpenGL translation + OpenJDK ports)
- Server hosting / local multiplayer helpers (Battly Worlds style exploration planned)
- Zero unnecessary friction – downloads go directly where they belong

**Important Reality Check:**  
A full custom Java + OpenGL engine from absolute zero is a multi-year team effort. RemomLauncher will start by leveraging the mature, battle-tested runtime stack from the open-source community (Pojav/Amethyst/Mojo lineage + MobileGL / GL4ES / ANGLE / Mesa options) and then layer a completely new, blue-themed horizontal UI + modern content management on top. This is the only practical path to a stable, playable launcher.

---

## Planned Feature List

### Core
- [ ] Horizontal blue-themed Material 3 / custom UI
- [ ] Profile creation for all major Minecraft versions
- [ ] Automatic download of game jars, assets, libraries
- [ ] Microsoft OAuth login + Offline accounts
- [ ] Skin selector & uploader (Battly-style)
- [ ] Pre-configured + fully editable control layouts
- [ ] Control import / export / add button
- [ ] RAM slider + advanced JVM arguments
- [ ] Renderer selection (MobileGL, GL4ES, ANGLE, etc.)
- [ ] Java runtime management (bundled + custom)

### Content Management
- [ ] Modrinth API search + one-click install (mods, resource packs, shaders, datapacks)
- [ ] CurseForge API search + one-click install (requires user API key or proxy)
- [ ] Per-profile mods list with Delete / Move / Enable-Disable
- [ ] Resource packs / Shaders / Texture packs management
- [ ] MRPACK / CurseForge ZIP import

### Gameplay & Extra
- [ ] Smooth loading screen
- [ ] Server list + quick join
- [ ] Local server hosting helpers / world sharing (inspired by Battly Worlds)
- [ ] Controller support (Xbox, PlayStation, Switch style mapping)
- [ ] Performance monitoring (FPS, memory)
- [ ] Custom backgrounds / themes

### Engine / Runtime
- [ ] Optimized OpenJDK mobile ports
- [ ] Support for Fabric, Forge, NeoForge, OptiFine, Quilt, Vanilla
- [ ] Modern LWJGL + input pipeline
- [ ] TouchController / Controlify friendly

---

## Architecture (Planned)

```
RemomLauncher/
├── app/                          # Android application module
│   ├── ui/                       # Horizontal Compose / View-based blue UI
│   ├── accounts/                 # Microsoft + Offline
│   ├── profiles/                 # Instance / profile manager
│   ├── content/                  # Modrinth + CurseForge clients
│   ├── controls/                 # On-screen control editor
│   └── settings/                 # RAM, JVM, renderer, etc.
├── runtime/                      # Adapted from Amethyst / Pojav lineage
│   ├── jre/                      # OpenJDK mobile builds
│   ├── gl/                       # MobileGL / GL4ES / ANGLE wrappers
│   └── launch/                   # Game process launcher
├── docs/
└── README.md
```

---

## Status

**Current Stage:** Project scaffolding & roadmap (October 2026)

This repository was initialized to host the full development of RemomLauncher.  
Next steps:
1. Set up Android project structure with Compose horizontal layout
2. Integrate a stable runtime backend (Amethyst / Mojo / FoldCraft style)
3. Implement Modrinth client first (easier, no key required for public endpoints)
4. Build profile + mods management UI
5. Iterate on controls and polish

---

## Credits & Upstream

This project stands on the shoulders of giants:

- **PojavLauncherTeam / AngelAuraMC (Amethyst)** – Core mobile Java runtime & OpenGL translation foundation
- **Boardwalk** – Original concept
- **Battly Launcher** – UX inspiration for skins, workspace, and management features
- **Modrinth** – Excellent public API for content
- **CurseForge** – Content ecosystem (API key required)
- OpenJDK, LWJGL, SDL, Mesa, ANGLE, MobileGL, GL4ES and all related mobile graphics projects

---

## Contributing

The project is young. Issues, feature requests, UI mockups, and PRs are welcome once the basic structure is in place.

---

## Disclaimer

RemomLauncher is **not** affiliated with Mojang Studios, Microsoft, CurseForge, or Modrinth.  
Minecraft is a trademark of Mojang Studios.  
Use at your own risk. Always respect the licenses of the content you download.

---

**Made for the community by Remon Francis**  
GitHub: [anjalifredy-ai/REMONLAUNCHER-JAVA](https://github.com/anjalifredy-ai/REMONLAUNCHER-JAVA)
