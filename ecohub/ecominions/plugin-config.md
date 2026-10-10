---
title: "Plugin Config"
sidebar_position: 7
---

The plugin-wide settings live in `config.yml` in the EcoMinions data folder (`/plugins/EcoMinions/config.yml`).
Edit it, then run `/minions reload` to apply changes.

## disabled-in-worlds

```yaml
disabled-in-worlds: [] # Worlds EcoMinions is fully disabled in
```

A list of world names, case-insensitive, where minions cannot be placed, do not operate and have no effects.

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
in a minion's own `sell-prices` section - an explicit price always wins and is never overridden. When a
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

  # What happens to the blocks a minion placed (seeded ores and crops, saplings,
  # grown trees and the water it laid) when it is picked up or removed:
  #   mine  - they stay in the world for players to mine
  #   clear - they are removed, unless a player has since replaced them
  seeded-blocks-on-pickup: mine

  # Upper bound on any minion's storage-slots and max-storage-slots.
  hard-slot-cap: 54

  xp:
    # Where the XP a minion earns goes: store or orb.
    mode: store
```

`seeded-blocks-on-pickup` decides whether a self-seeding minion leaves its blocks behind. With `mine`, players
can break them by hand like any other block. With `clear`, every spot the minion placed something at goes back
to air, except spots where the block has since been replaced with something the minion would not place.

`hard-slot-cap` bounds what any minion may declare for `storage-slots` and `max-storage-slots`. Minion
storage is virtual and is never opened as a chest, so this is a guard against typos rather than a Minecraft
limit - raise it if your types need more than a double chest's worth.

`xp.mode` decides where the XP a minion earns goes: ore XP, kill XP, catch XP, and any XP from effects or
EcoItems loot. With `store`, the default, it is kept on the minion and claimed from the stored XP button in its
menu. With `orb`, it drops as an orb at the minion, as it did before stored XP existed. A collection minion
always stores the orbs it absorbs, whatever the mode.

Controls the minion's hologram, its invulnerability, and how mined block regeneration is shown. The auto-sell
interval is no longer plugin-wide config - it's set with `every:` on the auto-sell upgrade's own effect block,
see [How to Make an Upgrade](how-to-make-an-upgrade).

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

<hr/>

## Where to go next

- **Build a minion:** [Configuring a Minion](how-to-make-a-minion/configuring-a-minion) covers the per-type config files.
- **Commands:** [Commands and Permissions](commands-and-permissions) for the reload and admin commands.
