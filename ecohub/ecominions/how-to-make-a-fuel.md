---
title: "How to Make a Fuel"
sidebar_position: 2
---

A **fuel** is an item players feed to a minion to make it work faster for a while. Each fuel sets the **item** that is burned, how long one item **burns** for, and how much it **speeds up** the minion. This page covers making one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/fuels/` folder and copy `_example.yml` to a new file, e.g. `charcoal.yml`. The file name is the fuel ID.
2. Set `name` and `item`.
3. Set `duration-ticks` and `speed-multiplier`.
4. Run `/minions reload`.
5. Open a placed minion's menu, click the fuel button with the item in your inventory, and confirm the button shows it as active.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real fuel. You can also organise fuels into subfolders inside `fuels/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the fuel ID. Every fuel works on every minion, so nothing else needs to reference it. `item` uses the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), so you can use vanilla items, custom items, and modifiers.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the fuel will not load.
:::

## The structure of a fuel

| Part | What it controls |
| --- | --- |
| **Item** | The item that is burned and its display name |
| **Burn** | How long one item lasts and how much faster the minion works |

```yaml
# === Item: what is burned ===
name: "&8Coal" # The name shown in the minion's fuel button.
item: COAL # The item burned as fuel.

# === Burn: how long and how fast ===
duration-ticks: 12000 # How long one item burns for, in ticks. Defaults to 12000.
speed-multiplier: 1.25 # How much faster the minion works while it burns. Defaults to 1.25.
```

### Item

The item players feed to the minion. It is matched with the item lookup, so a custom item works the same as a vanilla one.

```yaml
name: "&8Coal" # The name shown in the minion's fuel button.
item: COAL # The item burned as fuel.
```

Players feed fuel from the minion's menu. A click feeds one item, a shift-click feeds every matching item in their inventory, and dropping a stack on the button feeds the stack.

### Burn

How long one item lasts, and how much it speeds the minion up while it burns.

```yaml
duration-ticks: 12000 # How long one item burns for, in ticks. Defaults to 12000.
speed-multiplier: 1.25 # How much faster the minion works while it burns. Defaults to 1.25.
```

The minion's `speed-ticks` is divided by `speed-multiplier` while the fuel burns, so `1.25` makes a 60 tick minion act every 48 ticks. The `%speed%` placeholder shows the result.

:::info
Feeding more of the fuel that is already burning adds to the time left. Feeding a different fuel replaces the current one, and the time left on the old fuel is lost.
:::

:::tip
Fuel only burns while the minion is working. An idle minion (its owner offline, or its chunk unloaded) keeps its fuel frozen. When the owner comes back, fuel is used for the simulated offline time only, not the whole time they were away.
:::

:::tip Troubleshooting
- **Fuel not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that `item` is a valid item.
- **Fuel button says you have no fuel?** No item in your inventory matches any fuel's `item`.
- **Minion not faster?** Check `speed-multiplier` is above `1`.
:::

<hr/>

## Where to go next

- **Minions:** set the base `speed-ticks` a fuel speeds up in [Configuring a Minion](how-to-make-a-minion/configuring-a-minion).
- **Upgrades:** make permanent speed upgrades in [How to Make an Upgrade](how-to-make-an-upgrade).
- **Defaults:** the shipped fuels live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/fuels).
