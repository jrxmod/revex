# ⚖️ REVEX

> **Response Escalation Violation EXecutor**

![Version](https://img.shields.io/badge/version-0.1.0-orange) ![Loader](https://img.shields.io/badge/loader-fabric-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-brightgreen) ![License](https://img.shields.io/badge/license-Apache--2.0-red)

Addon for [PRAXIC AntiCheat](https://github.com/jrxmod/praxic). **Requires PRAXIC to function.**

PRAXIC detects. REVEX punishes.

---

## What it does

Instead of a flat kick or ban, REVEX applies a graduated punishment ladder.
Each violation moves the player one step closer to a permanent ban.

**Default ladder:**
1. `warn` — alert the player
2. `tempban 15m` — 15 minute ban
3. `tempban 1h` — 1 hour ban
4. `tempban 1d` — 1 day ban
5. `ban` — permanent

Every step is configurable in `config/revex.json`.
Per-check ladders are supported — FlyCheck can escalate faster than SpeedCheck.

---

## Requirements

- [PRAXIC AntiCheat](https://github.com/jrxmod/praxic) `>=0.4.0` — required
- [Fabric API](https://modrinth.com/mod/fabric-api) — required
- Fabric Loader `>=0.15.0`
- Minecraft `1.21.1`

---

## Installation

1. Install PRAXIC `0.4.0+` first
2. Drop `revex-x.x.x.jar` into your server's `mods/` folder
3. Start the server — config generates at `config/revex.json`
4. `/revex reload` to apply changes without restarting

---

## Commands

| Command | Description |
|---|---|
| `/revex status` | Current config and escalation ladder |
| `/revex bans` | All active bans with remaining time |
| `/revex ban <player> <duration> [reason]` | Manual tempban (15m, 1h, 7d, perm) |
| `/revex unban <player>` | Early unban |
| `/revex reload` | Hot-reload config |

*All commands require OP level 2.*

---

## Configuration

`config/revex.json`:
```json
{
  "enabled": true,
  "defaultEscalation": ["warn", "tempban 15m", "tempban 1h", "tempban 1d", "ban"],
  "perCheckEscalation": {},
  "escalationResetTime": "24h",
  "staffAlerts": true,
  "discordWebhookEnabled": false,
  "discordWebhookUrl": "YOUR_WEBHOOK_URL_HERE"
}
```

Duration format: `15m`, `1h`, `7d`, `perm` / `permanent`

---

## Persistent Storage

| File | Contents |
|---|---|
| `config/revex-bans.json` | Active bans with expiry timestamps |
| `config/revex-escalation.json` | Per-player escalation steps |

Both survive server restarts. Expired bans are cleaned automatically.

---

## License

Licensed under the **Apache License, Version 2.0**.

Copyright 2026 jrxmod.
