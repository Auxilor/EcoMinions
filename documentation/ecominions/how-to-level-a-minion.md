---
title: "How to Level a Minion"
sidebar_position: 5
---

## Levelling

Levelling is an opt-in `levelling:` block nested inside a minion type's YAML file (see
[How to Make a Minion](how-to-make-a-minion)). With the block absent, or `enabled: false`, a minion's level is
just the sum of its purchased upgrade tiers plus one - the original, non-levelling behaviour. With levelling
turned on, a minion instead earns XP toward explicit levels, independently of any upgrades it owns.

## Common Config

```yaml
levelling:
  enabled: true
  xp-formula: "100 * %level% ^ 1.5"
  # or an explicit ladder - the first entry is the cost of reaching level 2:
  # xp-requirements: [100, 250, 500]
  max-level: 50
  xp-gain-methods:
    - trigger: minion_mine_block
      multiplier: 1
    - trigger: minion_sell
      multiplier: 0.05
  conditions:
    - id: minion_owner_online
  level-up-effects:
    - id: send_message
      conditions: [{ id: minion_owner_online }]
      args:
        message: "&aYour minion reached level %level%!"
```

- `enabled` - whether this minion type levels via XP at all.
- `xp-formula` / `xp-requirements` - the level curve.
- `max-level` - the highest level the curve can reach.
- `xp-gain-methods` - what grants XP, and how much.
- `conditions` - a spec-level gate on whether XP can be gained at all.
- `level-up-effects` - libreforge effects run the moment a minion gains a level.

## enabled

If `enabled` is missing or `false`, the whole `levelling:` block is ignored and the minion falls back to the
original behaviour: level is the sum of purchased upgrade tiers, plus one. This also happens automatically if
`enabled: true` is set but neither `xp-formula` nor `xp-requirements` is configured - a levelling block that
could never grant a level is treated as broken and degraded to disabled, rather than rejecting the whole minion
type file. A minion typo in `levelling:` never deletes a working minion.

## xp-formula vs xp-requirements

These are two different ways to describe the level curve, and only one takes effect - if both are set,
`xp-formula` wins.

- `xp-formula` - an expression evaluated once per level-up, in terms of `%level%`. `%level%` is **the level
  being reached**, not the minion's current level, so `"100 * %level% ^ 1.5"` is the cost of going from level
  `%level% - 1` to `%level%`. With a formula, `max-level` is unbounded unless you set it explicitly - a minion
  can keep levelling forever.
- `xp-requirements` - an explicit list of XP costs, one per level transition. The **first entry is the cost of
  reaching level 2** (minions start at level 1, not 0), the second is the cost of reaching level 3, and so on.
  Running off the end of the list ends progression at that level, unless `max-level` says otherwise - see below.

```yaml
xp-requirements: [100, 250, 500]
```

reaches level 2 at 100 XP, level 3 at 250 more XP, and level 4 at 500 more XP after that.

## max-level

`max-level` caps the highest level a minion of this type can reach.

- With `xp-requirements`, it defaults to the length of the list plus one (i.e. the level reachable by the last
  entry). Setting it lower stops progression early; setting it higher than the list allows is clamped back down
  to that natural ceiling and reported as a config violation, since XP would otherwise keep banking with no way
  to ever reach the configured cap.
- With `xp-formula`, it defaults to unbounded - omit it and a minion can level indefinitely, evaluating the
  formula for however high `%level%` climbs.

## xp-gain-methods

Each entry in `xp-gain-methods` is a rule for granting XP in response to a libreforge trigger:

```yaml
xp-gain-methods:
  - trigger: minion_mine_block
    multiplier: 1
  - trigger: minion_sell
    value: 5
    filters:
      blocks: [DIAMOND_ORE]
    conditions:
      - id: minion_level_above
        args:
          level: 5
    args:
      amount: 1
```

- `trigger` - the id of a libreforge/EcoMinions trigger (e.g. `minion_mine_block`, `minion_sell`,
  `minion_tick`). See [Libreforge Components](libreforge-components) for the full list. Required - a method
  with no trigger is dropped and logged as a config violation.
- `multiplier` - an expression multiplying the trigger's own `value` (e.g. blocks broken, money earned) to get
  the XP granted. Cannot be combined with `value`.
- `value` - an expression giving the XP granted directly, ignoring the trigger's own `value`. Cannot be combined
  with `multiplier`. Omitting both grants the trigger's raw `value` unmodified.
- `filters` - the same filter block stock libreforge effects accept (`blocks`, `items`, `entities`, and so on),
  restricting which trigger firings this method reacts to.
- `conditions` - a libreforge condition list; the method only grants XP when these pass.
- `args` - arbitrary libreforge effect arguments, evaluated the same way an effect's `args` are (including
  argument-level conditions such as `if-condition`).

## conditions

The spec-level `conditions` block gates XP gain for the entire minion, independent of any per-method
`conditions`. It's compiled the same way any libreforge condition list is - if it fails, none of the minion's
`xp-gain-methods` grant XP, regardless of what they individually allow.

## level-up-effects

`level-up-effects` is a libreforge effect chain that runs once, immediately, whenever the minion crosses into a
new level. It uses the same effect blocks as a minion type's `effects:` or an upgrade's `effects:` (see
[How to Make an Upgrade](how-to-make-an-upgrade)), and can additionally react to the `minion_level_up` trigger
(see [Libreforge Components](libreforge-components)) if you want an effect elsewhere in `effects:` to also fire
on level-up.

**Owner caveat:** a minion can level up while its owner is completely offline - XP gain from `minion_tick` or
from an idle minion's simulated mining doesn't wait for a player to be online. Because of that, any
`level-up-effects` entry that targets the trigger's player - `send_message`, `give_money`, and similar - will
silently do nothing on an offline level-up, since there is no player to act on. Gate these with the
`minion_owner_online` condition, as in the example above, or use the `minion_owner_as_dispatcher` mutator to
retarget the chain explicitly.

**Built-in notification:** independent of `level-up-effects`, EcoMinions sends the owner (if online, same
offline no-op as above) the `messages.level-up` lang string on every level gained, with `%minion_name%` and
`%level%` resolved. Edit that key in `lang.yml` to change or silence it; it isn't something you configure per
minion type.

## Placeholders

These placeholders resolve anywhere minion placeholders already work - holograms, the menu title, spawn-item
lore - for any minion, levelling or not (non-levelling minions read as if they have no levelling data).

| Placeholder | Resolves to |
| --- | --- |
| `%xp%` | The minion's current XP, as a whole number. |
| `%xp_required%` | XP needed to reach the next level, or an "infinity" placeholder text if there is no next level (max level reached, or levelling not enabled/configured). |
| `%progress%` | Progress toward the next level as a percentage (0-100), or `100` if there is no next level. |
| `%max_level%` | The minion type's configured max level. |
| `%level_numeral%` | The minion's current level, rendered as a Roman numeral. |

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the `levelling:` block's place in a
  minion type file, and for `base-stats` expressions that read `%level%`.
- **Effects, triggers, and conditions:** [Libreforge Components](libreforge-components) for `minion_level_up`,
  `give_minion_xp`, and everything else available inside `xp-gain-methods` and `level-up-effects`.
