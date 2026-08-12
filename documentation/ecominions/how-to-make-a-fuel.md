---
title: "How to Make a Fuel"
sidebar_position: 2
---

## Fuels

Each fuel is a YAML file in `fuels/`. The file name (without `.yml`) is the fuel ID. An `_example.yml` is
provided in the folder to copy from.

## Common Config

```yaml
name: "&8Coal"
item: COAL
duration-ticks: 12000
speed-multiplier: 1.25
```

- `name` - the fuel's display name.
- `item` - the eco item lookup consumed as fuel (e.g. `COAL`, `BLAZE_ROD`).
- `duration-ticks` - how many ticks one unit of this fuel keeps a minion active for. Defaults to `12000` if
  omitted.
- `speed-multiplier` - multiplies the minion's mining speed while this fuel is burning. Defaults to `1.25` if
  omitted.

Fuel only burns while the minion is active - a minion that's idle (its owner offline, or its chunk unloaded)
does not consume fuel. On catching up after an owner returns, fuel is consumed proportionally to the simulated
offline span rather than the full wall-clock time the owner was away.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the full minion type config.
- **Build an upgrade:** [How to Make an Upgrade](how-to-make-an-upgrade) for speed and efficiency upgrades.
