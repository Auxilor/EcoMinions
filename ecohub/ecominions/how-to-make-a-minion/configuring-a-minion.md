---
title: "Configuring a Minion"
sidebar_position: 1
---

A **minion** works an area around itself and fills its own storage for its owner. Every minion is one config file with a **type**, its **stats**, its **item**, and the **upgrades**, **appearance** and **placement** shared by every type. Each type then reads a few keys of its own, covered on its own page. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `_example.yml` to a new file, e.g. `coal.yml`. The file name is the minion ID.
2. Set `name` and `type`, then the keys on that type's page.
3. Set `base-stats`, `spawn-item` and `allowed-upgrades`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion and confirm its hologram shows it working.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID. This is what `/minions give` and the `ecominions:<id>` item lookup use. Items, blocks and mobs use the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), [Block Lookup System](https://hub.auxilor.io/wiki/eco/the-block-lookup-system) and [Entity Lookup System](https://hub.auxilor.io/wiki/eco/the-entity-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a minion

| Part | What it controls |
| --- | --- |
| **Type** | What the minion does with its turn |
| **Stats** | Speed, efficiency, work area and storage, before upgrades |
| **Item** | The placeable item and its crafting recipe |
| **Selling** | What its storage sells for |
| **Upgrades** | Which upgrades and compactor recipes it can buy |
| **Effects** | libreforge effects that are always active |
| **Appearance** | Its equipment and particles |
| **Levelling** | Optional XP-based levels |
| **Placement** | Where its work area starts and how it makes its own work |

```yaml
# === Type: what the minion does ===
name: "&8Coal Minion" # The name shown on its hologram and item.
type: miner # One of miner, farmer, woodcutter, slayer, fishing or collector.
target-blocks: # Type keys go here; see the type's page.
  - COAL_ORE
bypass-antigrief: false # If true, it is placed and works where its owner cannot build.
limit: 2 # Optional. The most of this minion each player can place.

# === Stats: level-one values before upgrades ===
base-stats:
  speed-ticks: 70 # Ticks between actions.
  efficiency-ticks: 12 # Extra ticks each action takes.
  radius: 3 # How far the work area reaches.
  height: 1 # How tall the work area is.
  storage-slots: 27 # Stack slots in its storage.
  max-storage-slots: 54 # The most slots capacity upgrades can reach.
  regen-delay-ticks: 120 # How long a broken block takes to come back.

# === Item: the placeable minion ===
spawn-item:
  item: "COAL_BLOCK" # The item it is placed from.
  display-name: "&8Coal Minion &7(Lvl %level%)" # The item name.
  lore:
    - "&8Mines coal ore"
  crafting:
    enabled: true # Whether it has a recipe.

# === Selling: prices per stored item ===
sell-prices:
  COAL: 5.0

# === Upgrades: what it can buy ===
allowed-upgrades:
  - speed
  - capacity
compactor-recipes:
  - coal_block

# === Effects: always active ===
effects:
  - id: minion_compact
    triggers:
      - minion_mine_block
conditions: [] # Optional. Conditions that must pass for the effects to be active.

# === Appearance: armour and particles ===
equipment:
  chestplate: "LEATHER_CHESTPLATE"
particles:
  full:
    enabled: true

# === Levelling: optional XP levels ===
levelling:
  enabled: false

# === Placement: work area and self-seeding ===
placement:
  self-seed: false # Whether the minion makes its own work.
  floor-offset: -1 # The first layer of the work area, relative to the minion.
```

### Type

`type` is **required**. Each type has its own page for the keys only it reads.

| Type | What it does | Page |
| --- | --- | --- |
| `miner` | Breaks blocks and puts them back after `regen-delay-ticks`. | [Configuring a Miner](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-miner) |
| `farmer` | Harvests ripe crops and replants them. | [Configuring a Farmer](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-farmer) |
| `woodcutter` | Fells trees and plants saplings to grow new ones. | [Configuring a Woodcutter](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-woodcutter) |
| `slayer` | Kills mobs, and can spawn its own to kill. | [Configuring a Slayer](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-slayer) |
| `fishing` | Catches fish from water in its area. | [Configuring a Fishing Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-fishing-minion) |
| `collector` | Picks up dropped items and XP orbs in its area. | [Configuring a Collector](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-collector) |

### Stats

Level-one values before any upgrade. `storage-slots` and `max-storage-slots` are both capped by `minion.hard-slot-cap` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config). `regen-delay-ticks` only matters to types that break blocks.

Every value may be a plain number or an expression in `%level%`: `"60 - %level%"` makes `speed-ticks` fall by one per level. A non-levelling minion is always evaluated at level 1.

`height` is counted upward from `placement.floor-offset`. Left out, both default by type:

| Type | `height` | `floor-offset` | Work area (y relative to the minion) |
| --- | --- | --- | --- |
| `miner` | 1 | -1 | -1 |
| `farmer` | 1 | 0 | 0 |
| `woodcutter` | 5 | 0 | 0 to +4 |
| `slayer` | 3 | 0 | 0 to +2 |
| `fishing` | 1 | -1 | -1 |
| `collector` | 3 | -1 | -1 to +1 |

### Item

The item players place the minion from, and its optional crafting recipe.

```yaml
spawn-item:
  item: "COAL_BLOCK" # The item it is placed from.
  display-name: "&8Coal Minion &7(Lvl %level%)" # The item name.
  lore: # The item lore.
    - "&8Mines coal ore"
    - "&8Level: &7%level%"
  crafting:
    enabled: true # Whether it has a recipe.
    give-amount: 1 # How many minions the recipe makes. Defaults to 1.
    permission: "" # Optional. The permission needed to craft it. Blank allows everyone.
    shapeless: false # If true, slot positions don't matter. Defaults to false.
    recipe: # The 3x3 recipe ingredients, use "" for empty slots.
      - "COAL_BLOCK"
      - "STONE"
      - "COAL_BLOCK"
      - "STONE"
      - "ARMOR_STAND"
      - "STONE"
      - "COAL_BLOCK"
      - "STONE"
      - "COAL_BLOCK"
```

`item` is any item lookup, so a custom item, a `#tag`, or a player head with `player_head texture:<base64>` all work. `display-name` and `lore` support `%level%` and the other internal placeholders below.

Every minion is also an item lookup, `ecominions:<id>`, always at level 1, so shops, crates and other plugins can sell or hand out minions directly.

:::tip
Recipes support both shaped and shapeless layouts. See [Recipes](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system/recipes).
:::

### Selling

What each stored item sells for, keyed by **item ID**: what the block drops, not the block.

```yaml
sell-prices:
  COAL: 5.0
  "itemsadder:ruby": 100.0 # Custom items can be priced too.
```

Prices here always win over EcoShop prices and are never scaled. Items with no price here use EcoShop's sell price if `sell-pricing.auto-from-ecoshop` is on in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config). The auto sell upgrade only pays for items that have a price one way or the other.

### Upgrades

Which upgrades and compactor recipes this minion can buy.

```yaml
allowed-upgrades: # Upgrade IDs this minion can buy, from upgrades/.
  - speed
  - efficiency
  - fortune
  - radius
  - capacity
  - auto_smelt
  - auto_sell
  - experience
compactor-recipes: # Optional. Compactor recipe IDs this minion can buy, in menu order.
  - coal_block
  - iron_block
```

An upgrade not in `allowed-upgrades` does not show in this minion's upgrade menu. Each compactor recipe is bought on its own and needs no entry in `allowed-upgrades`. Owned recipes only run with a `minion_compact` effect, see below and [How to Make a Compactor Recipe](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-compactor-recipe).

### Effects

libreforge effects that are always active on every minion of this type, with no upgrade needed.

```yaml
effects:
  - id: minion_radius
    args:
      blocks: 1 # Adds 1 to the radius, on top of any radius upgrade.
  - id: minion_compact
    triggers:
      - minion_mine_block # Runs the minion's owned compactor recipes.
conditions: [] # Optional. Conditions that must pass for the effects to be active.
```

They work the same as an upgrade's effects (see [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade#effects)), without `%tier_value%`. They add to upgrade effects on the same stat, so the example's `+1` radius stacks with the radius upgrade.

:::tip
EcoMinions' own triggers, effects, conditions and mutators are listed on the [EcoMinions wiki](https://hub.auxilor.io/wiki/ecominions), and only work on minions. Stock libreforge effects that change drops, like `multiply_drops` and `autosmelt`, work with `minion_mine_block`, `minion_kill_mob` and `minion_catch_fish`.
:::

:::warning Offline owners
A minion keeps working on triggers like `minion_mine_block` without its owner, and catches up their offline time when they return. Effects that act on the player, like `send_message` or `give_money`, do nothing while the owner is offline. Guard them with the `minion_owner_online` condition, or move the chain onto the owner with the `minion_owner_as_dispatcher` mutator.
:::

### Appearance

What the minion wears, and the particles it shows.

```yaml
equipment: # Optional. Each slot is an item lookup.
  helmet: "" # Leave out to wear the spawn item on its head.
  chestplate: "LEATHER_CHESTPLATE"
  leggings: "LEATHER_LEGGINGS"
  boots: "LEATHER_BOOTS"
  hand: "" # Leave out to hold an iron pickaxe.

particles:
  full: # Shown while its storage is full.
    enabled: true
    particle: "angry_villager" # An eco particle, e.g. flame or dust:ff5050.
    interval-ticks: 20 # How often a burst is shown.
    count: 4 # Particles per burst.
    offset: 0.4 # How far the particles spread, in blocks.
    nearby-player-radius: 16 # Only shown when a player is this close.
  idle: # Shown while the minion is idle, instead of full.
    enabled: false
    particle: "smoke"
    interval-ticks: 20
    count: 2
    offset: 0.3
    nearby-player-radius: 16
  blocked: # A box around the work area while there is nothing to work on.
    enabled: true
    particle: "dust:ff5050"
    interval-ticks: 20
    count: 1
    offset: 0.0
    nearby-player-radius: 16
```

Every equipment slot is optional. Set a slot to `""` to leave it bare on purpose. A dyed leather piece is written the way any dyed item is in the item lookup.

The `blocked` box is sized from `radius` and `height`, and only the owner sees it. Keep its `offset` at `0.0` so the outline is drawn exactly on the box.

:::warning
`equipment` replaced the old `armor` section. A config that still uses `armor` is ignored, and the minion wears no armour: only the spawn item on its head and an iron pickaxe in its hand.
:::

### Levelling

Optional XP-based levels, instead of a level made from the upgrade tiers it owns.

```yaml
levelling:
  enabled: true # Whether this minion levels with XP.
  xp-formula: "100 * %level% ^ 1.5" # The XP to reach %level%.
  max-level: 50 # The highest level.
  xp-gain-methods: # What gives XP.
    - trigger: minion_mine_block
      multiplier: 1
```

See [How to Level a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-level-a-minion) for the full block.

### Placement

Where the work area starts, and how the minion makes its own work.

```yaml
placement:
  self-seed: false # Whether the minion makes its own work.
  floor-offset: -1 # The first layer of the work area, relative to the minion.
  seed-block: "" # What self-seeding places. Blank seeds the target's plant, or the target itself.
  spacing: 0 # Empty blocks kept between self-seeded plants.
  lift-regen: false # Whether a regenerating block gets surface-block put back under it.
  surface-block: "" # The block needed under a spot. Required when lift-regen is on.
```

| Key | Used by | Meaning |
| --- | --- | --- |
| `self-seed` | every type except `collector` | The minion makes its own work: places blocks, spawns mobs or lays water, as its type page describes. Defaults to `true` for `woodcutter` and `slayer`, `false` for the rest. |
| `floor-offset` | every type | The first layer of the work area, relative to the minion: `-1` is the layer one block below it, `0` its own level. Defaults by type (see the table above). |
| `seed-block` | `miner`, `farmer`, `woodcutter` | What `self-seed` places. Leave blank to seed each target's `plant`, falling back to the target block itself. |
| `spacing` | `miner`, `farmer`, `woodcutter` | Empty blocks `self-seed` keeps between plants, along each straight direction. `0` packs them side by side. |
| `lift-regen` | `miner`, `farmer`, `woodcutter` | When a block regenerates, `surface-block` is placed under it first if it is missing, e.g. farmland under wheat. Left out, it is on when a target has a `surface` in its [block definition](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-block), as crops and trees do. |
| `surface-block` | `miner`, `farmer`, `woodcutter` | The block placed under a regenerating block when `lift-regen` is on. |

### Land protection

A minion respects land protection plugins, such as WorldGuard, GriefPrevention, Lands, Towny, SuperiorSkyblock2 and BentoBox. A player can't place one where they can't build. A placed minion leaves alone every block in its work area that its owner can't build at: it does not mine, plant, seed or lay water there, and it does not collect items or kill mobs standing there.

```yaml
bypass-antigrief: false # If true, it is placed and works where its owner cannot build.
```

:::warning Bypassing protection
A type with `bypass-antigrief: true` ignores land protection completely, so its owner can use it to grief claims. Give it only to admins.
:::

### Limit

```yaml
limit: 2 # Optional. The most of this minion each player can place.
```

Leave `limit` out for no cap. A player with `ecominions.minion-limit.<minion>.<number>` gets that cap instead when it is higher, e.g. `ecominions.minion-limit.coal.4`. What happens at the cap is set by `limits.over-limit` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config#limits).

### Stored XP

Every minion keeps the XP it earns until its owner claims it from the stored XP button in its menu. The button is set under `gui.main.stored-xp` in `lang.yml`. Set `gui.main.show-stored-xp: false` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config) to hide it. Set `minion.xp.mode: orb` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config) to drop XP as orbs instead.

## Internal placeholders

These work in holograms, `lang.yml`, menu titles and item names, `spawn-item.lore`, and the `args` of any effect on the minion.

| Placeholder | Value |
| --- | --- |
| `%minion_name%` | The minion's `name`, colour codes included. |
| `%type%` | The minion's ID, i.e. its file name without `.yml`. |
| `%kind%` | The minion's `type`, capitalised, e.g. `Miner` or `Fishing`. |
| `%level%` | The minion's current level. |
| `%owner%` | The owner's name, or `Unowned` if it has none. |
| `%used%` | Stack slots filled in the minion's storage. |
| `%total%` | Stack slots in the minion's storage. |
| `%capacity%` | Slots its capacity stat allows, after upgrades and `max-storage-slots`. The same as `%total%` in normal use. |
| `%radius%` | How far the work area reaches sideways, in blocks. |
| `%diameter%` | The full width of the work area, i.e. `radius * 2 + 1`. |
| `%height%` | How tall the work area is, in blocks. |
| `%speed%` | Ticks between actions, after upgrades and any burning fuel. |
| `%efficiency%` | Ticks each action takes. |
| `%stored_experience%` | XP the minion has stored that its owner has not claimed. |
| `%limit_status%` | `gui.main.limit.status-active` or `status-limited` from `lang.yml`. |
| `%limit_name%` | The limit holding the minion back: its name, its type, `total` or `island`. Empty when none does. |
| `%limit_count%`, `%limit_max%` | Minions placed under that limit, and its cap. Empty when no limit holds it back. |
| `%minion_count%`, `%minion_limit%` | The owner's minions of this ID, and their cap. |
| `%type_count%`, `%type_limit%` | The owner's minions of this type, and their cap. |
| `%player_count%`, `%player_limit%` | The owner's minions in total, and their cap. |

Levelling adds `%xp%`, `%xp_required%`, `%progress%`, `%max_level%`, `%level_numeral%` and `%level_<offset>%`, see [How to Level a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-level-a-minion#internal-placeholders).

:::tip Troubleshooting
- **Minion not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that `type` is one of the six above.
- **Hologram says the area is empty?** The minion has nothing in range to work on; see its type page for what it needs.
- **Storage full?** A full minion stops working until its storage is emptied or sold.
:::

<hr/>

## Where to go next

- **Types:** [Miner](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-miner), [Farmer](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-farmer), [Woodcutter](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-woodcutter), [Slayer](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-slayer), [Fishing](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-fishing-minion) and [Collector](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-collector).
- **Blocks:** [Configuring a Block](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-block) for custom blocks and per-block overrides.
- **Upgrades:** [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade) for the `allowed-upgrades` list.
- **Levelling:** [How to Level a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-level-a-minion) for the `levelling:` block.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
