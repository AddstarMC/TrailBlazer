# AGENTS.md — TrailBlazer

## What is this?

TrailBlazer is a **Paper/Minecraft plugin** (Java 21, Gradle 8.8) for a "leader-unlocks-first" gameplay mechanic. A designated leader must perform an action (break, place, craft, interact, kill) before other survival-mode players are allowed to. Unlocked actions are tracked in a persistent whitelist. An optional auto-leader rotation system picks random online players on a timer and displays progress via a boss bar.

## Codebase map

```
build.gradle                       # Java 21, Paper API 1.21.4, Adventure/MiniMessage deps
settings.gradle                    # rootProject.name = 'TrailBlazer'
gradle/wrapper/                    # Gradle 8.8 wrapper

src/main/resources/
  plugin.yml                       # Bukkit plugin descriptor (main class, commands, perms)
  config.yml                       # Default config: messages (MiniMessage), leader settings

src/main/java/au/com/addstar/trailblazer/
  TrailblazerPlugin.java           # ★ ENTRY POINT — onEnable/onDisable, registers listeners & command
  WhitelistManager.java            # Persists allowed break/place/craft/use/mob sets to whitelist.yml
  LeaderManager.java               # Permanent + temporary + auto leader state; random selection logic

  commands/
    TrailblazerCommand.java        # /trailblazer command executor (unlock, stats, addleader, removeleader, autoleader, reload, debug, leaders)

  listeners/                       # One Bukkit Listener per event type
    BlockBreakListener.java
    BlockPlaceListener.java
    CraftItemListener.java
    PlayerInteractListener.java
    PassiveMobKillListener.java
    PortalUseListener.java
    PlayerQuitListener.java

  config/
    PluginConfig.java              # Reads messages from config.yml, applies MiniMessage placeholders

  ui/
    UnlockProgressBar.java         # Boss bar shown on unlock events
    AutoLeaderBar.java             # Rotating-leader countdown boss bar

  util/
    MessageUtil.java               # MiniMessage helper, enum humanizer, duration formatter
    DebugLogger.java               # Guarded debug logging (toggle via /trailblazer debug)
```

### Key wiring

- `plugin.yml` → `main: au.com.addstar.trailblazer.TrailblazerPlugin`
- All listeners are registered in `TrailblazerPlugin.onEnable()`.
- The single command `/trailblazer` is dispatched by `TrailblazerCommand` via a `switch` on the first argument.
- Runtime data file: `plugins/TrailBlazer/whitelist.yml` (created by `WhitelistManager` at server runtime, not in repo).
- Messages use **MiniMessage** format with custom `<placeholder>` tokens replaced at runtime.

### Tests

No test source set exists yet (`src/test/` is absent). Do not create tests, they are not desired for this project.

## Local norms

- **Package root**: `au.com.addstar.trailblazer` — all new classes must live under this package.
- **One listener per event**: each Bukkit event type gets its own `Listener` class in `listeners/`.
- **Listener constructor pattern**: every listener takes `TrailblazerPlugin` as the sole constructor arg and pulls managers/config from it.
- **Messaging**: use `MessageUtil.mm(...)` for ad-hoc MiniMessage components; use `PluginConfig.getMessage(...)` for config-driven messages with placeholders.
- **Debug logging**: wrap debug output in `DebugLogger.debug(context, message)` — never use raw `System.out` or `getLogger().info()` for debug tracing.
- **Config access**: read values through `plugin.getConfig()` (Bukkit API). `PluginConfig` is a thin wrapper for message resolution only.
- **Whitelist categories**: break, place, craft, use (materials + `ENTITY_` prefixed entities), mobs. Follow this naming when adding new categories.
- **Build**: `./gradlew build` produces a plugin JAR in `build/libs/`. The `processResources` task injects `version` into `plugin.yml`.
- **Java version**: source and target are Java 21. Use modern Java features (switch expressions, records, etc.) where appropriate.

## Self-correction

1. **Stale code map** — If you discover that the codebase map above no longer matches the actual file tree (new files, moved packages, removed classes), update the map in this file immediately before continuing your task.
2. **User corrections** — If the user corrects how work should be done in this repo (naming conventions, architectural patterns, workflow rules, etc.), add the correction to the "Local norms" section (or create a new clearly labeled section) so that future sessions inherit it.
