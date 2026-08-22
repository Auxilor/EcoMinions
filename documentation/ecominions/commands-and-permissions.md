---
title: "Commands and Permissions"
sidebar_position: 6
---

Every EcoMinions command and the permission node it requires.

## Commands

| Command                              | Description                                                     | Permission              |
|---------------------------------------|-------------------------------------------------------------------|--------------------------|
| `/minions`                           | Opens the minion type browser GUI                               | `ecominions.command`     |
| `/minions open`                      | Opens the minion type browser GUI                                | `ecominions.command`     |
| `/minions give <player> <type>`      | Gives a player a minion item. Minions are always given at level one - there is no level argument | `ecominions.admin.give`  |
| `/minions reload`                    | Reloads the plugin's configuration                                | `ecominions.admin.reload`|
| `/minions remove <player>`           | Removes every minion owned by a player                            | `ecominions.admin.remove`|
| `/minions info`                      | Reports information about the minion the sender is looking at    | `ecominions.admin.info`  |

**Aliases:** `/minions` can also be run as `/minion` or `/ecominions`.

## Permissions

| Permission                | Description                                                                 |
|----------------------------|-------------------------------------------------------------------------------|
| `ecominions.command`      | Base permission to use `/minions` and `/minions open`. Granted by default.   |
| `ecominions.admin`        | Parent node granting all admin subcommands below. Granted to operators by default. |
| `ecominions.admin.give`   | Required for `/minions give`. Granted to operators by default.               |
| `ecominions.admin.reload` | Required for `/minions reload`. Granted to operators by default.             |
| `ecominions.admin.remove` | Required for `/minions remove`. Granted to operators by default.             |
| `ecominions.admin.info`   | Required for `/minions info`. Granted to operators by default.               |

<hr/>

## Where to go next

- **Build a minion:** [How to Make a Minion](how-to-make-a-minion) to create minion types to give with `/minions give`.
- **Plugin config:** [Plugin Config](plugin-config) for the plugin-wide settings.
