---
title: "Plugin Config"
sidebar_position: 5
---

The plugin-wide settings live in `config.yml` in the EcoMinions data folder (`/plugins/EcoMinions/config.yml`).
Edit it, then run `/minions reload` to apply changes.

## worlds

```yaml
worlds:
  disabled: []
```

A list of world names where minions cannot be placed or operate.

## limits

```yaml
limits:
  per-player:
    enabled: true
    default: 5
    permission-overrides: true
```

Caps how many minions a single player can have placed at once. `default` is the base limit; when
`permission-overrides` is true, per-player permission nodes can raise or lower it for individual players.

## unloaded-calculation

```yaml
unloaded-calculation:
  enabled: true
  # Multiplier applied to offline drops relative to live mining.
  # 1.0 = full parity, 0.75 = 75% of live rate while offline.
  offline-efficiency: 1.0
```

A minion is idle whenever its owner is offline or its chunk is unloaded - it does not mine live in either case.
When the owner comes back online (or the chunk reloads), the elapsed idle span is credited in one go: cycles are
simulated for the time that passed, scaled by `offline-efficiency`. Fuel is consumed proportionally to that
simulated span, not to the whole wall-clock absence. Setting `enabled` to false stops idle spans from being
credited at all - a minion simply resumes from where it left off with no catch-up.

## sell-pricing

```yaml
sell-pricing:
  # Pull sell prices from EcoShop for any material with no explicit price in a
  # minion's sell-prices section. Requires EcoShop to be installed.
  auto-from-ecoshop: true
  # Scales EcoShop-sourced prices only. Explicit sell-prices are never scaled.
  sell-price-multiplier: 1.0
```

`auto-from-ecoshop` requires EcoShop to be installed, and only applies to materials that have no explicit entry
in a minion type's own `sell-prices` section - an explicit price always wins and is never overridden. When a
material has no explicit price and `auto-from-ecoshop` is true, its sell price is pulled from EcoShop instead.
`sell-price-multiplier` scales EcoShop-sourced prices only; explicit `sell-prices` entries are never scaled by it.

## minion

```yaml
minion:
  # Hologram offset above the minion (Y axis blocks)
  hologram-offset: 1.6
  # How often (in ticks) to refresh the hologram contents
  hologram-refresh-ticks: 20
  # Glow / outline armor stand body? Future use.
  invulnerable: true

  # Block regen visual: replace block with bedrock briefly during regen?
  # If false, block disappears entirely until regen tick.
  show-regen-marker: true
  # Block to place when a minion is picked up and regen markers haven't fired yet.
  # Replaces any bedrock regen markers left behind. Use AIR to simply remove them.
  regen-marker-fallback-block: AIR

  # auto_sell upgrade interval (in ticks) when triggered automatically
  auto_sell-interval-ticks: 200
```

Controls the minion's hologram, its invulnerability, how mined block regeneration is shown, and how often the
`auto_sell` upgrade triggers.

## gui

```yaml
gui:
  main:
    title: "&8%minion_name% &8(&7Lvl %level%&8)"
    rows: 6
    # How collected item counts are shown: title | lore | both
    storage-display: lore
    # Max item types shown in the storage area; highest count first
    storage-slots: 9
  upgrade:
    title: "&8Upgrade: &f%upgrade_name%"
    rows: 6
  type-select:
    title: "&8Choose a Minion"
    rows: 6
  upgrade-list:
    title: "&8Upgrades: &f%minion_name%"
    rows: 6
  cost-select:
    title: "&8Choose Payment"
    rows: 3
```

Titles and row counts for every GUI: the minion's main storage/control menu, the per-upgrade purchase menu, the
type browser opened by `/minions`, the list of a minion's available upgrades, and the payment-method chooser.

## storage

```yaml
storage:
  # Storage backend: pdc-only or sqlite
  # PDC-only relies entirely on per-item PDC + per-entity PDC.
  # SQLite stores a registry of placed minion UUIDs and their owners
  # to allow cross-server-restart limit enforcement and admin queries.
  backend: pdc-only
```

`pdc-only` keeps all minion state in persistent data on the item/entity themselves. `sqlite` additionally
registers placed minion UUIDs and their owners in a database, which lets per-player limits and admin queries
survive a server restart even before every minion's chunk has loaded.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) covers the per-type config files.
- **Commands:** [Commands and Permissions](commands-and-permissions) for the reload and admin commands.
