---
title: "Supported Plugins"
sidebar_position: 9
---

EcoMinions works with island and land plugins, so island owners decide who can place, use and pick up minions on their island. It also integrates with a few other plugins. Install the supported plugin alongside EcoMinions and it works with no extra setup, apart from the one SuperiorSkyblock2 step below.

:::tip Don't see your plugin?
If a plugin isn't listed here, it doesn't necessarily mean it's incompatible, it just means there isn't a direct integration yet. Feel free to **request support** on the [Discord](https://hub.auxilor.io/discord)!
:::

<hr/>

## Island and land plugins

EcoMinions adds three privileges to each plugin below, so owners set them in the plugin's own menu.

| Plugin | Where owners set ranks |
| --- | --- |
| SuperiorSkyblock2 | `/is permissions` |
| BentoBox | The island settings panel |
| IridiumSkyblock | The island permissions menu |
| Lands | The land's role menu |

A Lands land counts as an island everywhere on this page.

### Privileges

| Privilege | What it allows | Default |
| --- | --- | --- |
| Place | Placing a minion on the island | Members and up |
| Use | Opening a minion's menu, which covers collecting, fuelling and upgrading, and the shift-right-click `click_minion` trigger | Members and up |
| Pick up | Picking up a minion the player did not place | Island owner only |

Each plugin registers them under its own names:

| Plugin | Place | Use | Pick up |
| --- | --- | --- | --- |
| SuperiorSkyblock2 | `ECOMINIONS_PLACE` | `ECOMINIONS_USE` | `ECOMINIONS_PICKUP` |
| BentoBox | `ECOMINIONS_PLACE` | `ECOMINIONS_USE` | `ECOMINIONS_PICKUP` |
| IridiumSkyblock | `ecominionsPlace` | `ecominionsUse` | `ecominionsPickup` |
| Lands | `ecominions_place` | `ecominions_use` | `ecominions_pickup` |

Owners can change these defaults in the menus above. Placing also still has to pass your antigrief plugins.

### Rules

- A player can always open and pick up their own minions, even after leaving or being kicked from the island.
- Outside any island, only the minion's owner and players with `ecominions.admin` can use or pick it up.
- `ecominions.admin` skips every island check and both limits.
- Picking up someone else's minion does not change its owner. The item keeps the original owner, the minion stays in that owner's `per-player` count, and placing it again keeps that owner.
- Island privileges are on by default. Set `islands.privileges: false` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config) to turn them off. With no island plugin installed, nothing changes.

### SuperiorSkyblock2 setup

SuperiorSkyblock2 only shows privileges that are listed in its `menus/permissions.yml`. Add an entry for each of `ECOMINIONS_PLACE`, `ECOMINIONS_USE` and `ECOMINIONS_PICKUP` to that file for them to appear in `/is permissions`. Islands where a privilege is not set yet get the defaults above, with place and use written to the member role.

### Island limit

`limits.per-island` caps how many minions an island can hold, counting every minion on it whoever placed it. It is off by default, with a cap of 10.

- Give the island owner `ecominions.island-limit.<number>` to raise that island's cap, e.g. `ecominions.island-limit.20`. See [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).
- With both limits on, a minion must fit under both. Turn off `limits.per-player` for an island-only cap.

### Working while members are online

By default a minion works only while its owner is online. Set `islands.active-while-member-online: true` and a minion on an island works while its owner or any island member is online. See [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config#islands).

<hr/>

## Other plugins

| Plugin | Support |
| --- | --- |
| EcoShop | Minions use EcoShop sell prices for items with no price of their own, see `sell-pricing` in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config#sell-pricing) |
| EcoItems | Fishing minions use EcoItems `fishing` loot while the owner is online, slayers apply EcoItems `mob` loot, and mining uses EcoItems block drops. EcoItems items also work as item lookups in minion configs. |
| PlaceholderAPI | Minion counts and limits as placeholders, see [PlaceholderAPI](https://hub.auxilor.io/wiki/ecominions/placeholderapi) |

<hr/>

## For developers

To add your own island plugin, implement `IslandProvider` and `MinionIsland`, then register the provider. Providers are asked in the order they were registered, and the first one that returns an island wins.

```kotlin
class MyIslandProvider : IslandProvider {
    override val id = "MyIslands"

    override fun islandAt(location: Location): MinionIsland? =
        MyIslandsApi.getIslandAt(location)?.let { MyMinionIsland(it) }

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        MyIslandsApi.getIslandsOf(uuid).map { MyMinionIsland(it) }
}

class MyMinionIsland(private val island: MyIsland) : MinionIsland {
    override val provider = "MyIslands"
    override val id = island.id
    override val owner: UUID? = island.ownerId

    override fun isAllowed(player: Player, privilege: IslandPrivilege) =
        island.hasRank(player.uniqueId, privilege.name)

    override fun isMember(uuid: UUID) = island.hasMember(uuid)

    override fun members() = island.memberIds
}

EcoMinionsApi.instance.islands().register(MyIslandProvider())
```

:::info
An island id must be unique within its provider and must not contain `|`.
:::

<hr/>

## Where to go next

- **Config:** the island and limit keys are in [Plugin Config](https://hub.auxilor.io/wiki/ecominions/plugin-config).
- **Permissions:** `ecominions.island-limit.<number>` and `ecominions.admin` are in [Commands and Permissions](https://hub.auxilor.io/wiki/ecominions/commands-and-permissions).
- **Placeholders:** see [PlaceholderAPI](https://hub.auxilor.io/wiki/ecominions/placeholderapi).
