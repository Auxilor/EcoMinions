---
title: "How to Make a Compactor Tier"
sidebar_position: 4
---

## Compactor tiers

Compacting turns bulk drops into their block form (nine coal into a coal block, and so on). Unlike other
upgrades, it is not authored as a single `upgrades/*.yml` file - it is built from a shared catalog of tiers in
`compactor-tiers/`, and each minion type picks its own ordered subset of that catalog to unlock. The file name
(without `.yml`) is the tier's catalog id. An `_example.yml` is provided in the folder to copy from.

## Common config

```yaml
cost-options:
  - { value: 100000, type: vault }
  - { value: 500, type: gems }

recipes:
  coal_block:
    input: COAL
    input-amount: 9
    output: COAL_BLOCK
    output-amount: 1
  iron_block:
    input: IRON_INGOT
    input-amount: 9
    output: IRON_BLOCK
    output-amount: 1
```

- `cost-options` - one or more ways a player can pay to unlock this tier, same format as an upgrade tier's
  `cost-options` (see [How to Make an Upgrade](how-to-make-an-upgrade)).
- `recipes` - a map of recipe id to recipe. Each recipe:
  - `input` - the eco item lookup consumed.
  - `input-amount` - how many of `input` are consumed per compaction.
  - `output` - the eco item lookup produced.
  - `output-amount` - how many of `output` are produced per compaction.

**`output` must never resolve to the same item as `input`, and `input-amount` must exceed `output-amount`.**
The compact loop repeatedly consumes `input-amount` and produces `output-amount` while enough `input` remains
in storage, so a recipe that feeds itself (or that doesn't shrink the stack) will churn or destroy a player's
items rather than compact them.

## Letting a minion type use a tier

A tier in the catalog does nothing on its own - a minion type has to opt in with two top-level keys in its
`minions/<type>.yml`:

```yaml
compactor-tiers:
  - basic_block
  - rare_block

effects:
  - id: minion_compact
    triggers: [minion_mine_block]
```

- `compactor-tiers` - an ordered list of catalog ids this type unlocks. **Position in the list is the tier
  level** - `basic_block` above is tier 1, `rare_block` is tier 2 - so two minion types can share a catalog
  entry while unlocking it at different levels, or skip it entirely. Different minion types resolve their own
  independent ladder from the same catalog this way.
- `effects` - the type also needs `minion_compact` in its own top-level `effects:` block, or nothing ever
  dispatches the compaction. This is different from every other upgrade, where the upgrade file itself owns
  the `effects:` block that fires it.

Referencing a catalog id that doesn't exist truncates the ladder at that point (everything before the bad id
still resolves; the bad id and everything after it don't), and the startup report says so.

The resulting upgrade's id is always `compactor_<minion type id>` - use that id if you reference it from
`minion_has_upgrade` in some other config.

## Tiers stack, and selecting one

Compactor tiers are **selectable**: a minion owns every tier it has bought, but only one tier is *active* at a
time, and a player can switch back to any owned tier by clicking it in the upgrade menu (buying a new tier
activates it automatically). A minion running tier *N* applies every recipe from tiers `1..N`, not just tier
*N*'s own recipes - so a later tier should describe what it *adds*, not repeat what an earlier tier already
compacts.

Use `minion_has_upgrade`'s `mode` argument to tell "unlocked" apart from "currently running" elsewhere in a
config - see [Libreforge Components](libreforge-components).

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the `compactor-tiers` and `effects` keys
  that expose a ladder to a minion type.
- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) for hand-authored, non-compactor
  upgrades.
