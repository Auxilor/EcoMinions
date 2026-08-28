---
title: "How to Make an Upgrade"
sidebar_position: 3
---

## Upgrades

Each upgrade is a YAML file in `upgrades/`. The file name (without `.yml`) is the upgrade ID, and is what
a minion's `allowed-upgrades` list refers to. An `_example.yml` is provided in the folder to copy from.

## Common Config

```yaml
name: "&bSpeed"
display:
  icon: sugar

effects:
  - id: minion_speed
    args:
      ticks_faster: "%tier_value%"

tiers:
  1:
    value: 5
    stat-delta: "-5 ticks"
    cost-options:
      - value: 5000
        type: vault
        display: "&a$%value_commas%"
      - value: 64
        type: stone
        display: "&7%value_commas%x Stone"
  2:
    value: 10
    stat-delta: "-10 ticks"
    cost-options:
      - value: 12000
        type: vault
        display: "&a$%value_commas%"
      - value: 64
        type: coal
        display: "&8%value_commas%x Coal"
```

- `name` - the upgrade's display name, shown in the upgrade menu.
- `display.icon` - an eco item lookup used as the upgrade's icon in the menu.
- `effects` - a list of libreforge effects, authored **once** for the whole upgrade and shared by every tier.
  There is no separate `effect:` key and no closed list of stat names - an upgrade is just a holder of these
  effects, enabled at whatever tier the owning minion currently has purchased.
- `tiers` - a map of tier level to that tier's data. Levels must be whole numbers and are purchased in order.
  - `value` - the tier's raw number. It has **no fixed meaning** of its own; it only matters because
    `%tier_value%` resolves to it inside `effects`. What it represents - a tick count, a percentage, a flag -
    is entirely up to how the effect args use it.
  - `stat-delta` - display-only text describing the tier's effect, shown in the upgrade menu (e.g. `"-5 ticks"`,
    `"+10% drops"`, `"Enabled"`).
  - `cost-options` - one or more ways a player can pay for this tier; the player picks one at purchase time.
    Each entry has:
    - `value` (amount) and `type` - `type` can be `vault` for Vault economy money, a material name to charge
      an item, or another registered currency id (e.g. a custom currency plugin's `gems`).
    - `display` (**required**) - the text shown for this price option in the upgrade menu, e.g. `"&a$%value_commas%"`.
      `%value%` and `%value_commas%` (comma-grouped) are available inside it. Omitting `display` isn't just
      cosmetic - eco has no fallback for it, so the menu throws trying to render that cost option.
  - `buy-conditions` (optional) - a list of libreforge conditions that must pass for a player to buy this tier,
    checked before affordability. Absent or empty means anyone who can afford it can buy it (today's behavior
    for every upgrade). See [Libreforge Components](libreforge-components) for the available conditions.

Write each cost option as its own list item across multiple lines (`- value: ...` / `type: ...` /
`display: ...`), not the inline `{ value: ..., type: ... }` flow-map form - it's harder to scan and easy to
mistype once a `display` line is added.

### effects and placeholders

Every entry under `effects` is a libreforge effect block: an `id`, optional `args`, and (for anything that
isn't permanent) a `triggers` list telling it when to fire.

Inside `args`, two placeholders are available everywhere:

- `%tier_value%` - the `value` of whichever tier the minion currently owns.
- `%tier%` - the tier number itself.

**Permanent effects** - ones that just hold a stat open, like `minion_speed` - need no `triggers`. They enable
when the tier is purchased and disable if the upgrade is removed:

```yaml
effects:
  - id: minion_speed
    args:
      ticks_faster: "%tier_value%"
```

**Triggered effects** need a `triggers` list, because without one there is nothing telling libreforge when to
run them. This applies to almost every stock libreforge effect, since most of them are one-shot actions rather
than permanent stat holders. `multiply_drops` (used for a fortune-style upgrade) only makes sense reacting to
a mine event:

```yaml
effects:
  - id: multiply_drops
    triggers:
      - minion_mine_block
    args:
      multiplier: "1 + %tier_value%"
```

### Interval behaviour

Use `minion_static_<n>` instead of `minion_tick` for anything that should fire every `n` ticks rather than
every single tick - it skips dispatch entirely on the ticks it doesn't fire, unlike `minion_tick` + `every:`,
which dispatches every tick and discards most of it at the effect layer. The auto-sell upgrade uses this to
sell the minion's storage roughly every 200 ticks:

```yaml
effects:
  - id: minion_sell_storage
    triggers:
      - minion_static_200
```

`n` can also be an expression (e.g. a placeholder), re-evaluated on every check.

On/off upgrades such as `auto_smelt` and `auto_sell` typically only need a single tier, since the effect is
either present or not. Numeric upgrades such as `speed`, `efficiency`, `radius`, `height`, `fortune`, `capacity`, and
`experience` are usually laddered across several tiers, each with its own cost and its own `%tier_value%`.

Compacting is not authored as a hand-written upgrade file at all - each compactor recipe generates its own
single-tier purchasable from the `compactor-recipes/` catalog. See
[How to Make a Compactor Recipe](how-to-make-a-compactor-recipe).

See [Libreforge Components](libreforge-components) for the full list of minion-specific triggers, effects,
conditions, and mutators available inside `effects`, plus the stock libreforge components that also work here.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the `allowed-upgrades` list that exposes
  an upgrade to a minion.
- **Build a fuel:** [How to Make a Fuel](how-to-make-a-fuel) to keep minions active.
