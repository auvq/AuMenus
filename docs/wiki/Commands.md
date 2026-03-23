# Commands

Main command: `/aumenus` (alias: `/am`)

## Commands

| Command | Arguments | Permission | Description |
|---------|-----------|------------|-------------|
| `/am open` | `<menu> [player] [args...]` | `aumenus.open` | Open a menu. `aumenus.open.others` to target other players. |
| `/am list` | | `aumenus.list` | List loaded menus with commands and error counts. |
| `/am reload` | | `aumenus.reload` | Reload all menus and config. Open menus re-render automatically. |
| `/am execute` | `<player> <action>` | `aumenus.execute` | Execute a single action on a player. |
| `/am create` | `<name> <size>` | `aumenus.admin` | Create empty menu file and open the editor. Size must be a multiple of 9. |
| `/am editor` | `<menu>` | `aumenus.admin` | Open in-game editor for an existing menu. |
| `/am meta` | `<player> <op> [key] [type] [value]` | `aumenus.admin` | Manage persistent player metadata. |
| `/am migrate` | `[name]` | `aumenus.admin` | Migrate menu files. Omit name to migrate all. |

## Meta Operations

| Operation  | Requires key | Requires type | Requires value |
|------------|-------------|---------------|----------------|
| `set`      | Yes | Yes | Yes |
| `add`      | Yes | Yes | Yes |
| `subtract` | Yes | Yes | Yes |
| `switch`   | Yes | Yes | No |
| `get`      | Yes | Yes | No |
| `remove`   | Yes | Yes | No |
| `list`     | No  | No  | No |

Meta types: `STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN`.

## Target Player (`-p:`)

Menus that have `allow_target_player: true` support the `-p:playername` flag. This opens the menu for the sender, but resolves all PAPI placeholders and `{target}` against the specified player.

```
/stats -p:Notch
/am open stats -p:Steve
```

This is useful for admin menus that show another player's data (balance, stats, etc). The `-p:` flag can appear anywhere in the arguments.

In the menu title and items, use `{target}` to show the target player's name, or any PAPI placeholder to show their data.

By default, only online players can be targeted. To allow offline players, add `allow_offline_target: true` to the menu config. Note that most PAPI placeholders won't resolve for offline players.

```yaml
title: 'Stats - {target}'
allow_target_player: true
allow_offline_target: false
size: 27
```

Both options can also be set globally in `config.yml` as `default_allow_target_player` and `default_allow_offline_target`.

## Examples

```
/am open shop
/am open shop Steve
/stats -p:Notch
/am execute Steve msg &aHello!
/am execute Steve sound entity.experience_orb.pickup
/am meta Steve set coins INTEGER 100
/am meta Steve list
```

## Permissions

| Permission | Description |
|------------|-------------|
| `aumenus.open` | Use `/am open` |
| `aumenus.open.others` | Open menus for other players |
| `aumenus.list` | Use `/am list` |
| `aumenus.reload` | Use `/am reload` |
| `aumenus.execute` | Use `/am execute` |
| `aumenus.admin` | Use `/am create`, `editor`, `meta`, `migrate` |
| `aumenus.bypass.openrequirement` | Bypass menu open requirements |
