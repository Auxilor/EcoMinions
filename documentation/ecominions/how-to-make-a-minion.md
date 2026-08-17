---
title: "How to Make a Minion"
sidebar_position: 1
---

## Minion Types

Each minion type is a YAML file in `minions/`. The file name (without `.yml`) is the type ID, and is what
players and admins refer to elsewhere - `/minions give <player> coal`, the `ecominions:coal` item lookup, and
so on. An `_example.yml` is provided in the folder to copy from.

## Common Config

```yaml
name: "&8Coal Minion"

target-blocks:
  - COAL_ORE
  - DEEPSLATE_COAL_ORE

base-stats:
  speed-ticks: 70
  efficiency-ticks: 12
  radius: 3
  inventory-rows: 3
  regen-delay-ticks: 120
```

- `name` - the minion's display name, shown on its hologram and in its item lore.
- `target-blocks` - the block types this minion mines. Only these blocks are broken and regenerated.
- `base-stats` - level-one stats before any upgrades are applied:
  - `speed-ticks` - ticks between mining attempts.
  - `efficiency-ticks` - additional ticks the mining animation/action takes.
  - `radius` - how far from the minion, in blocks, it searches for target blocks.
  - `inventory-rows` - rows in the minion's own storage inventory.
  - `regen-delay-ticks` - how long a mined block takes to regenerate.

## effects

```yaml
effects:
  - id: minion_radius
    args:
      blocks: 1
```

`effects` is a list of libreforge effects that are **always active** on every minion of this type, at every
level - no upgrade purchase is needed to enable them. It uses the same effect blocks as an upgrade's `effects`
(see [How to Make an Upgrade](how-to-make-an-upgrade)), except there's no tier and no `%tier_value%` to bind to,
so args are just plain values.

A type-level effect stacks with any upgrade effects touching the same stat - the example above adds a flat
`+1` to `radius` on top of whatever the `radius` upgrade contributes, since both are just contributions to the
same stat accumulator.

See [Libreforge Components](libreforge-components) for the full list of minion-specific effects, triggers,
conditions, and mutators, plus the stock libreforge components that also work here.

## spawn-item

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

### spawn-item.crafting

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

## sell-prices

```yaml
sell-prices:
  COAL: 5.0
```

The price paid per item when a minion's storage is sold, keyed by material name. A minion type's own
`sell-prices` entries always win over any EcoShop-sourced price and are never scaled by
`sell-pricing.sell-price-multiplier` (see [Plugin Config](plugin-config)). Materials with no entry here fall
back to EcoShop pricing if `sell-pricing.auto-from-ecoshop` is enabled.

## allowed-upgrades

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
  - compactor
```

The list of upgrade IDs (matching file names under `upgrades/`) this minion type can have purchased on it.
Omitting an upgrade from this list hides it from this type's upgrade menu entirely.

## armor

```yaml
armor:
  chestplate:
    material: LEATHER_CHESTPLATE
    color:
      r: 54
      g: 54
      b: 54
  leggings:
    material: LEATHER_LEGGINGS
    color:
      r: 54
      g: 54
      b: 54
  boots:
    material: LEATHER_BOOTS
    color:
      r: 54
      g: 54
      b: 54
```

Optional. Any of `helmet`, `chestplate`, `leggings`, `boots` can be set to dress the minion's armor stand.
`material` is required per piece; `color` (an `r`/`g`/`b` triple) is optional and only takes effect on dyeable
leather armor.

## particles

```yaml
particles:
  full:
    enabled: true
    type: ANGRY_VILLAGER
    interval-ticks: 20
    count: 4
    offset: 0.4
    speed: 0.0
    nearby-player-radius: 16
```

`full` is emitted while the minion's storage is full and a player is within `nearby-player-radius` blocks.
`type` is a Bukkit particle name, `interval-ticks` how often it emits, `count`/`offset`/`speed` the usual
particle spawn parameters.

An equivalent `particles.idle` block is also available, mirroring `full`'s fields. It's shown only while the
minion is idle (owner offline or chunk unloaded) and only when a player is nearby, and is disabled by default.

## placement

```yaml
placement:
  lift-regen: false
  surface-block: ""
```

- `lift-regen` - when `true`, a mined block regenerates one block higher than where it was broken.
- `surface-block` - the material required beneath a spot for `lift-regen` to place a block there. Leave blank
  to allow any surface.

<hr/>

## Where to go next

- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) for the `allowed-upgrades` list.
- **Build a fuel:** [How to Make a Fuel](how-to-make-a-fuel) to keep minions active.
- **Plugin config:** [Plugin Config](plugin-config) for `sell-pricing` and other plugin-wide settings.
