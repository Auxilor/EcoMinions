---
title: "How to Make a Compactor Recipe"
sidebar_position: 4
---

A **compactor recipe** turns items in a minion's storage into a more compact form, such as nine coal into a coal block. Each recipe sets its **input** and **output**, its **price**, and optional **conditions**, and each minion type picks which recipes it can buy. This page covers making one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/compactor-recipes/` folder and copy `_example.yml` to a new file, e.g. `coal_block.yml`. The file name is the recipe ID.
2. Set `name`, `display.icon`, `input`, `output` and their amounts.
3. Set `cost-options`.
4. In a minion's config, add the recipe ID to `compactor-recipes` and add a `minion_compact` effect (see [Letting a minion compact](#letting-a-minion-compact)).
5. Run `/minions reload`, buy the recipe from that minion's upgrades menu, and confirm its storage compacts.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real recipe. You can also organise recipes into subfolders inside `compactor-recipes/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the recipe ID. This is what a minion's `compactor-recipes` list references. Each recipe is bought as its own upgrade, with the ID `compactor_<recipe id>`, so `coal_block` becomes `compactor_coal_block` for the `minion_has_upgrade` condition. `input`, `output` and `display.icon` use the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the recipe will not load.
:::

## The structure of a compactor recipe

| Part | What it controls |
| --- | --- |
| **Display** | The recipe's name and icon in the upgrade menu |
| **Recipe** | What is used up and what is made |
| **Price** | What the recipe costs to buy |
| **Conditions** | Who can buy it and when it runs |

```yaml
# === Display: how it shows in the upgrade menu ===
name: "&8Coal Compactor" # The recipe's name in the menu.
display:
  icon: coal_block # The recipe's icon in the menu.

# === Recipe: what is used up and what is made ===
input: COAL # The item used up.
input-amount: 9 # How many are used up each time.
output: COAL_BLOCK # The item made.
output-amount: 1 # How many are made each time.

# === Price: what the recipe costs ===
cost-options: # The ways a player can pay for the recipe. They pick one.
  - value: 15000
    type: coins
    display: "&a$%value_commas%"

# === Conditions: who can buy it and when it runs ===
buy-conditions: [] # Optional. Conditions that must pass to buy the recipe.
conditions: [] # Optional. Conditions that must pass each time it compacts.
higher-level-of: "" # Optional. The recipe whose output this one uses up, so it runs after it.
```

### Display

How the recipe shows in the minion's upgrade menu, the same as an upgrade.

```yaml
name: "&8Coal Compactor" # The recipe's name in the menu.
display:
  icon: coal_block # The recipe's icon in the menu.
```

### Recipe

What the recipe uses up and makes. It runs again and again while the storage holds at least `input-amount` of `input`.

```yaml
input: COAL # The item used up.
input-amount: 9 # How many are used up each time.
output: COAL_BLOCK # The item made.
output-amount: 1 # How many are made each time.
```

:::warning
`output` must be a different item to `input`, and `input-amount` must be more than `output-amount`. A recipe that feeds itself, or does not shrink the stack, will loop and can destroy a player's items.
:::

### Price

What the recipe costs. It is bought once, with no tiers, and uses the same `cost-options` as an upgrade tier. See [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade#tiers).

```yaml
cost-options: # The ways a player can pay for the recipe. They pick one.
  - value: 15000
    type: coins
    display: "&a$%value_commas%"
```

### Conditions

Two separate, optional gates, plus an ordering hint for chained recipes.

```yaml
buy-conditions: # Optional. Conditions that must pass to buy the recipe.
  - id: minion_has_upgrade
    args:
      upgrade: compactor_coal_block
conditions: # Optional. Conditions that must pass each time it compacts.
  - id: minion_level_above
    args:
      level: 5
higher-level-of: coal_block # Optional. The recipe whose output this one uses up, so it runs after it.
```

`buy-conditions` are checked once, when a player tries to buy the recipe. `conditions` are checked every time the minion compacts, so an owned recipe does nothing while they fail.

`higher-level-of` only sets the order. A recipe that uses up coal blocks with `higher-level-of: coal_block` runs after `coal_block`, so coal becomes coal blocks first. It does not lock the purchase: use a `minion_has_upgrade` buy condition for that, as above.

## Letting a minion compact

A recipe does nothing until a minion type lists it and has a `minion_compact` effect to run it. Both go in the minion's own config:

```yaml
compactor-recipes: # The recipes this minion can buy, in menu order.
  - coal_block
effects: # The effect that runs the minion's owned recipes.
  - id: minion_compact
    triggers:
      - minion_mine_block
```

Listing a recipe also allows its upgrade, so it does not need to be in `allowed-upgrades` too. Without the `minion_compact` effect, owned recipes never run.

:::tip Troubleshooting
- **Recipe not in the menu?** Add its ID to the minion's `compactor-recipes`.
- **Recipe bought but nothing compacts?** The minion needs a `minion_compact` effect with a trigger, and the recipe's `conditions` must pass.
- **Upgrade menu not opening?** Check every cost option has a `display`.
:::

<hr/>

## Where to go next

- **Minions:** list recipes with `compactor-recipes` in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Upgrades:** the shared `cost-options` format is in [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade).
- **Defaults:** the shipped recipes live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/compactor-recipes).
