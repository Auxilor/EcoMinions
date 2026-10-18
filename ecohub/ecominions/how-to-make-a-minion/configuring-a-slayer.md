---
title: "Configuring a Slayer"
sidebar_position: 5
---

A **slayer** kills mobs in its work area and stores what they drop. It can spawn its own frozen mobs to kill, or wait for mobs to wander in. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `zombie.yml` to a new file. The file name is the minion ID.
2. Set `type: slayer`.
3. List the mobs to kill under `target-mobs`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion on open ground and confirm it spawns and kills its first mob.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a slayer

| Part | What it controls |
| --- | --- |
| **Targets** | Which mobs it kills, and who gets the kill |
| **Spawning** | Its own frozen mobs, and how many at once |
| **Drops** | An optional table that replaces what the mob drops |

```yaml
# === Targets: which mobs the minion kills ===
type: slayer # The minion kills mobs.
target-mobs: # The mobs to kill.
  - zombie
  - ecomobs:tank_zombie
credit-owner: false # Whether kills count as the owner's while they are online.

# === Spawning: its own frozen mobs ===
max-mobs: 4 # The most spawned mobs alive at once. May use %level%.
placement:
  self-seed: true # Whether the minion spawns its own mobs.

# === Drops: optional loot table ===
drops: # Replaces what the mob drops. Leave this out to keep the mob's own drops.
  - item: rotten_flesh
    chance: 1
    amount: 1-2
    xp: 1-3
```

### Targets

The minion kills any living mob in its work area that matches `target-mobs`: vanilla mobs, mobs with arguments like `zombie baby`, or custom mobs like `ecomobs:tank_zombie`. It never attacks players, armour stands, tamed pets with an owner, or mobs with a custom name, and it leaves mobs spawned by other slayers alone. By default the work area runs from its own level up two blocks.

With `credit-owner: true`, a kill made while the owner is online counts as theirs: vanilla loot that needs a player kill can drop, and the owner's own kill effects fire on every kill. It is off by default.

### Spawning

`placement.self-seed` defaults to `true` for a slayer. It spawns its own mobs on open ground in its work area, up to `max-mobs` (default 4). They have no AI, cannot move or hurt players, and do not burn in daylight; players can still hit them. Picking up or removing the minion removes them. With `self-seed: false`, it only kills mobs that wander in.

### Drops

`drops` replaces what a killed mob drops. Each entry rolls its own chance and takes `item`, `chance` (0 to 1, default 1), `amount` (a number or range, default 1) and `xp` (a number or range). Leave it out to store the mob's own drops and XP. EcoItems `mob` loot applies too. Each offline cycle replays one of the last ten real kills, so a slayer that has not killed anything yet earns nothing offline.

:::tip Troubleshooting
- **Hologram says "No Mobs"?** Nothing in range matches `target-mobs`, and `self-seed` is off or `max-mobs` is reached.
- **No mobs spawning?** It needs a spot with a solid block below and two free blocks above.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](configuring-a-minion) for stats, item, upgrades and placement.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
