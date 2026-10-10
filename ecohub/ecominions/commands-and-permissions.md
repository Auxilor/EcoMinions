---
title: "Commands and Permissions"
sidebar_position: 6
---

Every EcoMinions command and the permission node it requires.

## Commands

| Command                         | Description                                                                                      | Permission                   |
|---------------------------------|--------------------------------------------------------------------------------------------------|------------------------------|
| `/minions`                      | Opens the minion browser GUI                                                                     | `ecominions.command.minions` |
| `/minions open`                 | Opens the minion browser GUI                                                                     | `ecominions.command.open`    |
| `/minions give <player> <type>` | Gives a player a minion item. Minions are always given at level one - there is no level argument | `ecominions.command.give`    |
| `/minions reload`               | Reloads the plugin's configuration                                                               | `ecominions.command.reload`  |
| `/minions remove <player>`      | Removes a player's loaded minions and resets their count; unloaded ones count again on load      | `ecominions.command.remove`  |
| `/minions info`                 | Reports information about the minion the sender is looking at                                    | `ecominions.command.info`    |

**Aliases:** `/minions` can also be run as `/minion` or `/ecominions`.

## Permissions

| Permission                   | Description                                                                   |
|------------------------------|-------------------------------------------------------------------------------|
| `ecominions.*`               | Everything below. Granted to operators by default.                            |
| `ecominions.command.*`       | Parent node granting every command below. Granted to operators by default.    |
| `ecominions.command.minions` | Required for `/minions`. Granted by default.                                  |
| `ecominions.command.open`    | Required for `/minions open`. Granted by default.                             |
| `ecominions.command.give`    | Required for `/minions give`. Granted to operators by default.                |
| `ecominions.command.reload`  | Required for `/minions reload`. Granted to operators by default.              |
| `ecominions.command.remove`  | Required for `/minions remove`. Granted to operators by default.              |
| `ecominions.command.info`    | Required for `/minions info`. Granted to operators by default.                |
| `ecominions.admin`           | Bypasses minion limits and ownership checks. Granted to operators by default. |

<hr/>

## Where to go next

- **Build a minion:** [Configuring a Minion](how-to-make-a-minion/configuring-a-minion) to create minions to give with `/minions give`.
- **Plugin config:** [Plugin Config](plugin-config) for the plugin-wide settings.
