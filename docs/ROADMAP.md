# RemomLauncher Development Roadmap

## Phase 0 – Scaffold (DONE ✓)
- [x] GitHub repo created
- [x] Android project structure (Gradle + Compose)
- [x] Horizontal NavigationRail UI (landscape-first)
- [x] Blue dark theme
- [x] Home / Profiles / Mods / Accounts / Settings screens
- [x] Modrinth API interface + models + NetworkModule
- [x] Placeholder data for profiles, accounts, mods

## Phase 1 – Core UI & Data (Next)
- [ ] Real local storage for profiles (JSON / Room)
- [ ] Profile creation dialog (version picker + loader)
- [ ] Version list from Mojang manifest
- [ ] Offline account persistence
- [ ] Microsoft OAuth (MSAL or custom flow)
- [ ] Skin preview & change (basic)

## Phase 2 – Content Management
- [ ] Working Modrinth search + version selection + download to profile/mods/
- [ ] Resource packs & shaders tabs
- [ ] CurseForge support (user API key)
- [ ] Enable / Disable / Delete / Move mods per profile
- [ ] MRPACK / CurseForge ZIP import

## Phase 3 – Runtime Integration
- [ ] Integrate Amethyst / Mojo / FoldCraft style backend
- [ ] OpenJDK mobile runtime packaging
- [ ] Renderer selection (MobileGL / GL4ES / ANGLE)
- [ ] Actual game launch pipeline
- [ ] Loading screen polish

## Phase 4 – Controls & Polish
- [ ] Full on-screen control editor (add / move / resize / opacity)
- [ ] Import / Export control layouts
- [ ] Pre-loaded default layouts
- [ ] Controller support mapping
- [ ] RAM & JVM args applied on launch

## Phase 5 – Extra Features
- [ ] Server list + quick join
- [ ] Local multiplayer / world sharing helpers
- [ ] Custom backgrounds
- [ ] Performance monitor overlay
- [ ] Auto-update system

---

**Priority right now:** Phase 1 storage + Phase 2 Modrinth download working end-to-end.
