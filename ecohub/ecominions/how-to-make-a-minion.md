---
title: "How to Make a Minion"
sidebar_position: 1
---

## Minions

Each minion is a YAML file in `minions/`. The file name (without `.yml`) is the minion's ID, and is what
players and admins refer to elsewhere - `/minions give <player> coal`, the `ecominions:coal` item lookup, and
so on. An `_example.yml` is provided in the folder to copy from.

## Common Config

```yaml
name: "&8Coal Minion"
type: miner

target-blocks:
  - COAL_ORE
  - DEEPSLATE_COAL_ORE

base-stats:
  speed-ticks: 70
  efficiency-ticks: 12
  radius: 3
  height: 1
  storage-slots: 27
  max-storage-slots: 54
  regen-delay-ticks: 120
```

- `name` - the minion's display name, shown on its hologram and in its item lore.
- `type` - **required.** What the minion does with the blocks it breaks:
  - `miner` - breaks the block and puts it back exactly as it was, fully grown, after `regen-delay-ticks`.
  - `woodcutter` - fells the tree, plants a sapling, and waits `regen-delay-ticks` for it to grow. Every
    target must be a tree,
    meaning it has a `plant` and `leaves` - built in for vanilla logs, and set in `blocks/` for a custom one.
    A woodcutter aimed at anything else is **rejected at startup**, because it clears what it breaks and
    would destroy that block permanently.
  - `farmer` - harvests the ripe crop and replants it at age zero, then lets the world grow it. A farmer is
    paced by how fast the crop ripens, not by `regen-delay-ticks`. A target with no `plant` regenerates
    instead, rather than being left as a hole.

  A woodcutter lists its log in `target-blocks` like any other minion lists its block; there is no
  auto-detection. A single block can override its minion's type - see [Block Definitions](#block-definitions).
- `target-blocks` - the blocks this minion mines. Only these are broken and restored. Custom block IDs from
  ItemsAdder, Oraxen, Nexo and CraftEngine work anywhere a vanilla material name does.
- `base-stats` - level-one stats before any upgrades are applied:
  - `speed-ticks` - ticks between mining attempts.
  - `efficiency-ticks` - additional ticks the mining animation/action takes.
  - `radius` - how far from the minion, in blocks, it searches for target blocks.
  - `height` - the vertical span of that search, in blocks, measured from one block below the minion
    upward. Left out, it defaults by type:
    - `miner` - `1`: just the floor ring around the minion, where its blocks sit.
    - `farmer` - `2`: the soil below and the crop standing at the minion's own level.
    - `woodcutter` - `6`: exactly the reach needed for the top log of a full tree.
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
  "itemsadder:ruby": 100.0
```

The price paid per item when a minion's storage is sold, keyed by **item ID** - what the block drops, not the
block itself. Custom item IDs work here too, which is the only way to price a drop from ItemsAdder, Oraxen,
Nexo or CraftEngine. A minion's own `sell-prices` entries always win over any EcoShop-sourced price and are
never scaled by `sell-pricing.sell-price-multiplier` (see [Plugin Config](plugin-config)). Items with no entry
here fall back to EcoShop pricing if `sell-pricing.auto-from-ecoshop` is enabled.

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

The list of upgrade IDs (matching file names under `upgrades/`) this minion can have purchased on it.
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

A list of `compactor-recipes/` catalog ids this minion allows - the order shown is just the order they're
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
  spacing: 0
```

- `lift-regen` - when `true`, a mined block regenerates one block higher than where it was broken.
- `surface-block` - the material required beneath a spot for `lift-regen` to place a block there. Leave blank
  to allow any surface.
- `self-seed` - when `true`, a minion with nothing left to mine places its own block: the nearest air spot in
  range with a valid surface beneath it. Place the minion on a floor and it runs on its own. When `false`, a
  player must place the starting blocks and the minion reports being blocked until they do. Woodcutters default
  to `true`, since they have always planted their own saplings; every other type defaults to `false`.
- `seed-block` - what `self-seed` places. Any block or custom block ID. Leave blank to seed what the target's
  block definition names as its `plant` - a sapling for a woodcutter, a crop for a farmer - falling back to
  the mined block itself.
- `spacing` - how many empty blocks `self-seed` keeps between each plant, in every direction including
  diagonals. `0` packs plants side by side. Cactus and sugar cane use `1` (cactus breaks next to another
  block); trees use `2` so their leaves have room. Any plant within range counts, at any height in the work
  area, so a tall cactus column blocks its neighbours all the way up. A minion with no spot far enough from
  every plant stops seeding until one frees up.

A seeded spot needs a surface: `surface-block` if one is set, otherwise any solid block directly beneath.
A crop minion therefore seeds only onto farmland, and a minion standing over a hole stays blocked.

<hr/>

## Block Definitions

A block definition describes what a block **is**. The minion's `type` decides what is **done** with it. That
split is why the same `OAK_LOG` is felled by a woodcutter and regenerated by a miner, with no per-block config
for either.

**`blocks/` ships empty, and that is normal.** Vanilla Minecraft is built in: the plugin already knows every
tree's sapling and leaves, that wheat sits on farmland, that cactus and sugar cane are cut at their base, and
which blocks break too badly for a minion to touch. Everything else - stone, ores, deepslate, gravel - needs
nothing at all.

Write a file here only to describe a custom block, to change a built-in default, or to override what a
minion's type would otherwise do. A file always wins over the built-in definition for the same block, so
every default is overridable without touching the jar.

The file name (without `.yml`) is the definition's ID, so `blocks/oak.yml` has the ID `oak`. The `block` key
is what matches it to a target - `target-blocks` names `OAK_LOG`, not `oak`.

```yaml
block: OAK_LOG
plant: OAK_SAPLING
leaves: OAK_LEAVES
surface: GRASS_BLOCK
```

| Key | Meaning |
| --- | --- |
| `block` | **Required.** The block this describes. Any block ID eco understands, custom blocks included. |
| `plant` | What is planted to bring the block back - a sapling for a woodcutter, a crop for a farmer. |
| `leaves` | The canopy of the grown tree. Needed by woodcutters. |
| `surface` | What must be underneath to plant here. Leave it out to accept any solid block. Also supplies the default lift-regen surface, which is how a crop lands on farmland without anyone configuring it. |
| `tree-pattern` | Which built-in tree shape to grow - see [Tree Shapes](#tree-shapes). Defaults to the definition's own ID, falling back to a generic small tree. |
| `drops` | Item IDs credited during offline catch-up. Only needed for custom blocks, whose loot tables cannot be read while nobody is online. A custom block with no `drops` credits nothing offline. |
| `mineable` | Defaults to `true`. Set it `false` for blocks that break badly - amethyst buds, dripstone, vines - so no minion can be configured onto them. Targeting one is reported as a config violation. |
| `stacked-column` | Defaults to `false`. Marks a block that grows upward in a column, like sugar cane or bamboo. A column is only ever cut at its base, taking the whole stack in one action. |
| `promotes-to` | What targeting this block really means. A minion configured onto seeds mines the grown crop, so the seed promotes to it. |
| `restore` | **Override.** Forces `regen` (as a miner), `replant` (as a farmer) or `tree` (as a woodcutter) for this one block, beating the minion's `type`. Almost never needed. An override the definition cannot satisfy - `tree` with no `leaves` - is reported and dropped, leaving the type to decide. |

<hr/>

## Tree Shapes

A woodcutter grows its tree from a built-in shape. Which one comes from the block definition's
`tree-pattern`, or - if that is not set - from the definition's own file name. A name that matches nothing
grows the generic shape, so a custom log in `blocks/ebony.yml` gets the generic tree unless it names one.

| Shape | Canopy | Looks like |
| --- | --- | --- |
| `oak` | round, radius 2, 2 layers | A standard broadleaf. |
| `jungle` | round, radius 2, 2 layers | Same build as oak. |
| `pale_oak` | round, radius 2, 2 layers | Same build as oak. |
| `mangrove` | round, radius 2, 2 layers | Same build as oak. |
| `birch` | round, radius 1, 3 layers | Narrow and tall-canopied. |
| `spruce` | conifer, radius 2, 3 layers | Rings that narrow towards the top. |
| `acacia` | flat, radius 3, 1 layer | One wide slab, no depth. |
| `dark_oak` | round, radius 3, 2 layers | The widest canopy. |
| `cherry` | round, radius 3, 2 layers | Same build as dark oak. |
| *(unrecognised)* | round, radius 1, 1 layer | A small, plain tree. |

Four of the nine deliberately share the oak build - vanilla does not give those species a distinct enough
silhouette to be worth a separate shape, and inventing one would only make them wrong in a different way.

### Trunk height and the minion's reach

**Every shape has a four-log trunk, and that is not arbitrary.** A woodcutter fells its tree by *mining the
logs*, so every log has to fall inside the minion's search box. That box reaches `height - 2` blocks above
the minion, and a sapling can be planted one block above it - so with `height: 6` the highest reachable log
is exactly four above the sapling.

A five-log trunk would leave its top log standing forever and the minion would never clear the tree. If you
ever want taller trees, `height` has to rise with them.

Shapes live in `TreeShape.SPECIES` in the plugin rather than in a config file, so adding a tenth needs a
build. Ask if you want them exposed as config - it is a small change.

<hr/>

## Placeholders

These resolve anywhere minion placeholders work - holograms, `lang.yml`, menu titles and item names,
`spawn-item.lore`, and the `args` of any effect on the minion.

| Placeholder | Resolves to |
| --- | --- |
| `%minion_name%` | The minion's display `name`, colour codes included. |
| `%type%` | The minion's ID, i.e. its filename without `.yml`. |
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
