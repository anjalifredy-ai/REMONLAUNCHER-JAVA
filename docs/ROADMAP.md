# RemomLauncher Development Roadmap

## Phase 0 – Scaffold (DONE ✓)
- [x] GitHub repo created
- [x] Android project structure (Gradle + Compose)
- [x] Horizontal NavigationRail UI (landscape-first)
- [x] Blue dark theme
- [x] Home / Profiles / Mods / Accounts / Settings screens
- [x] Modrinth API interface + models + NetworkModule

## Phase 1 – Core UI & Data (MOSTLY DONE ✓)
- [x] Real local storage for profiles (JSON via ProfileRepository)
- [x] Profile creation dialog (version + loader)
- [x] Offline account persistence
- [x] Settings persistence (RAM, Java, Renderer, JVM args)
- [x] LauncherViewModel central state
- [x] Installed mods list from profile folder + delete
- [x] Working Modrinth search + download into profile/mods/
- [ ] Version list from Mojang manifest (next)
- [ ] Microsoft OAuth
- [ ] Skin preview & change

## Phase 2 – Content Management
- [x] Working Modrinth search + download to profile/mods/
- [ ] Resource packs & shaders tabs
- [ ] CurseForge support (user API key)
- [ ] Enable / Disable / Move mods
- [ ] MRPACK / CurseForge ZIP import

## Phase 3 – Runtime Integration
- [ ] Integrate Amethyst / Mojo / FoldCraft style backend
- [ ] OpenJDK mobile runtime packaging
- [ ] Renderer selection applied on launch
- [ ] Actual game launch pipeline
- [ ] Loading screen polish

## Phase 4 – Controls & Polish
- [ ] Full on-screen control editor
- [ ] Import / Export control layouts
- [ ] Pre-loaded default layouts
- [ ] Controller support mapping

## Phase 5 – Extra Features
- [ ] Server list + quick join
- [ ] Local multiplayer helpers
- [ ] Custom backgrounds
- [ ] Performance monitor

---

**Current focus:** Mojang version manifest → then runtime integration (Phase 3).
