---
title: "Libreforge Components"
sidebar_position: 7
---

## Libreforge Components

EcoMinions upgrades and minion type `effects:` blocks are built on [libreforge](https://libreforge.wiki/) - the
same effect/trigger/condition/mutator system used across eco plugins. This page lists every minion-specific
component EcoMinions registers, plus the stock libreforge components that are known to work well on minions.

See [How to Make an Upgrade](how-to-make-an-upgrade) and [How to Make a Minion](how-to-make-a-minion) for how
`effects:` blocks are authored in practice.

## Triggers

Triggers decide *when* a triggered effect runs. Pass one or more in an effect's `triggers:` list.

| Trigger | Fires when | Parameters |
| --- | --- | --- |
| `minion_mine_block` | A minion breaks a target block, before its drops are stored. | `player` (the owning player, if online), `block` (the block the minion broke), `event` (an editable drop event over the block's drops), `location`, `item`, `value` (how many blocks this action broke - more than one for stacked plants) |
| `minion_storage_full` | A minion tries to mine but its storage is full. | `player`, `location` |
| `minion_regen_block` | A block a minion mined regenerates. | `player`, `block` (the block that regenerated), `location` |
| `minion_tick` | Every tick an active minion runs. Pair with `every:` for interval behaviour - it fires very frequently, so always constrain it with `every:` or a condition. | `player`, `location` |
| `minion_place` | A minion is placed in the world. | `player`, `location` |
| `minion_pickup` | A minion is picked up out of the world. | `player`, `location` |
| `minion_upgrade` | A player buys an upgrade tier for a minion. | `player`, `location`, `text` (the id of the upgrade that was bought), `value` (the tier that was bought) |
| `minion_fuel_start` | A minion starts burning a fuel. | `player`, `location`, `text` (the id of the fuel that started burning) |
| `minion_fuel_expire` | The tick a minion's fuel runs out. Only fires for an active minion - an idle minion's fuel is frozen, so it cannot expire while idle. | `player`, `location` |
| `minion_sell` | A minion's storage is sold. | `player`, `location`, `event` (an editable drop event over the stacks being sold), `value` (the money the sale earned) |
| `minion_level_up` | A minion gains a level. Dispatched by EcoMinions itself, not a Bukkit listener - can fire while the owner is offline. | `player` (the owning player, if online), `location`, `value` (the level reached) |

## Effects

Bespoke minion effects, with the argument(s) each takes inside `args:`.

| Effect | Args | What it does |
| --- | --- | --- |
| `minion_speed` | `ticks_faster` (expression) | Reduces the ticks between a minion's mine cycles. Permanent - no `triggers:` needed. |
| `minion_efficiency` | `ticks_faster` (expression) | Reduces the ticks a minion spends breaking a block. Permanent - no `triggers:` needed. |
| `minion_radius` | `blocks` (expression) | Widens the area a minion scans for target blocks. Permanent - no `triggers:` needed. |
| `minion_capacity` | `slots` (expression) | Adds stack slots to a minion's storage, up to the type's `max-storage-slots`. Permanent - no `triggers:` needed. Resizes live storage; any overflow is dropped in the world rather than lost. |
| `minion_sell_storage` | none | Sells the entire contents of a minion's storage. Triggered - needs a `triggers:` list (e.g. `[minion_tick]` with `every:` for an auto-sell upgrade). |
| `minion_compact` | none | Compacts a minion's storage using the configured compactor recipes. Triggered - needs a `triggers:` list. |
| `give_minion_xp` | `amount` (expression) | Grants XP to a minion, letting a fuel or an upgrade tier feed levelling progression directly. Triggered - needs a `triggers:` list. |

`minion_speed`, `minion_efficiency`, `minion_radius`, and `minion_capacity` are permanent stat effects: they
contribute to a stat while their holder (the type or upgrade tier) is active, and remove that contribution the
instant it's disabled. Their `args` value supports full expressions, so `%tier_value%` and arithmetic on it both
work.

## Conditions

| Condition | Args | Passes when |
| --- | --- | --- |
| `minion_level_above` | `level` (expression) | The minion's level is at or above the given level. |
| `minion_level_below` | `level` (expression) | The minion's level is at or below the given level. |
| `minion_is_type` | `types` (string list) | The minion is one of the given minion type ids. |
| `minion_has_upgrade` | `upgrade` (string, required), `tier` (expression, optional - defaults to `1`) | The minion owns the given upgrade at or above the specified tier. |
| `minion_storage_above_percent` | `percent` (expression, 0-100) | The minion's storage is at least the given percentage full. |
| `minion_has_fuel` | none | The minion is currently burning fuel. |
| `minion_owner_online` | none | The minion's owner is online. |

## Mutators

Mutators rewrite the trigger data flowing through an effect chain.

| Mutator | What it does |
| --- | --- |
| `minion_owner_as_dispatcher` | Makes the minion's owner the dispatcher for the rest of the chain, adding a `player` parameter. Lets an admin say "run the rest of this on the player" explicitly, rather than relying on the trigger data already carrying a player. |
| `minion_location_to_target_block` | Sets the chain's location to the block the minion is acting on. |

## Stock components that work on minions

Minion drops are routed through a real libreforge drop event - that's what the `event` parameter on
`minion_mine_block` and `minion_sell` is. Because of that, stock libreforge components that operate on drop
events work on minions exactly as they would on a player's own mining, including:

- `multiply_drops` - multiplies the quantity of items dropped (used for fortune-style upgrades).
- `autosmelt` - smelts drops into their furnace output before they're stored.
- `drop_xp` - grants XP for the drop.
- `cancel_drops` - cancels the drop entirely.
- `telekinesis` - sends drops straight to storage/inventory instead of the ground.
- The `blocks` / `items` / `entities` filter arguments many stock effects accept, to restrict which blocks,
  items, or entities an effect applies to.

Any of these can be dropped straight into an upgrade's or a minion type's `effects:` list alongside the
minion-specific effects above, with a `triggers:` list of `[minion_mine_block]` (or `[minion_sell]` for
sell-time effects).

## The owner caveat

Most minion triggers carry a `player` parameter, but it's only populated while the minion's owner is online -
an idle minion still fires triggers like `minion_tick`, but with a null player. Any stock effect that targets
the trigger's player - `send_message`, `give_money`, and similar - silently does nothing while the owner is
offline, because there is no player for it to act on.

If an effect needs to reach the owner regardless, gate it with the `minion_owner_online` condition so it's
clear the effect is owner-dependent, or use the `minion_owner_as_dispatcher` mutator to retarget the chain at
the owner explicitly.

This caveat applies just as much to a minion type's `level-up-effects` (see
[How to Level a Minion](how-to-level-a-minion)) - a minion can level up while its owner is offline, so an
owner-targeting effect there needs the same `minion_owner_online` guard.
