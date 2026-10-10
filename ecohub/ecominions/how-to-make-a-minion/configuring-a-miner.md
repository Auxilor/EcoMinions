---
title: "Configuring a Miner"
sidebar_position: 2
---

A **miner** breaks its target blocks and puts each one back exactly as it was after `regen-delay-ticks`. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `coal.yml` to a new file. The file name is the minion ID.
2. Set `type: miner`.
3. List the blocks it mines under `target-blocks`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion over a dug-out ring and confirm it mines into its storage.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a miner

| Part | What it controls |
| --- | --- |
| **Targets** | Which blocks it mines |
| **Work area** | The ring below the minion it mines in |
| **Self-seeding** | Placing its own blocks into an empty ring |

```yaml
# === Targets: what the minion mines ===
type: miner # The minion breaks and regenerates blocks.
target-blocks: # The blocks it mines. Any block or custom block id.
  - COAL_ORE
  - DEEPSLATE_COAL_ORE

# === Work area: the ring below the minion ===
base-stats:
  height: 1 # The layer below the minion only.

# === Self-seeding: filling an empty ring ===
placement:
  self-seed: true # Whether the minion places its own target blocks.
```

### Targets

The minion mines any block in its work area that matches `target-blocks`. Offline cycles roll each target block's real drop table; a custom block whose drops cannot be read offline needs `drops` in its [block definition](configuring-a-block). Vanilla ores are worth their vanilla XP, and any block can set its own with `xp` in its block definition.

### Work area

By default the work area is the ring one block below the minion (`height: 1`, `floor-offset: -1`), where its blocks sit.

### Self-seeding

With `placement.self-seed: true`, the minion places its own target blocks into air in its work area. Every configured target is seeded, not only the first. On flat ground there is no air in the layer below, so it seeds nothing and stays blocked until the ring is dug out.

:::tip Troubleshooting
- **Hologram says "No Blocks to Mine"?** Nothing in the work area matches `target-blocks`. With `self-seed` on, dig out the ring below the minion.
- **Minion not loading?** Every entry in `target-blocks` must be a block that can be mined.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](configuring-a-minion) for stats, item, upgrades and placement.
- **Blocks:** [Configuring a Block](configuring-a-block) for custom blocks and per-block overrides.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
