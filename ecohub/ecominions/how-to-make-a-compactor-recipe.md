---
title: "How to Make a Compactor Recipe"
sidebar_position: 4
---

## Compactor recipes

Compacting turns bulk drops into their block form (nine coal into a coal block, and so on). Each recipe is a
YAML file in `compactor-recipes/`, shared across minions the same way a fuel or an upgrade is - the file
name (without `.yml`) is the recipe's catalog id. An `_example.yml` is provided in the folder to copy from.

Unlike other upgrades, a compactor recipe is not written by hand as a full `upgrades/*.yml` file - loading it
generates a single-tier purchasable automatically, with the recipe's own price, `buy-conditions`, and
`conditions`.

## Common config

```yaml
name: "&8Coal Compactor"
display:
  icon: coal_block

input: COAL
input-amount: 9
output: COAL_BLOCK
output-amount: 1

cost-options:
  - value: 15000
    type: coins
    display: "&a$%value_commas%"
```

- `name` / `display.icon` - the recipe's own display name and menu icon, same as an upgrade's.
- `input` / `input-amount` - the eco item lookup consumed, and how many are consumed per compaction.
- `output` / `output-amount` - the eco item lookup produced, and how many are produced per compaction.
- `cost-options` - one or more ways a player can pay to unlock this recipe, same format as an upgrade tier's
  `cost-options` (see [How to Make an Upgrade](how-to-make-an-upgrade)) - each entry needs its own `display`
  string or the upgrade menu throws trying to render that price.

**`output` must never resolve to the same item as `input`, and `input-amount` must exceed `output-amount`.**
The compact loop repeatedly consumes `input-amount` and produces `output-amount` while enough `input` remains
in storage, so a recipe that feeds itself (or that doesn't shrink the stack) will churn or destroy a player's
items rather than compact them.

## buy-conditions and conditions

Two optional, separate gates - each a list of libreforge conditions (see
[Libreforge Components](libreforge-components) for the available ones):

```yaml
buy-conditions:
  - id: minion_has_upgrade
    args:
      upgrade: compactor_coal_block

conditions:
  - id: minion_level_above
    args:
      level: 5
```

- `buy-conditions` - must pass for a player to buy this recipe at all, checked before affordability. Use this
  to require another recipe be bought first (with `minion_has_upgrade`, referencing its `compactor_<id>`
  upgrade id), gate a recipe behind a permission, or anything else a condition can express.
- `conditions` - must pass every time a minion tries to compact for this recipe to actually run. Unlike
  `buy-conditions`, this is re-checked continuously - a minion can own the recipe and still have it do nothing
  if the condition isn't currently met.

Both default to empty (no gate) if omitted.

## higher-level-of

```yaml
higher-level-of: coal_block
```

An optional ordering hint: if this recipe's `input` is another recipe's `output` - e.g. an `enchanted_coal`
recipe consuming `COAL_BLOCK`, where `coal_block` is the recipe that produces `COAL_BLOCK` from `COAL` - set
`higher-level-of` to that lower recipe's id, so the compactor processes `coal -> coal_block` before
`coal_block -> enchanted_coal`. This is not required for correctness (compacting repeats until nothing more
changes, so a chain converges regardless of order over multiple passes) but keeps a linear chain predictable.

It does **not** gate purchase or ownership on its own - pair it with a `buy-conditions: minion_has_upgrade`
entry (as shown above) if a recipe should require the lower one to be bought first.

## Letting a minion use a recipe

A recipe in the catalog does nothing on its own - a minion has to opt in with two top-level keys in its
`minions/<type>.yml`:

```yaml
compactor-recipes:
  - coal_block
  - iron_block

effects:
  - id: minion_compact
    triggers:
      - minion_mine_block
```

- `compactor-recipes` - the list of catalog ids this type allows. The order here is just menu display order -
  each recipe is bought and gated independently, there is no shared tier or level.
- `effects` - the type also needs `minion_compact` in its own top-level `effects:` block, or nothing ever
  dispatches compacting. This is different from every other upgrade, where the upgrade file itself owns the
  `effects:` block that fires it.

Referencing a catalog id that doesn't exist is dropped (with a violation reported at startup); every other id
in the list still resolves normally.

The resulting upgrade's id is always `compactor_<recipe id>` - use that id from `minion_has_upgrade`,
elsewhere in this config or another recipe's `buy-conditions`.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the `compactor-recipes` and `effects`
  keys that expose a recipe to a minion.
- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) for hand-authored, non-compactor
  upgrades.
