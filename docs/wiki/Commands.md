# Commands

The main command is `/aumenus` with the alias `/am`.

## /aumenus open

Opens a menu for a player.

```
/aumenus open <menu> [player] [args...]
```

| Argument | Description                                       |
|----------|---------------------------------------------------|
| `menu`   | The menu name (file name without extension)        |
| `player` | Target player (optional, defaults to sender)       |
| `args`   | Space-separated arguments passed to the menu       |

Arguments are sanitized (MiniMessage tags are escaped) to prevent injection.

**Permission:** `aumenus.open`
**Open for others:** `aumenus.open.others`

**Examples:**

```
/am open shop
/am open shop Steve
/am open profile Steve Steve
```

## /aumenus list

Lists all loaded menus with their commands and error counts.

```
/aumenus list
```

**Permission:** `aumenus.list`

## /aumenus reload

Reloads all menu files and the plugin configuration. Players with open menus have their menus re-rendered automatically. If a menu's size or type changed, the inventory is re-opened.

```
/aumenus reload
```

**Permission:** `aumenus.reload`

## /aumenus execute

Executes a single action on a player. Useful for testing actions from the console.

```
/aumenus execute <player> <action>
```

**Permission:** `aumenus.execute`

**Examples:**

```
/am execute Steve msg &aHello!
/am execute Steve sound entity.experience_orb.pickup
/am execute Steve open shop
/am execute Steve meta set coins INTEGER 100
```

## /aumenus create

Creates a new empty menu file and opens the in-game editor.

```
/aumenus create <name> <size>
```

| Argument | Description                                      |
|----------|--------------------------------------------------|
| `name`   | Menu name (becomes the file name)                 |
| `size`   | Inventory size (must be a multiple of 9: 9-54)    |

**Permission:** `aumenus.admin`

## /aumenus editor

Opens the in-game menu editor for an existing menu. See [Editor](Editor.md).

```
/aumenus editor <menu>
```

**Permission:** `aumenus.admin`

## /aumenus meta

Manages persistent player metadata. See [Meta System](Meta-System.md).

```
/aumenus meta <player> <operation> <key> <type> [value]
```

### Operations

| Operation  | Description                     | Requires value |
|------------|---------------------------------|----------------|
| `set`      | Set a meta key to a value       | Yes            |
| `remove`   | Remove a meta key               | No             |
| `add`      | Add a number to a meta value    | Yes            |
| `subtract` | Subtract a number               | Yes            |
| `switch`   | Toggle a boolean value          | No             |
| `get`      | Display the current value       | No             |
| `list`     | List all meta keys for a player | No (no key needed) |

### Types

`STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN`

**Permission:** `aumenus.admin`

**Examples:**

```
/am meta Steve set coins INTEGER 100
/am meta Steve add coins INTEGER 50
/am meta Steve get coins INTEGER
/am meta Steve switch vip BOOLEAN
/am meta Steve remove coins STRING
/am meta Steve list
```

## /aumenus migrate

Migrates DeluxeMenus menu files to AuMenus format. See [Migration](Migration.md).

```
/aumenus migrate           # Migrate all files
/aumenus migrate <name>    # Migrate a single file
```

**Permission:** `aumenus.admin`

## Permission Summary

| Permission                     | Description                          |
|--------------------------------|--------------------------------------|
| `aumenus.open`                 | Use `/am open`                       |
| `aumenus.open.others`          | Open menus for other players         |
| `aumenus.list`                 | Use `/am list`                       |
| `aumenus.reload`               | Use `/am reload`                     |
| `aumenus.execute`              | Use `/am execute`                    |
| `aumenus.admin`                | Use `/am create`, `editor`, `meta`, `migrate` |
| `aumenus.bypass.openrequirement` | Bypass menu open requirements      |
