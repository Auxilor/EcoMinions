---
title: "How to Make an Upgrade"
---

## Upgrades

Each upgrade is a YAML file in `upgrades/`. The file name (without `.yml`) is the upgrade ID, and is what
a minion type's `allowed-upgrades` list refers to. An `_example.yml` is provided in the folder to copy from.

## Common Config

```yaml
name: "&eSpeed"
effect: speed
display:
  icon: sugar
tiers:
  1:
    value: 5
    stat-delta: "-5 ticks"
    cost-options:
      - { value: 5000, type: vault }
      - { value: 64, type: stone }
  2:
    value: 10
    stat-delta: "-10 ticks"
    cost-options:
      - { value: 12000, type: vault }
      - { value: 64, type: coal }
```

- `name` - the upgrade's display name, shown in the upgrade menu.
- `effect` - which stat this upgrade modifies. Must be one of: `speed`, `efficiency`, `fortune`, `radius`,
  `capacity`, `auto_smelt`, `auto_sell`, `experience`, `compactor`.
- `display.icon` - an eco item lookup used as the upgrade's icon in the menu.
- `tiers` - a map of tier level to that tier's data. Levels must be whole numbers and are purchased in order.
  - `value` - the effect's magnitude at this tier (meaning depends on `effect` - e.g. a tick reduction for
    `speed`, a drop percentage for `fortune`, `1` to mean "enabled" for on/off effects like `auto_sell`).
  - `stat-delta` - display-only text describing the tier's effect, shown in the upgrade menu (e.g. `"-5 ticks"`,
    `"+10% drops"`, `"Enabled"`).
  - `cost-options` - one or more ways a player can pay for this tier; the player picks one at purchase time.
    Each entry has a `value` (amount) and a `type`. `type` can be `vault` for Vault economy money, a material
    name to charge an item, or another registered currency id (e.g. a custom currency plugin's `gems`).

On/off effects such as `auto_smelt`, `auto_sell`, and `compactor` typically only need a single tier.
Numeric effects such as `speed`, `efficiency`, `radius`, `fortune`, `capacity`, and `experience` are usually
laddered across several tiers, each with its own cost.

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) for the `allowed-upgrades` list that exposes
  an upgrade to a minion type.
- **Build a fuel:** [How to Make a Fuel](how-to-make-a-fuel) to keep minions active.
