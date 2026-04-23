# TrailBlazer

A Minecraft plugin for **leader-gated progression** events. One player is designated the *leader* — only the leader can unlock new actions (breaking blocks, placing blocks, crafting items, interacting with entities, using portals, killing passive mobs). Once the leader performs an action for the first time, it becomes available to all other players. Non-leaders are blocked from actions that haven't been unlocked yet.

Designed for Twitch livestream events where a Twitch bot calls commands automatically in response to stream events (subs, raids, bits, etc.).

---

## How It Works

- **Leaders** perform actions first to unlock them for everyone.
- **Non-leaders** are blocked from any action that hasn't been unlocked yet, and see an action bar message explaining why.
- When a leader performs a new action, a server-wide chat message and boss bar notification announce the unlock.
- The **auto-leader** system randomly rotates the leader role on a configurable timer, with a countdown boss bar visible to all players.

---

## Commands

All commands require the `trailblazer.admin` permission (default: OP).

### `/trailblazer unlock <type> <material>`

Manually unlocks an action for all players without requiring the leader to perform it.

| Argument | Values |
|---|---|
| `type` | `break`, `place`, `craft` |
| `material` | Any valid Minecraft material name (e.g. `oak_log`, `diamond`) |

**Example:** `/trailblazer unlock break oak_log`

---

### `/trailblazer stats`

Displays current unlock progress — how many blocks/items have been unlocked for each action type (break, place, craft, use, mob kills).

---

### `/trailblazer addleader <player> [duration] [reason...]`

Adds a player as a leader.

- **Permanent** (no duration): `/trailblazer addleader Steve`
- **Temporary** (duration in seconds): `/trailblazer addleader Steve 300 Sub gift from Viewer123`

When a temporary leader is added with a reason, a server-wide message is broadcast. The leader role is automatically removed after the duration expires.

---

### `/trailblazer removeleader <player>`

Removes a player from both temporary and permanent leader roles immediately.

---

### `/trailblazer leaders`

Lists all current permanent and temporary leaders by name.

---

### `/trailblazer autoleader start <min> <max>`

Starts the auto-leader rotation timer. A random eligible online player (Survival mode only) is selected as the leader every `min`–`max` seconds. The countdown is displayed to all players on a boss bar.

- If `min` equals `max`, the interval is fixed.
- The previous auto-leader's temporary leader status is removed when a new leader is selected.
- Players in Creative or Spectator mode are excluded from selection.
- If the timer is already running, calling `start` again immediately re-elects a new leader using the new timing bounds. Useful for Twitch bot triggers that don't track state.

**Example:** `/trailblazer autoleader start 120 300`

---

### `/trailblazer autoleader stop`

Stops the auto-leader timer, hides the boss bar, and removes the current auto-leader's temporary leader status.

---

### `/trailblazer autoleader set <player> [reason...]`

Immediately assigns a specific online player as the current auto-leader, cancelling the active countdown and starting a fresh one. Requires the auto-leader system to already be running.

- If a reason is provided it is broadcast server-wide (e.g. `won the viewer vote`, `raid reward`).
- Without a reason, uses the standard leader-chosen broadcast.
- The previous auto-leader loses their role immediately.

**Example:** `/trailblazer autoleader set Steve won the viewer poll`

---

### `/trailblazer reload`

Reloads `config.yml` and `whitelist.yml` from disk. Active temporary leaders, their expiry timers, and the current auto-leader are all preserved — only config values and the permanent leaders list are updated.

Requires `trailblazer.admin`.

---

### `/trailblazer debug [on|off]`

Toggles verbose debug logging to the server console. Omit `on`/`off` to check the current state.

Requires `trailblazer.admin`.

---

## Configuration (`config.yml`)

| Key | Default | Description |
|---|---|---|
| `select-new-leader-on-quit` | `false` | If `true`, immediately selects a new auto-leader when the current one disconnects. Recommended `true` for live events. |
| `allow-repeat-leader` | `true` | If `false`, the same player cannot be selected twice in a row by the auto-leader system. |
| `permanent-leaders` | `[]` | List of UUIDs for permanent leaders. Managed automatically by `addleader`/`removeleader`. |

All chat and boss bar messages are configurable under the `messages:` block in `config.yml`.

---

## Unlock Persistence (`whitelist.yml`)

Unlocked actions are stored in `whitelist.yml` in the plugin data folder. Writes are debounced (5 seconds) to avoid excessive disk I/O during rapid unlock events. A flush is forced on plugin disable to prevent data loss.
