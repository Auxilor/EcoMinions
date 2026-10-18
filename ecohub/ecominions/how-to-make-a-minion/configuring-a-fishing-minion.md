---
title: "Configuring a Fishing Minion"
sidebar_position: 6
---

A **fishing minion** catches fish from water source blocks in its work area and stores the catch. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `fishing.yml` to a new file. The file name is the minion ID.
2. Set `type: fishing`.
3. Set how many water sources it needs with `min-water`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion next to still water and watch for the splash of its first cast.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a fishing minion

| Part | What it controls |
| --- | --- |
| **Water** | How much water it needs, and whether it lays its own |
| **Catch** | What it catches |

```yaml
# === Water: what the minion fishes in ===
type: fishing # The minion catches fish.
min-water: 1 # Still water source blocks needed before it fishes.
placement:
  self-seed: false # Whether the minion lays its own water.

# === Catch: optional loot table ===
drops: # Replaces the vanilla catch. Leave this out to use vanilla fishing loot.
  - item: cod
    chance: 0.6
  - item: ecoitems:pearl
    chance: 0.02
    xp: 1-3
```

### Water

The minion fishes from still water source blocks in its work area, by default the ring one block below it. Flowing water does not count. While it is short of water it checks every cycle; once it has enough, it rechecks every 20 cycles. With `placement.self-seed: true`, it turns one air or flowing water block on solid ground into a water source each cycle, until the layer is full. Like a miner, it needs a pit: on flat ground its layer is solid, so there is nowhere to lay water.

### Catch

Each catch comes from the minion's own `drops` list first, then EcoItems `fishing` loot while the owner is online, then vanilla fishing loot. `drops` takes the same fields as a slayer's. Each catch is worth 1 to 6 XP, or the `xp` of a `drops` entry. Offline cycles roll `drops` or the vanilla catch; EcoItems loot needs the owner online.

:::tip Troubleshooting
- **Hologram says "No Water"?** The work area has fewer still water sources than `min-water`. A self-seeding minion on flat ground needs the ring below it dug out first.
- **Catch is always vanilla?** EcoItems fishing loot only applies while the owner is online.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion) for stats, item, upgrades and placement.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
