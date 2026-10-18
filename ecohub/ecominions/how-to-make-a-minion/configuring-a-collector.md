---
title: "Configuring a Collector"
sidebar_position: 7
---

A **collector** picks up dropped items and XP orbs in its work area and stores them. Its area grows with the `radius` stat like every other type. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `collector.yml` to a new file. The file name is the minion ID.
2. Set `type: collector`.
3. List the items to collect under `target-items`, or leave it empty for everything.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the minion next to a farm and confirm a dropped item in range is collected.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a collector

| Part | What it controls |
| --- | --- |
| **Items** | Which dropped items it picks up |
| **XP** | Whether it absorbs XP orbs |

```yaml
# === Items: what the minion picks up ===
type: collector # The minion picks up dropped items.
target-items: [] # Items to collect. Leave this empty to collect everything.

# === XP: orbs in the area ===
collect-xp: true # Whether XP orbs are absorbed into stored XP.
```

### Items

The minion picks up dropped items in its work area once they can be picked up, by default from one block below it to one above. A stack that only partly fits is split, and the rest stays on the ground. It never picks up a minion item. It has nothing to seed and earns nothing offline, since nothing drops while nobody is there.

### XP

With `collect-xp: true`, the default, absorbed XP orbs go into stored XP, whatever `minion.xp.mode` says.

:::tip Troubleshooting
- **Items not collected?** Check the item matches `target-items`, or leave the list empty.
- **Hologram says "Nothing to Collect"?** No dropped item or XP orb is in range yet.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion) for stats, item, upgrades and placement.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
