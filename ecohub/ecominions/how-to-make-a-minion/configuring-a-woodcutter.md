---
title: "Configuring a Woodcutter"
sidebar_position: 4
---

A **woodcutter** fells its target trees by mining their logs, plants a sapling, and waits for it to grow. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `wood.yml` to a new file. The file name is the minion ID.
2. Set `type: woodcutter`.
3. List the logs it fells under `target-blocks`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion on grass and confirm it plants and fells trees.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a woodcutter

| Part | What it controls |
| --- | --- |
| **Targets** | Which logs it fells |
| **Work area** | The space its trees grow in |
| **Self-seeding** | Planting saplings, spaced apart |

```yaml
# === Targets: what the minion fells ===
type: woodcutter # The minion fells trees.
target-blocks: # The logs it fells. Every target must be a tree.
  - OAK_LOG

# === Work area: room for a full tree ===
base-stats:
  height: 5 # Reaches the top log of a full tree.

# === Self-seeding: planting saplings ===
placement:
  self-seed: true # Woodcutters plant their own saplings by default.
  spacing: 2 # Room between trees for their leaves.
```

### Targets

Every target must be a tree, meaning it has a `plant` and `leaves`: built in for vanilla logs, set in a [block definition](configuring-a-block) for a custom one. Anything else is rejected at startup, because a woodcutter clears what it breaks. Offline cycles roll each log's real drop table. Logs are worth no XP unless their block definition sets `xp`.

### Work area

By default the work area runs from the minion's own level up four blocks (`height: 5`, `floor-offset: 0`), which reaches the top log of a full tree.

### Self-seeding

`placement.self-seed` defaults to `true`. The minion fills its work area with several trees kept apart by `spacing`.

## Tree shapes

A woodcutter grows its tree from a built-in shape. The shape comes from the block definition's `tree-pattern`, or from the definition's file name when that is not set. A name that matches no shape grows the generic tree, so a custom log in `blocks/ebony.yml` needs `tree-pattern` set to get a real shape.

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

### Trunk height and the minion's reach

Every shape has a four-log trunk. A woodcutter fells its tree by mining the logs, so every log has to be inside its work area. The work area reaches `height - 1` blocks above the minion, and a sapling is planted at most one block above it, so with the default `height: 5` the highest log it can reach is four above the sapling.

:::info
The tree shapes are built into the plugin and cannot be changed in config.
:::

:::tip Troubleshooting
- **Top logs left standing?** The trunk is taller than the work area; raise `height`.
- **Minion not loading?** A target without `plant` and `leaves` is not a tree.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](configuring-a-minion) for stats, item, upgrades and placement.
- **Blocks:** [Configuring a Block](configuring-a-block) for custom blocks and per-block overrides.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
