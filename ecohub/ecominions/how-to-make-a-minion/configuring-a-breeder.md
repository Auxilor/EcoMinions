---
title: "Configuring a Breeder"
sidebar_position: 9
---

A **breeder** breeds the animals around it with food that players put in its storage. Its area grows with the `radius` stat like every other type. This page builds one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/minions/` folder and copy `breeder.yml` to a new file. The file name is the minion ID.
2. Set `type: breeder`.
3. List the animals under `target-mobs` and the food under `food`.
4. Run `/minions reload`, then `/minions give <player> <id>` to hand the minion out.
5. Place the breeder next to a pen with two adults, click food into its storage, and confirm they breed.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real minion. You can also organise minions into subfolders inside `minions/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the minion ID, as for every type. Everything not on this page is shared by every type and covered in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the minion will not load.
:::

## The structure of a breeder

| Part | What it controls |
| --- | --- |
| **Animals** | Which animals it breeds |
| **Food** | The items it accepts as food |
| **Cap** | How many animals it allows in its area |

```yaml
# === Animals: who it breeds ===
type: breeder # The minion breeds nearby animals.
target-mobs: [cow, sheep, pig, chicken] # Required. The animals to breed.

# === Food: what players load into storage ===
food: [WHEAT, CARROT, WHEAT_SEEDS] # Required. The items accepted as food.

# === Cap: how many animals fit ===
max-mobs: 8 # The most target animals in the area, babies included.
```

### Animals

`target-mobs` is required and may not be empty. Each entry is a vanilla mob type or a custom mob id, such as `cow` or `ecomobs:tank_zombie`. An entry that names no mob is reported on load and ignored, and a breeder with no valid mob does not load. The breeder skips animals that have a custom name, are already in love, were spawned by a slayer minion, are not adults, or are still on their vanilla breeding cooldown.

### Food

`food` is required and may not be empty. Each entry is an item or custom item id. An animal is only bred with food that is both in `food` and food that animal eats in vanilla, so listing `WHEAT` does not make pigs breed on it.

Players load food into the breeder by clicking its storage slots in the menu.

- **Deposit:** click a storage slot with listed food on the cursor. What does not fit stays on the cursor.
- **Refused:** an item that is not in `food` is refused with the `messages.input-not-accepted` message.
- **Take back:** click a slot with an empty cursor to take up to one stack.
- **Collect all:** empties the whole breeder.

Shift-clicking from the player inventory does not deposit.

### Cap

Each cycle the breeder breeds one pair, using one food item for each parent. It needs two eligible adults of the same kind in range. `max-mobs` is the most target animals it allows in its area. Babies count toward it, and once the count reaches the cap the breeder stops until some are gone. It may use `%level%`, and it defaults to `8`. It only works animals in spots where its owner can build, unless `bypass-antigrief` is set. A breeder never spawns animals, so a pen must already hold a pair. A breeder does nothing while its owner is offline, and it does not earn catch-up time.

:::tip Troubleshooting
- **Hologram says "No Food Stored"?** Click food into the breeder's storage.
- **Hologram says "Nothing to Breed"?** No two eligible adults are in range, one of them is still on its breeding cooldown, or the stored food is not eaten by them in vanilla.
- **Hologram says "Pen Full"?** The area holds `max-mobs` target animals already, babies included.
:::

<hr/>

## Where to go next

- **Shared config:** [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion) for stats, item, upgrades and placement.
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
