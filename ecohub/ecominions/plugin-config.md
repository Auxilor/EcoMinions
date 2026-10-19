---
title: "Plugin Config"
sidebar_position: 8
---

This is the server-wide config for EcoMinions, `config.yml`, found at `/plugins/EcoMinions/config.yml`. It holds disabled worlds, placement limits, offline catch-up, sell pricing, minion behaviour, and the menu layouts. Apply changes with `/minions reload`.

## Default config.yml

```yaml
# EcoMinions main config

# Worlds that EcoMinions should be disabled in
disabled-in-worlds: []

limits:
  per-player:
    enabled: true
    default: 5
    permission-overrides: true
  per-island:
    enabled: false # If islands have a cap on placed minions. Needs an island plugin.
    default: 10 # The cap for every island.
    permission-overrides: true # If ecominions.island-limit.<number> on the island owner can raise the island's cap.

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


# The layout of each menu.
# A button's item and location are set here, and its name and lore in lang.yml.
# A location is a row and a column, counted from the top left corner, which is row 1, column 1.
# An area is the rectangle from its top-left slot to its bottom-right slot.
# If you change rows, move the locations and areas so they still fit in the menu.
gui:
  main: # A minion's own menu.
    title: "&8%minion_name% &8(&7Lvl %level%&8)"
    rows: 6
    storage-display: lore # How stored item counts are shown: title, lore or both.
    storage-slots: 9 # The most item types shown in the storage area, highest count first.
    show-stored-xp: true # If the stored XP button is shown.
    # The background. Each digit in pattern is the item at that position in items, and 0 is empty.
    mask:
      items:
        - black_stained_glass_pane
      pattern:
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
    custom-slots: [] # Extra items placed in the menu, each with an item, a location and optional click actions.
    title-item: # The minion itself, at the top.
      item: player_head
      location:
        row: 1
        column: 5
    storage-area: # Where the stored items are shown. Empty slots show the filler item from lang.yml.
      top-left:
        row: 2
        column: 2
      bottom-right:
        row: 5
        column: 8
    collect-all: # Moves every stored item into the player's inventory.
      item: hopper
      location:
        row: 6
        column: 2
    fuel: # Adds fuel. While fuel burns, it shows that fuel's item instead.
      item: coal
      location:
        row: 6
        column: 4
    pickup: # Picks the minion up.
      item: barrier
      location:
        row: 6
        column: 5
    stored-xp: # Claims the stored XP.
      item: experience_bottle
      location:
        row: 6
        column: 6
    upgrades: # Opens the minion's upgrades.
      item: anvil
      location:
        row: 6
        column: 7
    info: # Shows the minion's stats.
      item: book
      location:
        row: 6
        column: 8
  upgrade: # One upgrade's tiers.
    title: "&8Upgrade: &f%upgrade_name%"
    rows: 6
    # The background. Each digit in pattern is the item at that position in items, and 0 is empty.
    mask:
      items:
        - black_stained_glass_pane
      pattern:
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
    custom-slots: [] # Extra items placed in the menu, each with an item, a location and optional click actions.
    title-item: # The upgrade's icon. Its item is the upgrade's own icon.
      location:
        row: 1
        column: 5
    tier-area: # Where the tiers are shown, lowest first. Tiers that do not fit are not shown.
      top-left:
        row: 2
        column: 2
      bottom-right:
        row: 5
        column: 8
    back: # Goes back to the upgrade list.
      item: arrow
      location:
        row: 6
        column: 1
  type-select: # The minion browser opened by /minions.
    title: "&8Choose a Minion"
    rows: 6
    # The background, set on every page. Each digit in pattern is the item at that position in items, and 0 is empty.
    mask:
      items:
        - black_stained_glass_pane
      pattern:
        - "000000000"
        - "000000000"
        - "000000000"
        - "000000000"
        - "000000000"
        - "000000000"
    custom-slots: [] # Extra items placed in the menu, each with an item, a location and optional click actions.
    type-area: # Where the minions are shown. Minions that do not fit go on the next page.
      top-left:
        row: 2
        column: 2
      bottom-right:
        row: 5
        column: 8
    previous-page: # Goes to the previous page. Hidden on the first page.
      item: arrow
      location:
        row: 6
        column: 1
    next-page: # Goes to the next page. Hidden on the last page.
      item: arrow
      location:
        row: 6
        column: 9
  upgrade-list: # A minion's upgrades.
    title: "&8Upgrades: &f%minion_name%"
    rows: 6
    # The background. Each digit in pattern is the item at that position in items, and 0 is empty.
    mask:
      items:
        - black_stained_glass_pane
      pattern:
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
        - "111111111"
    custom-slots: [] # Extra items placed in the menu, each with an item, a location and optional click actions.
    upgrade-area: # Where the upgrades are shown. Upgrades that do not fit are not shown.
      top-left:
        row: 2
        column: 2
      bottom-right:
        row: 5
        column: 8
    back: # Goes back to the minion's menu.
      item: arrow
      location:
        row: 6
        column: 1
  cost-select: # The payment chooser.
    title: "&8Choose Payment"
    rows: 3
    # The background. Each digit in pattern is the item at that position in items, and 0 is empty.
    mask:
      items:
        - black_stained_glass_pane
      pattern:
        - "111111111"
        - "111111111"
        - "111111111"
    custom-slots: [] # Extra items placed in the menu, each with an item, a location and optional click actions.
    option-row: 2 # The row the payment options are shown in, from column 2 to column 8.
```

### Disabled worlds

`disabled-in-worlds` is a list of world names, not case sensitive. In those worlds minions cannot be placed, do not work, and have no effects.

### Limits

`limits.per-player` caps how many minions one player can have placed. `default` is the cap for everyone. With `permission-overrides: true`, a player with `ecominions.limit.<number>` gets that cap instead when it is higher, e.g. `ecominions.limit.10`. Players with `ecominions.admin` have no cap. See [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).

`limits.per-island` caps how many minions one island can hold, whoever placed them. It is off by default and needs an island plugin, see [Supported Plugins](https://hub.auxilor.io/wiki/ecominions/supported-plugins). `default` is the cap for every island. With `permission-overrides: true`, an island whose owner has `ecominions.island-limit.<number>` gets that cap instead when it is higher. Players with `ecominions.admin` skip this cap too. When both limits are on, both apply. Turn `per-player` off for an island-only cap. A Lands land counts as an island.

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

Each menu has a `title` and `rows`: `main` is a minion's menu, `upgrade-list` its upgrades, `upgrade` one upgrade's tiers, `cost-select` the payment chooser, and `type-select` the minion browser opened by `/minions`. Button names and lore live in `lang.yml`. Everything else about the layout is set here.

| Key | Menus | What it controls |
|---|---|---|
| `mask` | all | The background. `items` is a list of items, and each digit in `pattern` places the item at that position in the list. `0` leaves the slot empty. `type-select` ships with an empty background. |
| `custom-slots` | all | Extra items placed in the menu, each with an `item`, a `location` and optional click actions. A custom slot replaces any built-in item in the same slot. |
| `title-item` | `main`, `upgrade` | The item at the top. In `upgrade` only its location can be changed, because it shows the upgrade's own icon. |
| `storage-area` | `main` | Where the minion's stored items are shown. Empty slots show `gui.filler.material` from `lang.yml`. |
| `collect-all`, `fuel`, `pickup`, `stored-xp`, `upgrades`, `info` | `main` | The minion's buttons. While fuel burns, the fuel button shows that fuel's item. |
| `type-area` | `type-select` | Where the minions are shown. Minions that do not fit go on the next page. |
| `previous-page`, `next-page` | `type-select` | The page buttons. Each one is hidden when there is no page in its direction. |
| `upgrade-area` | `upgrade-list` | Where the upgrades are shown. Upgrades that do not fit are not shown. |
| `tier-area` | `upgrade` | Where the tiers are shown, lowest first. Tiers that do not fit are not shown. |
| `back` | `upgrade-list`, `upgrade` | The back button. |
| `option-row` | `cost-select` | The row the payment options are shown in, from column 2 to column 8. |

A button has an `item` and a `location` with a `row` and a `column`, counted from the top left slot, which is row 1, column 1.

An area is the rectangle from its `top-left` slot to its `bottom-right` slot.

:::warning
The shipped locations and areas fit the shipped `rows`. If you change `rows`, move them too, or the menu will not open.
:::

<hr/>

## Where to go next

- **Minions:** the per-minion config is in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Island plugins:** ranks, privileges and the island limit are in [Supported Plugins](https://hub.auxilor.io/wiki/ecominions/supported-plugins).
- **Permissions:** the limit and admin nodes are in [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).
- **Placeholders:** use minion counts and limits elsewhere with [PlaceholderAPI](https://hub.auxilor.io/wiki/ecominions/placeholderapi).
