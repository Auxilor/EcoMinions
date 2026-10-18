---
title: "How to Level a Minion"
sidebar_position: 5
---

**Levelling** lets a minion earn XP and level up as it works. It is an optional `levelling` block in a minion's config that sets the **level curve**, what **earns XP**, and what happens on **level up**. Without it, a minion's level is the number of upgrade tiers it owns, plus one. This page covers setting it up from scratch.

## Quick start

1. Open a minion's config in `/plugins/EcoMinions/minions/`.
2. Add a `levelling` block with `enabled: true`.
3. Set the curve with `xp-formula` or `xp-requirements`, and optionally `max-level`.
4. Add at least one entry to `xp-gain-methods`, e.g. `minion_mine_block`.
5. Run `/minions reload`, place the minion, and confirm `%xp%` rises in its menu as it works.

:::tip
The shipped `minions/_example.yml` has a commented `levelling` block to copy from. Stats in `base-stats` can use `%level%`, so a levelling minion can get faster or bigger as it levels.
:::

## Naming and IDs

Levelling has no file or ID of its own. It belongs to the minion whose config it is in, and every minion type can have its own curve. `xp-gain-methods` use trigger IDs, and `conditions` and `level-up-effects` use the [libreforge](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect) format.

:::warning
A `levelling` block with `enabled: true` but no `xp-formula` or `xp-requirements` is reported at startup and treated as disabled. The rest of the minion still loads.
:::

## The structure of levelling

| Part | What it controls |
| --- | --- |
| **Curve** | How much XP each level costs, and the highest level |
| **XP gain** | Which triggers give XP, and how much |
| **Level up** | Effects that run when the minion levels up |

```yaml
levelling:
  # === Curve: XP per level ===
  enabled: true # Whether this minion levels with XP.
  xp-formula: "100 * %level% ^ 1.5" # The XP to reach %level%. Use this or xp-requirements.
  xp-requirements: [] # The XP to reach each level, starting at level 2. Use this or xp-formula.
  max-level: 50 # The highest level.

  # === XP gain: what earns XP ===
  xp-gain-methods: # Each entry gives XP when its trigger fires.
    - trigger: minion_mine_block
      multiplier: 1
    - trigger: minion_sell
      multiplier: 0.05
  conditions: [] # Optional. Conditions that must pass for the minion to gain any XP.

  # === Level up: what happens on level up ===
  level-up-effects: # Effects run each time the minion gains a level.
    - id: send_message
      conditions:
        - id: minion_owner_online
      args:
        message: "&aYour minion reached level %level%!"
```

### Curve

How much XP each level costs, and the highest level.

```yaml
enabled: true # Whether this minion levels with XP.
xp-formula: "100 * %level% ^ 1.5" # The XP to reach %level%. Use this or xp-requirements.
xp-requirements: [] # The XP to reach each level, starting at level 2. Use this or xp-formula.
max-level: 50 # The highest level.
```

Minions start at level 1. Set one of the two curves; if both are set, `xp-formula` is used.

- `xp-formula`: an expression for the XP needed to reach a level, where `%level%` is the level being reached. With a formula, there is no maximum unless `max-level` is set.
- `xp-requirements`: a list of XP costs. The first entry is the cost of reaching level 2, so `[100, 250, 500]` reaches level 4 at most. `max-level` defaults to the end of the list, and a higher value is lowered to it and reported.

### XP gain

Which triggers give XP, and how much. Any trigger works, and the [EcoMinions wiki](https://hub.auxilor.io/wiki/ecominions) lists the minion triggers.

```yaml
xp-gain-methods:
  - trigger: minion_mine_block # The trigger that gives XP. Required.
    multiplier: 1 # Optional. XP is the trigger's value times this.
  - trigger: minion_sell
    value: 5 # Optional. A fixed amount of XP, ignoring the trigger's value.
    filters: # Optional. Only count trigger firings that match.
      blocks:
        - diamond_ore
    conditions: # Optional. Only give XP while these pass.
      - id: minion_level_above
        args:
          level: 5
conditions: [] # Optional. Conditions that must pass for the minion to gain any XP.
```

With neither `multiplier` nor `value`, the trigger's own value is given as XP, such as the blocks broken or the money earned. `multiplier` and `value` cannot be used together. `filters` work the same as on an effect.

The top-level `conditions` apply to every method: when they fail, the minion gains no XP at all.

:::tip
To give XP from anything else, such as a fuel or an upgrade, use the `give_minion_xp` effect.
:::

### Level up

Effects that run each time the minion gains a level. If one XP gain crosses several levels, they run once per level.

```yaml
level-up-effects: # Effects run each time the minion gains a level.
  - id: send_message
    conditions:
      - id: minion_owner_online
    args:
      message: "&aYour minion reached level %level%!"
```

These placeholders describe the level up that just happened:

| Placeholder | Value |
| --- | --- |
| `%level%` | The level just reached. |
| `%level_numeral%` | The level just reached, in Roman numerals. |
| `%previous_level%` | The level before. |
| `%previous_level_numeral%` | The level before, in Roman numerals. |

:::warning Offline owners
A minion can level up while its owner is offline. Effects that act on the player, like `send_message` or `give_money`, do nothing then, as there is no player. Guard them with the `minion_owner_online` condition, as above, or move the chain onto the owner with the `minion_owner_as_dispatcher` mutator.
:::

:::info
EcoMinions also sends the owner the `messages.level-up` message from `lang.yml` on every level up, if they are online. Edit or empty it there.
:::

## Internal placeholders

These work anywhere minion placeholders work, such as holograms, menus and the spawn item. A minion without levelling reads as having no levelling data.

| Placeholder | Value |
| --- | --- |
| `%xp%` | The minion's current XP, as a whole number. |
| `%xp_required%` | The XP needed for the next level, or `infinity` from `lang.yml` at the max level or without levelling. |
| `%progress%` | Progress to the next level, from 0 to 100. `100` at the max level. |
| `%max_level%` | The highest level. |
| `%level_numeral%` | The minion's level, in Roman numerals. |
| `%level_<offset>%` | The minion's level plus an offset, e.g. `%level_1%` for the next level or `%level_-1%` for the last. |
| `%level_<offset>_numeral%` | The same, in Roman numerals. |

:::tip Troubleshooting
- **Minion not gaining XP?** Check `enabled: true`, a curve is set, and the top-level `conditions` pass.
- **Level-up message not arriving?** The owner was offline. Add a `minion_owner_online` condition so this is clear in the config.
- **Stuck below `max-level`?** With `xp-requirements`, the list ends before it. Add more entries.
:::

<hr/>

## Where to go next

- **Minions:** scale `base-stats` with `%level%` in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Upgrades:** lock tiers behind levels with `buy-conditions` in [How to Make an Upgrade](https://hub.auxilor.io/wiki/ecominions/how-to-make-an-upgrade).
- **Defaults:** the shipped minions live [here](https://github.com/Auxilor/EcoMinions/tree/master/eco-core/core-plugin/src/main/resources/minions).
