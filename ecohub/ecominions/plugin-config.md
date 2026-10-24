---
title: "Plugin Config"
sidebar_position: 8
---

This is the server-wide config for EcoMinions, `config.yml`, found at `/plugins/EcoMinions/config.yml`. It holds disabled worlds, placement limits, offline catch-up, sell pricing, minion behaviour, and the menu titles. Apply changes with `/minions reload`.

## Default config.yml

```yaml
# EcoMinions main config

# Worlds that EcoMinions should be disabled in
disabled-in-worlds: []

limits:
  # What happens when a placement would go past any limit below.
  # prevent: the placement is refused.
  # inactive: the minion is placed, and the newest-placed minions over the limit stay idle until there is room.
  over-limit: prevent
  per-player:
    enabled: true
    default: 5
    permission-overrides: true
  per-island:
    enabled: false # If islands have a cap on placed minions. Needs an island plugin.
    default: 10 # The cap for every island.
    permission-overrides: true # If ecominions.island-limit.<number> on the island owner can raise the island's cap.
  per-type:
    enabled: false # If each player has a cap per minion type.
    permission-overrides: true # If ecominions.type-limit.<type>.<number> can raise a player's cap for that type.
    types: # The cap for each type. A type left out has no cap.
      miner: 10
      farmer: 10

islands:
  privileges: true # If island ranks decide who may place, use and pick up minions. Needs an island plugin.
  active-while-member-online: false # If a minion on an island keeps working while any island member is online, not only its owner.

unloaded-calculation:
  enabled: true
  # Multiplier applied to offline drops relative to live mining.
  # 1.0 = full parity, 0.75 = 75% of live rate while offline.
  offline-efficiency: 1.0

sell-pricing:
  # Pull sell prices from EcoShop for any material with no explicit price in a
  # minion's sell-prices section. Requires EcoShop to be installed.
  auto-from-ecoshop: true
  # Scales EcoShop-sourced prices only. Explicit sell-prices are never scaled.
  sell-price-multiplier: 1.0

minion:
  # Hologram offset above the minion (Y axis blocks)
  hologram-offset: 1.6
  # How often (in ticks) to refresh the hologram contents
  hologram-refresh-ticks: 20
  # Sets the invulnerable flag on the minion's armour stand. Minions always
  # ignore damage, whatever this is set to.
  invulnerable: true

  # Block regen visual: replace block with cobblestone briefly during regen?
  # If false, block disappears entirely until regen tick.
  show-regen-marker: true
  # Block to place when a minion is picked up and regen markers haven't fired yet.
  # Replaces any cobblestone regen markers left behind. Use AIR to simply remove them.
  regen-marker-fallback-block: AIR

  # What happens to the blocks a minion placed (seeded ores and crops, saplings,
  # grown trees and the water it laid) when it is picked up or removed:
  #   mine  - they stay in the world for players to mine
  #   clear - they are removed, unless a player has since replaced them
  seeded-blocks-on-pickup: mine

  # Upper bound on any minion type's storage-slots and max-storage-slots.
  # Minion storage is virtual and is never opened as a chest, so this is a
  # sanity guard against typos, not a Minecraft limit. Raise it if your types
  # need more.
  hard-slot-cap: 54

  xp:
    # Where the XP a minion earns goes.
    # store - kept on the minion and claimed from its menu.
    # orb - dropped as an orb at the minion.
    mode: store


gui:
  main:
    title: "&8%minion_name% &8(&7Lvl %level%&8)"
    rows: 6
    # How collected item counts are shown: title | lore | both
    storage-display: lore
    # Max item types shown in the storage area; highest count first
    storage-slots: 9
    # Whether the stored XP button is shown.
    show-stored-xp: true
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

### Disabled worlds

`disabled-in-worlds` is a list of world names, not case sensitive. In those worlds minions cannot be placed, do not work, and have no effects.

### Limits

`limits.per-player` caps how many minions one player can have placed. `default` is the cap for everyone. With `permission-overrides: true`, a player with `ecominions.limit.<number>` gets that cap instead when it is higher, e.g. `ecominions.limit.10`. Players with `ecominions.admin` have no cap. See [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).

`limits.per-island` caps how many minions one island can hold, whoever placed them. It is off by default and needs an island plugin, see [Supported Plugins](https://hub.auxilor.io/wiki/ecominions/supported-plugins). `default` is the cap for every island. With `permission-overrides: true`, an island whose owner has `ecominions.island-limit.<number>` gets that cap instead when it is higher. Players with `ecominions.admin` skip this cap too. When both limits are on, both apply. Turn `per-player` off for an island-only cap. A Lands land counts as an island.

`limits.per-type` caps how many minions of each `type` one player can have placed, e.g. at most 10 miners of any kind. It is off by default. Each key under `types` is a type (`miner`, `farmer`, `woodcutter`, `slayer`, `fishing`, `collector`, `feeder` or `breeder`); a type left out has no cap. With `permission-overrides: true`, a player with `ecominions.type-limit.<type>.<number>` gets that cap instead when it is higher, e.g. `ecominions.type-limit.miner.15`. A single minion can have its own cap too, with `limit` in its config, see [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion#limit).

Every limit counts the minions of the minion's owner, whoever places it. `limits.over-limit` decides what happens past any limit:

| Value | What happens |
| --- | --- |
| `prevent` | The placement is refused with a message. Minions already placed keep working if a cap is lowered. |
| `inactive` | The minion is placed, but the newest-placed minions over a limit stay idle and earn nothing until there is room. Picking one up lets the next one start. Lowering a cap idles the newest extra minions. |

A minion idled by one limit does not use up room under the other player limits. Idle minions show `hologram.limited` in `lang.yml`, and their menu has a limits button that says which limit holds them back.

### Islands

`islands.privileges` lets island ranks decide who may place, use and pick up minions on an island. It is on by default and does nothing without an island plugin.

`islands.active-while-member-online` is off by default, so a minion works only while its owner is online. With `true`, a minion on an island works while its owner or any member of that island is online, and idles only when none of them are. Offline payout counts the time nobody from the island was online.

### Offline catch-up

A minion is idle while its owner is offline or its chunk is unloaded. When it wakes up, the idle time is paid out in one go: work is simulated for the time that passed, scaled by `offline-efficiency`, and the drops go through the minion's own effects, so upgrades like fortune still apply. Fuel is used for the simulated time only. With `enabled: false`, a minion picks up where it left off and gets nothing for the idle time.

### Sell pricing

A minion's own `sell-prices` always win, and are never scaled. For items with no price there, `auto-from-ecoshop: true` uses the EcoShop sell price, scaled by `sell-price-multiplier`. This needs EcoShop installed.

### Minion

`show-regen-marker` puts cobblestone where a block is waiting to regenerate. `regen-marker-fallback-block` replaces any markers left behind when a minion is picked up.

`seeded-blocks-on-pickup` decides what happens to blocks a self-seeding minion placed when it is picked up or removed. With `mine` they stay for players to mine. With `clear` they are removed, except where a player has replaced them since.

`hard-slot-cap` is the most `storage-slots` and `max-storage-slots` any minion can have, however high a formula goes.

`xp.mode` decides where a minion's XP goes, from ores, kills, catches, effects, or EcoItems loot. With `store` it is kept on the minion and claimed from its menu. With `orb` it drops as an orb at the minion. A collector always stores the orbs it picks up.

:::info
There is no plugin-wide auto sell interval. Set it with the trigger on the auto sell upgrade's effect, see [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade#effects).
:::

### Menus

The title and rows of each menu: `main` is a minion's menu, `upgrade-list` its upgrades, `upgrade` one upgrade's tiers, `cost-select` the payment chooser, and `type-select` the minion browser opened by `/minions`. Buttons and their text live in `lang.yml`.

<hr/>

## Where to go next

- **Minions:** the per-minion config is in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Island plugins:** ranks, privileges and the island limit are in [Supported Plugins](https://hub.auxilor.io/wiki/ecominions/supported-plugins).
- **Permissions:** the limit and admin nodes are in [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).
- **Placeholders:** use minion counts and limits elsewhere with [PlaceholderAPI](https://hub.auxilor.io/wiki/ecominions/placeholderapi).
