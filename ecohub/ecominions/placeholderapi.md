---
title: "PlaceholderAPI"
sidebar_position: 6
---

EcoMinions exposes placeholders you can use anywhere PlaceholderAPI is read, e.g. scoreboards, holograms, and menus. In every placeholder below, `<minion>` is a minion ID (its file name).

## Placeholders

| Placeholder | Description |
| --- | --- |
| `%ecominions_count%` | The amount of minions a player has placed |
| `%ecominions_count_<minion>%` | The amount of a given minion a player has placed |
| `%ecominions_total_levels%` | The levels of every minion a player has placed, added together |
| `%ecominions_limit%` | The most minions a player can place, or `infinity` from `lang.yml` with no limit |

:::info
The placeholders inside a minion's own holograms, menus and spawn item, such as `%level%` and `%used%`, are a separate, non-PlaceholderAPI mechanism. See [Configuring a Minion](how-to-make-a-minion/configuring-a-minion#internal-placeholders) for those.
:::

<hr/>

## Where to go next

- **Minion IDs:** minion IDs are file names, see [Configuring a Minion](how-to-make-a-minion/configuring-a-minion).
- **Limits:** set the limit `%ecominions_limit%` shows in [Plugin Config](plugin-config).
