---
title: "Configuring a Farmer"
sidebar_position: 3
---

A **farmer** harvests its target crops when they are ripe and replants them at age zero, then lets the world grow them. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `wheat.yml` to a new file. The file name is the minion ID.
2. Set `type: farmer`.
3. List the crops it harvests under `target-blocks`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion on farmland and confirm it plants and harvests.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a farmer

| Part | What it controls |
| --- | --- |
| **Targets** | Which crops it harvests |
| **Work area** | The layer the crops stand in |
| **Self-seeding** | Planting its own crops |

```yaml
# === Targets: what the minion harvests ===
type: farmer # The minion harvests and replants crops.
target-blocks: # The crops it harvests. Any block or custom block id.
  - WHEAT

# === Work area: the minion's own level ===
base-stats:
  height: 1 # The layer the crops stand in.

# === Self-seeding: planting its own crops ===
placement:
  self-seed: true # Whether the minion plants its own crops.
  spacing: 0 # Empty blocks kept between plants.
```

### Targets

The minion harvests a crop once it is ripe and replants it. It is paced by how fast the crop ripens, not by `regen-delay-ticks`. A target with no `plant` regenerates instead of being left as a hole. Targets are blocks, so list the crop block (`WHEAT`, `CARROTS`, `BEETROOTS`), not its seed item: `WHEAT_SEEDS` or `BEETROOT_SEEDS` is not a block and is rejected at startup. Offline cycles roll each target's real drop table. Crops are worth no XP unless their [block definition](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-block) sets `xp`.

### Work area

By default the work area is the minion's own level (`height: 1`, `floor-offset: 0`), where crops stand on the soil below.

### Self-seeding

With `placement.self-seed: true`, the minion plants its own crops on valid soil in its work area: farmland for crops, soul sand for nether wart, sand for cactus. Columns such as cactus need `spacing: 1`. A sugar cane minion lays its own water and plants cane beside it.

:::tip Troubleshooting
- **Nothing planted?** The layer below the work area needs the crop's soil, e.g. farmland for wheat.
- **Crops never harvested?** The crop has to ripen in the world first; light and water still matter.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion) for stats, item, upgrades and placement.
- **Blocks:** [Configuring a Block](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-block) for custom blocks and per-block overrides.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
