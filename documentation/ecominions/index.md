---
title: "EcoMinions"
---

## What is EcoMinions?

EcoMinions adds placeable mining minions to your server. Players place a minion item in the world, and the minion
mines a configured set of blocks into its own storage for the owner to collect, sell, or process automatically.

## Features

- **Placeable minions** - spawn a minion from an item, each type mining its own set of target blocks
- **Per-minion storage** - mined items collect into the minion's own inventory rather than the world
- **Idle while offline** - a minion only mines while its owner is online; when the owner logs back in, the
  offline span is credited in one go
- **Upgrades** - each upgrade is a config-authored libreforge effect holder, purchased tier by tier; the
  shipped set includes speed, efficiency, radius, fortune, capacity, auto-smelt, auto-sell, and experience,
  but any libreforge effect can be wired up as an upgrade
- **Compacting** - a per-minion-type tier ladder that compresses bulk drops into their block form; a minion
  owns every tier it buys but only one runs at a time, and the player can switch back to a lower owned tier
- **Fuel** - minions consume fuel to stay active, with different fuel items burning for different durations
- **Item lookup integration** - every minion's spawn item is available to any eco item lookup as `ecominions:<id>`

## How does it work?

A minion is placed from an item and appears as an armor stand that mines nearby target blocks into its own
storage. While its owner is online and its chunk is loaded, it mines on a timer, following the `speed-ticks` and
`efficiency-ticks` set for its type and level.

When the owner logs out, or the minion's chunk unloads, the minion goes idle: it stops animating, stops mining,
and stops emitting its full-storage particles. When the owner logs back in, the elapsed absence is credited in
one go - cycles are simulated for the time that passed (scaled by `unloaded-calculation.offline-efficiency`),
fuel is consumed for that simulated span rather than the whole wall-clock absence, and the resulting drops are
credited using each mined block's real drop table and passed through the minion's own effects in a single
batch, so upgrades like fortune and auto-smelt still apply to offline mining. Mining then resumes live.

Upgrades, fuels, and compactor tiers extend what a minion can do - see the how-to pages below. Every minion type's
spawn item is also registered with eco's item lookup system as `ecominions:<id>`, so shops, crates, and other
plugins that consume eco item lookups can reference or hand out a minion item directly.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) walks through a full minion type config.
- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) covers upgrade tiers and costs.
- **Build a compactor tier:** [How to Make a Compactor Tier](how-to-make-a-compactor-tier) covers the tier
  catalog and how a minion type unlocks it.
- **Build a fuel:** [How to Make a Fuel](how-to-make-a-fuel) covers fuel items and burn duration.
- **Plugin config:** [Plugin Config](plugin-config) covers the plugin-wide settings.
- **Commands:** [Commands and Permissions](commands-and-permissions) lists every command and node.
