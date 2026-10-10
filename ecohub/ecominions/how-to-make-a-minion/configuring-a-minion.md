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
  mode: item # Either item or head.
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
effects: []

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
| `miner` | Breaks blocks and puts them back after `regen-delay-ticks`. | [Configuring a Miner](configuring-a-miner) |
| `farmer` | Harvests ripe crops and replants them. | [Configuring a Farmer](configuring-a-farmer) |
| `woodcutter` | Fells trees and plants saplings to grow new ones. | [Configuring a Woodcutter](configuring-a-woodcutter) |
| `slayer` | Kills mobs, and can spawn its own to kill. | [Configuring a Slayer](configuring-a-slayer) |
| `fishing` | Catches fish from water in its area. | [Configuring a Fishing Minion](configuring-a-fishing-minion) |
| `collector` | Picks up dropped items and XP orbs in its area. | [Configuring a Collector](configuring-a-collector) |

### Stats

Level-one values before any upgrade. `storage-slots` and `max-storage-slots` are both capped by `minion.hard-slot-cap` in [Plugin Config](../plugin-config). `regen-delay-ticks` only matters to types that break blocks.

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

```yaml
spawn-item:
  mode: item
  item: "COAL_BLOCK"
  display-name: "&8Coal Minion &7(Lvl %level%)"
  lore:
    - "&8Mines coal ore"
    - "&8Level: &7%level%"
```

`mode` controls how the placeable item looks:

- `mode: item` - the item is a plain material. Set `item` to the material name (e.g. `"COAL_BLOCK"`).
- `mode: head` - the item is a player head with a custom skin texture. Set `texture` to a base64 skin texture
  string instead of `item`.

`display-name` and `lore` support `%level%` and other minion placeholders.

The minion item itself is also registered with eco's item lookup system as `ecominions:<id>` (e.g.
`ecominions:coal`), always resolving to a level-one item of that type. Any plugin or config that consumes eco
item lookups - shops, crates, custom recipes - can reference or hand out a minion this way.

`crafting` nests under `spawn-item`, alongside `mode`/`item`/`display-name`/`lore`:

```yaml
spawn-item:
  mode: item
  item: "COAL_BLOCK"
  display-name: "&8Coal Minion &7(Lvl %level%)"
  lore:
    - "&8Mines coal ore"
    - "&8Level: &7%level%"
  crafting:
    enabled: true
    give-amount: 1
    permission: ""
    shapeless: false
    recipe:
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

- `enabled` - whether this minion has a crafting recipe at all.
- `give-amount` - how many minion items the recipe yields. Defaults to `1`.
- `permission` - the permission node required to craft this recipe. Defaults to `""`, meaning no permission is
  required.
- `shapeless` - when `true`, ingredient position in the grid doesn't matter. Defaults to `false`.
- `recipe` - nine entries, read left-to-right, top-to-bottom as a 3x3 crafting grid. Each entry is an eco item
  lookup with an optional amount - a material name (`"STONE"`), a material with a count (`"STONE:4"`), a custom
  item id such as `"ecominions:coal"`, or `""` for an empty slot.

Minions are craftable at a crafting table only. Recipes are registered and replaced through eco's recipe system.

### Selling

```yaml
sell-prices:
  COAL: 5.0
  "itemsadder:ruby": 100.0
```

The price paid per item when a minion's storage is sold, keyed by **item ID** - what the block drops, not the
block itself. Custom item IDs work here too, which is the only way to price a drop from ItemsAdder, Oraxen,
Nexo or CraftEngine. A minion's own `sell-prices` entries always win over any EcoShop-sourced price and are
never scaled by `sell-pricing.sell-price-multiplier` (see [Plugin Config](../plugin-config)). Items with no entry
here fall back to EcoShop pricing if `sell-pricing.auto-from-ecoshop` is enabled.

### Upgrades

```yaml
allowed-upgrades:
  - speed
  - efficiency
  - fortune
  - radius
  - capacity
  - auto_smelt
  - auto_sell
  - experience
```

The list of upgrade IDs (matching file names under `upgrades/`) this minion can have purchased on it.
Omitting an upgrade from this list hides it from this type's upgrade menu entirely. Compacting is not listed
here - see `compactor-recipes` below.

```yaml
compactor-recipes:
  - coal_block
  - iron_block

effects:
  - id: minion_compact
    triggers:
      - minion_mine_block
```

A list of `compactor-recipes/` catalog ids this minion allows - the order shown is just the order they're
listed in the upgrade menu. Each one is bought and gated independently (its own price, its own
`buy-conditions`); there is no shared "compactor" upgrade or tier ladder. Omitting this key (or leaving it
empty) means the type has no compactor recipes at all. Each resolved upgrade's id is
`compactor_<recipe id>`, and it is automatically added to the type's allowed upgrades.

This key alone does not make compacting run - the type also needs `minion_compact` in its own top-level
`effects:` block, as shown above. See [How to Make a Compactor Recipe](../how-to-make-a-compactor-recipe) for the
recipe file format.

### Effects

```yaml
effects:
  - id: minion_radius
    args:
      blocks: 1
```

`effects` is a list of libreforge effects that are **always active** on every minion of this type, at every
level - no upgrade purchase is needed to enable them. It uses the same effect blocks as an upgrade's `effects`
(see [How to Make an Upgrade](../how-to-make-an-upgrade)), except there's no tier and no `%tier_value%` to bind to,
so args are just plain values.

A type-level effect stacks with any upgrade effects touching the same stat - the example above adds a flat
`+1` to `radius` on top of whatever the `radius` upgrade contributes, since both are just contributions to the
same stat accumulator.

See [Libreforge Components](../libreforge-components) for the full list of minion-specific effects, triggers,
conditions, and mutators, plus the stock libreforge components that also work here.

### Appearance

```yaml
equipment:
  chestplate: "LEATHER_CHESTPLATE"
  leggings: "LEATHER_LEGGINGS"
  boots: "LEATHER_BOOTS"
```

Optional. Dresses the minion's armor stand. Each of `helmet`, `chestplate`, `leggings`, `boots` and `hand` is
an eco item lookup string - the same syntax as `spawn-item.item` - so custom items, `#tags` and player heads
all work, and a dyed leather piece is written the way eco writes any other dyed item rather than through a
separate `color` block.

Every slot is optional, and two have defaults: an omitted `helmet` uses this type's own `spawn-item.item` as
its head, and an omitted `hand` becomes an iron pickaxe. Set a slot to `""` to leave it deliberately bare.

> Renamed from `armor` in an earlier version, where each piece was a `material` plus an optional `r`/`g`/`b`
> `color`. There is no fallback to the old key, so a config still using `armor:` is ignored entirely and the
> minion falls back to the defaults above.

```yaml
particles:
  full:
    enabled: true
    particle: "angry_villager"
    interval-ticks: 20
    count: 4
    offset: 0.4
    nearby-player-radius: 16
```

`full` is emitted while the minion's storage is full and a player is within `nearby-player-radius` blocks.
`particle` is an eco particle string, so anything eco knows works - a particle name like `flame`, or a
factory form like `dust:ff5050` or `dust_transition:ff0000:0000ff`. `interval-ticks` is how often it emits,
`count` how many particles per burst, and `offset` how far, in blocks, they're scattered from the emit point.

Two more layers use the same fields:

- `idle` - shown only while the minion is idle (owner offline or chunk unloaded) and a player is nearby.
  Disabled by default.
- `blocked` - a wireframe box traced around the minion's work area while it has nothing to work on, so
  the area is visible. Sized from `radius` and `height`. Unlike the other two, it's shown
  to the minion's owner alone - a stalled farm would otherwise blanket every passer-by in outlines. Give it
  `offset: 0.0` so the outline lands exactly on its points rather than near them.

### Levelling

```yaml
levelling:
  enabled: true
  xp-formula: "100 * %level% ^ 1.5"
  max-level: 50
  xp-gain-methods:
    - trigger: minion_mine_block
      multiplier: 1
```

Optional. Lets a minion earn XP toward explicit levels instead of levelling purely from purchased upgrade
tiers. See [How to Level a Minion](../how-to-level-a-minion) for the full block, including `xp-requirements`,
`conditions`, and `level-up-effects`.

### Placement

| Key | Used by | Meaning |
| --- | --- | --- |
| `self-seed` | every type except `collector` | The minion makes its own work: places blocks, spawns mobs or lays water, as its type page describes. Defaults to `true` for `woodcutter` and `slayer`, `false` for the rest. |
| `floor-offset` | every type | The first layer of the work area, relative to the minion: `-1` is the layer one block below it, `0` its own level. Defaults by type (see the table above). |
| `lift-regen` | `miner`, `farmer`, `woodcutter` | When `true`, a mined block regenerates one block higher than where it was broken. |
| `surface-block` | `miner`, `farmer`, `woodcutter` | The block required beneath a spot to place a block there. Leave blank to allow any surface. |
| `seed-block` | `miner`, `farmer`, `woodcutter` | What `self-seed` places. Leave blank to seed the target's `plant`, falling back to the mined block itself. |
| `spacing` | `miner`, `farmer`, `woodcutter` | Empty blocks `self-seed` keeps between plants along each straight direction. `0` packs them side by side. |

### Stored XP

Every minion keeps the XP it earns in a counter until its owner claims it. The main menu shows a stored XP
button with the amount; clicking it gives the XP to the player and empties the counter. The button is set
under `gui.main.stored-xp` in `lang.yml`, and removing that section hides it. Set `minion.xp.mode: orb` in
[Plugin Config](../plugin-config) to drop XP as orbs instead.

## Internal placeholders

These resolve anywhere minion placeholders work - holograms, `lang.yml`, menu titles and item names,
`spawn-item.lore`, and the `args` of any effect on the minion.

| Placeholder | Resolves to |
| --- | --- |
| `%minion_name%` | The minion's display `name`, colour codes included. |
| `%type%` | The minion's ID, i.e. its filename without `.yml`. |
| `%kind%` | The minion's `type`, capitalised, e.g. `Miner` or `Fishing`. |
| `%level%` | The minion's current level. |
| `%owner%` | The owner's name, or `Unowned` if it has none. |
| `%used%` | Stack slots currently filled in the minion's storage. |
| `%total%` | Stack slots the minion's storage currently holds. |
| `%capacity%` | Slots its capacity stat allows, after upgrades and the type's `max-storage-slots`. Matches `%total%` in normal operation. |
| `%radius%` | How far the minion reaches horizontally, in blocks. |
| `%diameter%` | The full width of the work box, i.e. `radius * 2 + 1`. |
| `%height%` | The vertical span of the work box, in blocks. |
| `%speed%` | Ticks between actions, after upgrades and any active fuel. |
| `%efficiency%` | Ticks each action itself takes. |
| `%stored_experience%` | XP the minion has stored and its owner has not claimed yet. |

Levelling adds `%xp%`, `%xp_required%`, `%progress%`, `%max_level%` and `%level_numeral%` - see
[How to Level a Minion](../how-to-level-a-minion#placeholders).

:::tip Troubleshooting
- **Minion not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that `type` is one of the six above.
- **Hologram says the area is empty?** The minion has nothing in range to work on; see its type page for what it needs.
- **Storage full?** A full minion stops working until its storage is emptied or sold.
:::

<hr/>

## Where to go next

- **Types:** [Miner](configuring-a-miner), [Farmer](configuring-a-farmer), [Woodcutter](configuring-a-woodcutter), [Slayer](configuring-a-slayer), [Fishing](configuring-a-fishing-minion) and [Collector](configuring-a-collector).
- **Blocks:** [Configuring a Block](configuring-a-block) for custom blocks and per-block overrides.
- **Upgrades:** [How to Make an Upgrade](../how-to-make-an-upgrade) for the `allowed-upgrades` list.
- **Levelling:** [How to Level a Minion](../how-to-level-a-minion) for the `levelling:` block.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
