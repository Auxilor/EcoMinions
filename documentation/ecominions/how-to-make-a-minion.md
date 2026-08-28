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
  height: 3
  storage-slots: 27
  max-storage-slots: 54
  regen-delay-ticks: 120
```

- `name` - the minion's display name, shown on its hologram and in its item lore.
- `target-blocks` - the block types this minion mines. Only these blocks are broken and regenerated.
- `base-stats` - level-one stats before any upgrades are applied:
  - `speed-ticks` - ticks between mining attempts.
  - `efficiency-ticks` - additional ticks the mining animation/action takes.
  - `radius` - how far from the minion, in blocks, it searches for target blocks.
  - `height` - the vertical span of that search, in blocks, measured from one block below the minion
    upward. Defaults to `3` - one below, the minion's own level, and one above - which is the box every
    type had before this was configurable. Raise it for tall targets: the wood minion uses `6`, exactly
    the reach needed for the top log of a full tree.
  - `storage-slots` - stack slots in the minion's own storage, before upgrades.
  - `max-storage-slots` - the most slots capacity upgrades may reach on this type.
    Both are capped by `minion.hard-slot-cap` in `config.yml`.
  - `regen-delay-ticks` - how long a mined block takes to regenerate.

Every `base-stats` value may be a plain number or an expression in `%level%`, evaluated for the minion's
current level. This lets stats scale as a levelling minion progresses, without needing an upgrade for it -
`"60 - %level%"` makes `speed-ticks` fall by one per level, and `"3 + floor(%level% / 10)"` makes `radius` grow
by one every ten levels. Plain numbers still work exactly as before; a non-levelling minion is just always
evaluated at level 1. See [How to Level a Minion](how-to-level-a-minion) for how a minion's level itself is
earned.

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
```

The list of upgrade IDs (matching file names under `upgrades/`) this minion type can have purchased on it.
Omitting an upgrade from this list hides it from this type's upgrade menu entirely. Compacting is not listed
here - see `compactor-recipes` below.

## compactor-recipes

```yaml
compactor-recipes:
  - coal_block
  - iron_block

effects:
  - id: minion_compact
    triggers:
      - minion_mine_block
```

A list of `compactor-recipes/` catalog ids this minion type allows - the order shown is just the order they're
listed in the upgrade menu. Each one is bought and gated independently (its own price, its own
`buy-conditions`); there is no shared "compactor" upgrade or tier ladder. Omitting this key (or leaving it
empty) means the type has no compactor recipes at all. Each resolved upgrade's id is
`compactor_<recipe id>`, and it is automatically added to the type's allowed upgrades.

This key alone does not make compacting run - the type also needs `minion_compact` in its own top-level
`effects:` block, as shown above. See [How to Make a Compactor Recipe](how-to-make-a-compactor-recipe) for the
recipe file format.

## equipment

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

## particles

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
- `blocked` - a wireframe box traced around the minion's mining area while it has nothing left to mine, so
  the area that needs clearing is visible. Sized from `radius` and `height`. Unlike the other two, it's shown
  to the minion's owner alone - a stalled farm would otherwise blanket every passer-by in outlines. Give it
  `offset: 0.0` so the outline lands exactly on its points rather than near them.

## levelling

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
tiers. See [How to Level a Minion](how-to-level-a-minion) for the full block, including `xp-requirements`,
`conditions`, and `level-up-effects`.

## placement

```yaml
placement:
  lift-regen: false
  surface-block: ""
  self-seed: false
  seed-block: ""
```

- `lift-regen` - when `true`, a mined block regenerates one block higher than where it was broken.
- `surface-block` - the material required beneath a spot for `lift-regen` to place a block there. Leave blank
  to allow any surface.
- `self-seed` - when `true`, a minion with nothing left to mine places its own block: the nearest air spot in
  range with a valid surface beneath it. Place the minion on a floor and it runs on its own. When `false`, a
  player must place the starting blocks and the minion reports being blocked until they do. Tree types default
  to `true`, since they have always planted their own saplings; every other type defaults to `false`.
- `seed-block` - what `self-seed` places. Leave blank to seed the block the minion mines - its first
  `target-blocks` entry, or the sapling of its wood family for a tree type.

A seeded spot needs a surface: `surface-block` if one is set, otherwise any solid block directly beneath.
A crop minion therefore seeds only onto farmland, and a minion standing over a hole stays blocked.

<hr/>

## Placeholders

These resolve anywhere minion placeholders work - holograms, `lang.yml`, menu titles and item names,
`spawn-item.lore`, and the `args` of any effect on the minion.

| Placeholder | Resolves to |
| --- | --- |
| `%minion_name%` | The type's display `name`, colour codes included. |
| `%type%` | The type's ID, i.e. its filename without `.yml`. |
| `%level%` | The minion's current level. |
| `%owner%` | The owner's name, or `Unowned` if it has none. |
| `%used%` | Stack slots currently filled in the minion's storage. |
| `%total%` | Stack slots the minion's storage currently holds. |
| `%capacity%` | Slots its capacity stat allows, after upgrades and the type's `max-storage-slots`. Matches `%total%` in normal operation. |
| `%radius%` | How far the minion reaches horizontally, in blocks. |
| `%diameter%` | The full width of the mining box, i.e. `radius * 2 + 1`. |
| `%height%` | The vertical span of the mining box, in blocks. |
| `%speed%` | Ticks between mining attempts, after upgrades. Active fuel speeds the minion up without changing this number. |
| `%efficiency%` | Ticks each mining action itself takes. |

Levelling adds `%xp%`, `%xp_required%`, `%progress%`, `%max_level%` and `%level_numeral%` - see
[How to Level a Minion](how-to-level-a-minion#placeholders).

## Where to go next

- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) for the `allowed-upgrades` list.
- **Level a minion:** [How to Level a Minion](how-to-level-a-minion) for the `levelling:` block.
- **Build a fuel:** [How to Make a Fuel](how-to-make-a-fuel) to keep minions active.
- **Plugin config:** [Plugin Config](plugin-config) for `sell-pricing` and other plugin-wide settings.
