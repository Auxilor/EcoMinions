---
title: "Commands and Permissions"
sidebar_position: 7
---

This page lists every EcoMinions command and permission, so you can hand out minions, reload configs, and control how many minions players can place. All commands run under `/minions`, `/minion`, or `/ecominions`.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/minions` | Opens the minion browser | `ecominions.command.minions` |
| `/minions open` | Opens the minion browser | `ecominions.command.open` |
| `/minions give <player> <minion>` | Gives a player a level 1 minion item | `ecominions.command.give` |
| `/minions remove <player>` | Removes a player's loaded minions and resets their count | `ecominions.command.remove` |
| `/minions info` | Shows information about the minion you are looking at | `ecominions.command.info` |
| `/minions reload` | Reloads the plugin | `ecominions.command.reload` |

`/minions give` takes a minion ID, which is the file name in `/minions/`. `/minions remove` only reaches minions in loaded chunks; minions in unloaded chunks count towards the limit again when they load.

## Additional permissions

| Permission | Description |
| --- | --- |
| `ecominions.limit.<number>` | Raises a player's minion limit to `<number>`, e.g. `ecominions.limit.10`. The highest one a player has wins. Needs `limits.per-player.permission-overrides: true` |
| `ecominions.island-limit.<number>` | Raises the minion limit of every island the holder owns to `<number>`, e.g. `ecominions.island-limit.20`. The highest one wins. Needs `limits.per-island.permission-overrides: true` |
| `ecominions.type-limit.<type>.<number>` | Raises a player's limit for one minion type to `<number>`, e.g. `ecominions.type-limit.miner.15`. The highest one wins. Needs `limits.per-type.permission-overrides: true` |
| `ecominions.minion-limit.<minion>.<number>` | Raises a player's limit for one minion to `<number>`, e.g. `ecominions.minion-limit.coal.4`. The highest one wins. Only applies to a minion with `limit` in its config |
| `ecominions.rename` | Rename your own minions with a name tag. See [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config#rename) |
| `ecominions.rename.color` | Keep colour and format codes in a minion's name. Needs `ecominions.rename` |
| `ecominions.admin` | No minion limit, and can pick up other players' minions. Skips every island check and every limit |
| `ecominions.command.*` | Permission to use every command |
| `ecominions.*` | Every permission above |

:::info
`/minions` and `/minions open` are given to everyone by default. `ecominions.rename` and `ecominions.rename.color` are given to nobody by default, but operators get them through `ecominions.*`. Every other command, `ecominions.admin`, and the parent nodes are given to operators by default.
:::

<hr/>

## Where to go next

- **Minions:** make minions to hand out in [Configuring a Minion](https://hub.auxilor.io/wiki/ecominions/how-to-make-a-minion-configuring-a-minion).
- **Islands:** island ranks and limits are in [Supported Plugins](https://hub.auxilor.io/wiki/ecominions/supported-plugins).
- **Limits:** the default limit and permission overrides live in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config).
- **Placeholders:** show a player's limit with [PlaceholderAPI](https://hub.auxilor.io/wiki/ecominions/placeholderapi).
