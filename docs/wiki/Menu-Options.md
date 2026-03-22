# Menu Options

Each menu is a single YAML file placed in `plugins/AuMenus/menus/`. The file name (without extension) becomes the menu's internal name.

## Basic Options

### title

The inventory title shown to the player. Supports MiniMessage, legacy color codes (`&a`, `&#RRGGBB`), placeholders, and argument references (`{arg_name}`).

```yaml
title: "<dark_gray>My Shop</dark_gray>"
title: "&8My Shop"
title: "&8Profile: {player_name}"
title: "<dark_gray>Warps {page}/{max_page}</dark_gray>"
```

### size

The number of slots in a chest inventory. Must be a multiple of 9: 9, 18, 27, 36, 45, or 54. Ignored for non-chest inventory types.

```yaml
size: 27
```

### type

The inventory type. Defaults to `CHEST` if omitted.

Valid types: `CHEST`, `BARREL`, `ENDER_CHEST`, `SHULKER_BOX`, `HOPPER`, `DISPENSER`, `DROPPER`, `FURNACE`, `BLAST_FURNACE`, `SMOKER`, `BREWING`, `WORKBENCH`, `ANVIL`.

```yaml
type: HOPPER
```

### command

A command that opens this menu when a player types it. The command is registered automatically.

```yaml
command: shop
```

Players can then use `/shop` to open the menu.

### register_command

Whether to register the command with the server's command map. Defaults to `true`. Set to `false` if you want to handle the command yourself or use it only via `/am open`.

```yaml
register_command: false
```

### update_interval

How often (in ticks) items with `update: true` are refreshed. Defaults to the value in `config.yml` (`default_update_interval`, default 20 ticks = 1 second). Set to 0 to disable automatic updates for this menu.

```yaml
update_interval: 40
```

## Arguments

### args

A list of argument names this menu accepts. Arguments are passed via command usage or when opening the menu with `/am open`.

```yaml
args:
  - player_name
```

When the command is `/profile Steve`, the argument `{player_name}` resolves to `Steve` in titles, item names, lore, and actions.

### args_usage

A message shown when a player opens the menu without providing required arguments.

```yaml
args_usage: "&cUsage: /profile <player>"
```

### arg_require

Requirements that validate argument values before the menu opens. Each key is an argument name with a requirement config block.

```yaml
arg_require:
  player_name:
    type: is_object
    object: PLAYER
    deny: "&cPlayer '{player_name}' is not online!"
```

The `{arg_name}` placeholder in deny messages is replaced with the actual argument value.

## Open Requirements and Events

### open_require

Requirements that must pass before the menu opens. Uses the same syntax as all other requirement blocks (shorthand or full form). See [Requirements](Requirements.md).

```yaml
open_require:
  perm: shop.access
  deny: "&cYou don't have permission to open this shop."
```

Players with the `aumenus.bypass.openrequirement` permission skip open requirements.

### on_open

Actions executed when the menu opens successfully. See [Actions](Actions.md).

```yaml
on_open:
  - sound: block.chest.open
  - msg: "&aWelcome to the shop!"
```

### on_close

Actions executed when the menu closes (not triggered during reload or when opening another menu via `open` action).

```yaml
on_close:
  - sound: block.chest.close
```

## Complete Example

```yaml
title: "<dark_gray>Server Shop</dark_gray>"
command: shop
register_command: true
size: 27
update_interval: 20

open_require:
  perm: shop.access
  deny: "&cYou don't have permission!"

on_open:
  - sound: block.chest.open

on_close:
  - sound: block.chest.close

items:
  example_item:
    material: DIAMOND
    slot: 13
    name: "&bDiamond"
    on_click:
      - msg: "&aYou clicked the diamond!"
```
