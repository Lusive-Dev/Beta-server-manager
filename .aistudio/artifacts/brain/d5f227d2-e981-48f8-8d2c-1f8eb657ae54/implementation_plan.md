# MC Beta 1.7 Server Host & Shell

A native Android application that hosts and operates a genuine Minecraft Beta 1.7.3 server on-device using a process shell runner, automated OpenJDK runtime environment, interactive terminal console, auto-EULA agreer, cracked/offline mode toggle, and real-time networking telemetry.

## User Review & Critical Decisions

> [!IMPORTANT]
> Key architectural decisions confirmed through interactive clarification and user feedback:

- **Automatic `eula.txt` Agreer**: Automatically pre-generates and enforces `eula=true` in `eula.txt` prior to server boot, preventing server halts and ensuring seamless first-time startup.
- **Cracked / Offline Mode Toggle**: Dedicated, prominent switch on the dashboard and configuration screen to toggle `online-mode=false` (Cracked mode, permitting custom launchers and offline clients without Mojang session verification) or `online-mode=true`.
- **Server JAR Source**: Auto-download official Mojang Beta 1.7.3 `server.jar` archive with integrated fallback mirrors, plus a custom storage file picker (`OpenDocument`) to import modified/modded jars (e.g. CraftBukkit b1060 for Beta 1.7.3).
- **Runtime Environment**: Automated mobile OpenJDK (headless aarch64/armv7 JRE) installer extracting into the app's internal sandbox (`context.filesDir/jre`) with executable permissions, paired with a custom binary path override and an embedded pure-Kotlin Beta 1.7.3 protocol server engine to guarantee zero-failure local hosting.
- **Console & Terminal**: Real-time streaming stdout/stderr terminal with VT100 ANSI color filtering, auto-scroll toggle, command history, and quick-action buttons (`/stop`, `/list`, `/op`, `/say`, `/time set day`, `/save-all`).
- **Server Configuration & Management**: Visual `server.properties` graphical editor (port, max players, seed, pvp, gamemode, view-distance, white-list, online-mode/cracked), operator (`ops.txt`) manager, player whitelist, and one-tap WiFi LAN IP & connection string copy.

---

## 1. Overview & Core Concept

### What It Does
The application transforms an Android smartphone or tablet into a local host for Minecraft Java Edition Beta 1.7.3. It provisions a Linux process shell environment inside the app's sandboxed storage, automatically writes `eula=true` and custom `server.properties` (including Cracked/Offline mode), launches the server with allocated JVM memory parameters (`-Xms`, `-Xmx`), streams the process input/output via a live terminal, and broadcasts port 25565 across the local network so authentic Beta 1.7 clients can connect and play.

### Target Audience & Persona
- Retro Minecraft enthusiasts, beta version players, and preservationists seeking authentic Beta 1.7 nostalgia with friends on local Wi-Fi or hotspot.
- Mobile server administrators who want an untethered, portable server node on an Android device without requiring an always-on desktop.

### Key Value
- **Zero Mockups / Genuine Operability**: Actually starts a listening socket on port 25565, accepts connections, and pipes input/output to the genuine server engine.
- **Automated Bootstrap & EULA**: No tedious manual terminal commands; one tap downloads the server files, writes `eula=true`, configures parameters, and sets up execution rights.
- **Cracked / Offline Mode Ready**: Crucial for Beta 1.7, as legacy authentication servers are frequently unreachable; toggling cracked allows all legacy clients to connect seamlessly.
- **Mobile-Tailored Terminal**: Solves the awkward touch keyboard typing by providing dedicated quick-macro buttons, command history cycling, and clean monospace styling.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Setup Flow**:
   - First launch checks environment: verifies if `minecraft_server.jar` and the JRE binary exist in `files/mcserver`.
   - Displays a clean setup card with a one-tap "Download Official Beta 1.7.3 (1.4 MB)" button or "Import Custom JAR" button.
   - Quick toggles right on setup: "Auto-agree EULA" (checked by default) and "Cracked / Offline Mode" (checked by default for hassle-free connection).
   - If JRE is not yet configured, offers automated one-tap runtime extraction or direct lightweight engine activation.
2. **Dashboard & Server Control Flow**:
   - Home dashboard shows server status: **OFFLINE** (Crimson red badge), **STARTING** (Amber yellow badge), or **RUNNING** (Vibrant emerald badge).
   - Prominent quick-toggles for "Cracked (Offline Mode)" and "Auto EULA" directly visible on the server control pane.
   - Live telemetry card displays Local LAN IP (e.g., `192.168.1.45:25565`), active player count, uptime timer, and memory usage.
   - Tap **"Start Server"**: Writes `eula.txt`, updates `server.properties`, boots the process, and switches or surfaces the live terminal tab.
   - Tap **"Stop Server"**: Safely sends the `stop` command through standard input to trigger world saving and clean shutdown.
3. **Interactive Console & Shell Flow**:
   - Displays real-time terminal output with auto-scrolling log lines formatted with timestamps and log levels (`[INFO]`, `[WARNING]`, `[SEVERE]`).
   - Command input field at the bottom with keyboard submit, clear button, and up/down history recollection.
   - Quick command chips above the keyboard: `help`, `list`, `save-all`, `time day`, `weather clear`, `stop`.
4. **Configuration (`server.properties` & EULA) Flow**:
   - Visual settings screen mapping each `server.properties` key to native M3 controls:
     - `online-mode` (Cracked Mode toggle with explanatory label)
     - `server-port` (Number input, default `25565`)
     - `max-players` (Slider / number stepper, default `20`)
     - `level-seed` (Text field)
     - `pvp`, `spawn-monsters`, `spawn-animals`, `white-list` (M3 Switches)
     - `level-name` (World folder selector/name)
     - EULA status badge and "Re-apply EULA Agreement" button.
   - Save button writes atomically back to `server.properties` on disk.
5. **Player & Operator Management Flow**:
   - Visual lists for `ops.txt`, `white-list.txt`, and `banned-players.txt` with add/remove chips.

### Visual Identity & Theme
- **Aesthetic Direction**: *Retro Industrial Cyber-Terminal*. Combines authentic dark slate-gray surfaces (`#121316`, `#1A1C20`) with vibrant Minecraft emerald green accents (`#38A169`, `#48BB78`) and phosphor-amber telemetry highlights (`#ED8936`).
- **Typography**:
  - Monospace font for console logs, IP banners, and command lines.
  - Bold, clean sans-serif headline font for telemetry stats and section headers.
- **Atmosphere & Depth**: Subtle linear gradient overlays, dark tonal elevation on cards (`SurfaceContainerHigh`), and border highlights distinguishing active server states.

---

## 3. Key Product Decisions & Trade-Offs

- **Automated `eula.txt` Management**:
  - *Chosen Approach*: When launching the server or initializing the server directory, the app automatically checks for `eula.txt` and ensures `eula=true` is written with a timestamp.
  - *Why*: Prevents immediate process exits that confuse mobile users when newer or modified server jars enforce Mojang's EULA check.
- **Cracked Mode (Offline Mode) by Default for Beta 1.7**:
  - *Chosen Approach*: Set `online-mode=false` as the default recommendation, with a clear toggle switch to re-enable online mode if desired.
  - *Why*: Beta 1.7.3 authentication servers from 2011 are deprecated by Mojang, causing genuine players to experience "Failed to verify username" errors unless offline mode is enabled.
- **Dual Server Execution Architecture (Process Shell + Native Protocol Engine)**:
  - *Chosen Approach*: Implement a primary process runner executing `java -jar server.jar` via `ProcessBuilder` with full pipe redirection (`stdout`, `stderr`, `stdin`), coupled with an embedded Kotlin Beta 1.7.3 protocol fallback.
  - *Why*: Guarantees that whether running on real ARM64 hardware with a standalone JRE or inside constrained emulator environments, the user gets an immediately working server on port 25565 without cryptic crash loops.
- **Sandboxed Internal Storage**:
  - *Chosen Approach*: Store the server files inside `context.filesDir.resolve("mc_server")`.
  - *Why*: Android permissions restrict executing binaries outside the private application directory on modern Android (W^X security restrictions). Running out of `filesDir` complies with Android security policies.
- **Foreground Service with Persistent Notification**:
  - *Chosen Approach*: Run server management inside an Android `ForegroundService` with notification channel and quick "Stop" action.
  - *Why*: Prevents the Android system from killing the background server process when the user turns off the screen or switches to Minecraft on the same device.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├────────────────────────────────────────────────────────────────────────┤
│  DashboardScreen   │   ConsoleScreen    │  ConfigScreen  │ PlayersTab  │
│  - State Badges    │   - Log Stream     │  - Properties  │ - Ops / Ban │
│  - IP & Stats      │   - Command Bar    │  - EULA Agreer │ - Whitelist │
│  - Cracked Toggle  │   - Quick Macros   │  - Port & Seed │ - Add/Kick  │
│  - Start/Stop Fab  │                    │  - Save Button │             │
└─────────┬───────────────────┬────────────────────┬───────────────┬─────┘
          │                   │                    │               │
┌─────────▼───────────────────▼────────────────────▼───────────────▼─────┐
│                           ServerViewModel                              │
│  - StateFlow<ServerState> (Offline, Booting, Running, Stopping)        │
│  - StateFlow<List<LogMessage>> (Terminal log buffer)                   │
│  - StateFlow<ServerConfig> (Parsed server.properties + EULA flag)      │
│  - Network Info State (LAN WiFi IPv4, Port 25565, Connected clients)   │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
┌─────────────────────────────▼──────────────────────────────────────────┐
│                   ServerRunnerService & Core Engine                    │
│ ┌──────────────────────────────────┐ ┌───────────────────────────────┐ │
│ │       ProcessShellRunner         │ │   Beta17ProtocolEngine        │ │
│ │  - ProcessBuilder invocation     │ │   - ServerSocket(25565)       │ │
│ │  - Stdin CommandWriter           │ │   - Packet 0xFE (Ping)        │ │
│ │  - Stdout/Stderr Stream Coroutine│ │   - Packet 0x02 (Handshake)   │ │
│ └──────────────────────────────────┘ └───────────────────────────────┘ │
│ ┌──────────────────────────────────┐ ┌───────────────────────────────┐ │
│ │      ServerFileManager           │ │     LanBroadcastManager       │ │
│ │  - Auto-download b1.7.3 jar      │ │   - Local Wi-Fi IPv4 fetch    │ │
│ │  - Auto EULA writer (eula=true)  │ │   - MOTD broadcast            │ │
│ │  - server.properties parser/write│ │   - Cracked mode sync         │ │
│ └──────────────────────────────────┘ └───────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Component Breakdown
1. `ServerState.kt`: Sealed class representing `Stopped`, `Downloading(progress)`, `Starting`, `Running(uptime, players)`, `Error(message)`.
2. `ServerConfig.kt`: Data class representing `server.properties` and `eula.txt` settings:
   - `onlineMode`: Boolean (Cracked vs Premium)
   - `autoEula`: Boolean (enforces `eula=true`)
   - `serverPort`: Int (default 25565)
   - `maxPlayers`, `levelName`, `levelSeed`, `pvp`, `spawnMonsters`, `spawnAnimals`, `motd`
3. `ServerFileManager.kt`: Manages `mc_server/` directory, checks/writes `eula.txt`, parses and serializes `server.properties`, manages `ops.txt` and `white-list.txt`, and downloads/imports jar files.
4. `ProcessShellRunner.kt`: Executes the server process via Linux shell, binds stdin/stdout/stderr, writes commands, and terminates processes safely.
5. `Beta17ServerEngine.kt`: Real TCP socket server implementing Beta 1.7.3 protocol (v14) with full cracked/offline client handshake support, ping status, world chunk dispatch, and real console commands.
6. `ServerService.kt`: Android Foreground Service maintaining active socket and server process while app is in background.
7. `ConsoleView.kt`, `DashboardView.kt`, `ConfigView.kt`: High-fidelity Jetpack Compose screens with Material 3 design, monospace terminal, cracked mode badges, and quick server macros.
