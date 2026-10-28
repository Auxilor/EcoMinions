---
title: "Configuring a Feeder"
sidebar_position: 8
---

A **feeder** refuels the minions around it from fuel that players put in its storage. Its area grows with the `radius` stat like every other type. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `feeder.yml` to a new file. The file name is the minion ID.
2. Set `type: feeder`.
3. List the fuels it may use under `fuels`, or leave it empty for every fuel.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the feeder next to a minion, click fuel into the feeder's storage, and confirm the minion starts burning it.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a feeder

| Part | What it controls |
| --- | --- |
| **Fuels** | Which fuels the feeder may use |
| **Storage** | The fuel players load into it |

```yaml
# === Fuels: what the minion may burn ===
type: feeder # The minion refuels nearby minions.
fuels: [] # Fuel IDs from fuels/. Leave this empty to use every fuel.
```

### Fuels

Each entry is the ID of a fuel from the `fuels/` folder, see [How to Make a Fuel](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-fuel). An ID that names no fuel is reported on load and ignored. With an empty list, every fuel is accepted.

### Storage

Players load fuel into the feeder by clicking its storage slots in the menu.

- **Deposit:** click a storage slot with listed fuel on the cursor. What does not fit stays on the cursor.
- **Refused:** an item that is not a listed fuel is refused with the `messages.input-not-accepted` message.
- **Take back:** click a slot with an empty cursor to take up to one stack.
- **Collect all:** empties the whole feeder.

Shift-clicking from the player inventory does not deposit.

### Who it feeds

Once per cycle the feeder picks one minion in its area and gives it one fuel item from storage. A minion is only fed when all of these hold:

- It is active.
- It holds no fuel at all, neither burning nor frozen.
- Its storage is not full, unless it is a breeder, whose storage holds input and is never full of output.
- It has the same owner as the feeder, or is on the same island when island privileges are on.

It only feeds minions in spots where its owner can build, unless `bypass-antigrief` is set. A feeder never feeds another feeder. Cancelling the fuel event returns the item to the feeder. A feeder does nothing while its owner is offline, and it does not earn catch-up time.

:::tip Troubleshooting
- **Hologram says "No Fuel Stored"?** Click fuel into the feeder's storage.
- **Hologram says "Nothing to Feed"?** No minion in range is active, unfuelled and owned by the same player or island.
- **Item refused?** It is not in `fuels`, or it is not a registered fuel.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion) for stats, item, upgrades and placement.
- **Fuels:** [How to Make a Fuel](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-fuel) for the items a feeder can burn.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
