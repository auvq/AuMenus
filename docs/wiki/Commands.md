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

## Target Player

Menus with `allow_target_player: true` can resolve placeholders against a different player. There are two ways to specify the target:

**Direct argument** (requires `target_player_arg: true`):
```
/stats Steve
/profile Notch
```

**Flag** (works with any `allow_target_player` menu):
```
/stats -p:Steve
/am open stats -p:Notch
```

Both open the menu for the sender, but all PAPI placeholders and `{target}` resolve for the specified player. Tab completion suggests online player names when `target_player_arg` is enabled.

```yaml
allow_target_player: true
target_player_arg: true
```

By default, only online players can be targeted. Add `allow_offline_target: true` for offline players (most PAPI placeholders won't resolve for them).

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
