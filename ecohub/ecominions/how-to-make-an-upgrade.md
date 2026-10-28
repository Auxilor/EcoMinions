---
title: "How to Make an Upgrade"
sidebar_position: 3
---

An **upgrade** is something players buy for a placed minion, one **tier** at a time. Each upgrade is a set of **effects**, a ladder of **tiers** with their own costs, and the **display** shown in the upgrade menu. This page covers making one from scratch.

## Quick start

1. Open the `/plugins/EcoMinions/upgrades/` folder and copy `_example.yml` to a new file, e.g. `speed.yml`. The file name is the upgrade ID.
2. Set `name` and `display.icon`.
3. Add the `effects` the upgrade gives, using `%tier_value%` for the number that changes per tier.
4. Add each tier under `tiers`, with its `value`, `stat-delta` and `cost-options`.
5. Add the upgrade ID to a minion's `allowed-upgrades`, run `/minions reload`, open that minion's upgrades menu, and confirm it appears.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real upgrade. You can also organise upgrades into subfolders inside `upgrades/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the upgrade ID. This is what a minion's `allowed-upgrades` list and the `minion_has_upgrade` condition reference. `display.icon` uses the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), and `cost-options` use the [Price Lookup System](https://hub.auxilor.io/wiki/eco/the-price-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the upgrade will not load.
:::

## The structure of an upgrade

| Part | What it controls |
| --- | --- |
| **Display** | The upgrade's name and icon in the upgrade menu, and whether players can switch it off |
| **Effects** | What the upgrade does, shared by every tier |
| **Tiers** | Each tier's value, menu text, costs and buy conditions |

```yaml
# === Display: how it shows in the upgrade menu ===
name: "&bSpeed" # The upgrade's name in the menu.
display:
  icon: sugar # The upgrade's icon in the menu.
toggleable: true # Optional. Whether the owner can switch it off once bought. Defaults to true.

# === Effects: what the upgrade does ===
effects: # Effects shared by every tier. %tier_value% is the owned tier's value.
  - id: minion_speed
    args:
      ticks_faster: "%tier_value%"
conditions: [] # Optional. Conditions that must pass for the effects to be active.

# === Tiers: the ladder players buy up ===
tiers:
  1:
    value: 5 # The number %tier_value% resolves to at this tier.
    stat-delta: "-5 ticks" # Menu text describing this tier.
    cost-options: # The ways a player can pay for this tier. They pick one.
      - value: 5000
        type: coins
        display: "&a$%value_commas%"
      - value: 64
        type: stone
        display: "&7%value_commas%x Stone"
    buy-conditions: [] # Optional. Conditions that must pass to buy this tier.
  2:
    value: 10
    stat-delta: "-10 ticks"
    cost-options:
      - value: 12000
        type: coins
        display: "&a$%value_commas%"
```

### Display

How the upgrade shows in the minion's upgrade menu.

```yaml
name: "&bSpeed" # The upgrade's name in the menu.
display:
  icon: sugar # The upgrade's icon in the menu.
toggleable: true # Optional. Whether the owner can switch it off once bought. Defaults to true.
```

Once a minion owns an upgrade, its owner can right-click it in the upgrade menu to switch it off, and right-click again to switch it back on. A switched-off upgrade keeps its tier and still counts towards the minion's level, but its effects stop. Set `toggleable: false` for upgrades that should always stay on. The shipped `capacity` upgrade does this, because switching it off would shrink storage that already holds items.

### Effects

The effects the upgrade gives. They are written once and shared by every tier, and only the owned tier is ever active. Buying the next tier swaps the old tier out for the new one.

```yaml
effects: # Effects shared by every tier. %tier_value% is the owned tier's value.
  - id: minion_speed
    args:
      ticks_faster: "%tier_value%"
conditions: [] # Optional. Conditions that must pass for the effects to be active.
```

Two placeholders work inside the effects:

| Placeholder | Value |
| --- | --- |
| `%tier_value%` | The `value` of the tier the minion owns. |
| `%tier%` | The tier number the minion owns. |

Permanent effects such as `minion_speed` need no `triggers`: they are active while the tier is owned. Triggered effects, which is almost every stock libreforge effect, need a `triggers` list:

```yaml
effects:
  - id: multiply_drops
    triggers:
      - minion_mine_block # Runs each time the minion breaks a block.
    args:
      multiplier: "1 + %tier_value%"
```

For something that should run on a timer, use `minion_static_<ticks>` rather than `minion_tick` with `every`, as it only runs on the ticks it is due. The shipped auto sell upgrade sells every 200 ticks:

```yaml
effects:
  - id: minion_sell_storage
    triggers:
      - minion_static_200 # Runs every 200 ticks.
```

:::info
A tier can have its own `effects` block. It replaces the upgrade's `effects` for that tier only, it is not merged with them.
:::

:::tip
EcoMinions adds its own triggers, effects, conditions and mutators, such as `minion_mine_block`, `minion_speed` and `minion_owner_online`. They are listed with the rest on the [EcoMinions wiki](https://hub.auxilor.io/wiki/ecominions), and only work on minions. Any stock libreforge effect also works with a minion trigger, see [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect).
:::

### Tiers

The ladder players buy up, in order. Tier keys must be whole numbers.

```yaml
tiers:
  1:
    value: 5 # The number %tier_value% resolves to at this tier.
    stat-delta: "-5 ticks" # Menu text describing this tier.
    cost-options: # The ways a player can pay for this tier. They pick one.
      - value: 5000
        type: coins
        display: "&a$%value_commas%"
      - value: 64
        type: stone
        display: "&7%value_commas%x Stone"
    buy-conditions: [] # Optional. Conditions that must pass to buy this tier.
```

`value` has no meaning of its own. It is what `%tier_value%` resolves to, so it is a tick count, a multiplier or a flag depending on how the effects use it.

Each entry in `cost-options` is a price, so `type` can be `coins`, an item, or any other price type. When there is more than one, the player picks which to pay with. `display` is **required** on every entry, and `%value%` and `%value_commas%` work inside it.

`buy-conditions` are checked before the price. Use them to lock a tier behind a minion level, another upgrade, or anything else a condition can check.

:::warning
A cost option with no `display` breaks the upgrade menu when it tries to show that price.
:::

## Internal placeholders

These work in the upgrade menus in `lang.yml`.

| Placeholder | Value |
| --- | --- |
| `%upgrade_name%` | The upgrade's `name`. |
| `%tier%` | The tier the minion owns. |
| `%max_tier%` | The highest tier. |
| `%delta%` | The `stat-delta` of the tier being shown. |
| `%value%` | A cost option's amount. |
| `%value_commas%` | A cost option's amount, with commas. |

:::tip Troubleshooting
- **Upgrade not in the menu?** Add its ID to the minion's `allowed-upgrades`.
- **Upgrade bought but doing nothing?** A triggered effect needs a `triggers` list, and a minion-only effect needs a minion trigger.
- **Upgrade menu not opening?** Check every cost option has a `display`.
:::

<hr/>

## Where to go next

- **Minions:** expose the upgrade with `allowed-upgrades` in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Compacting:** compactor recipes make their own upgrades, see [How to Make a Compactor Recipe](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-compactor-recipe).
- **Levelling:** lock tiers behind minion levels with [How to Level a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-level-a-minion).
- **Defaults:** the shipped upgrades live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/upgrades).
