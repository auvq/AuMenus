# Migration from DeluxeMenus

AuMenus includes a built-in migration tool that converts DeluxeMenus menu files to AuMenus format.

## Commands

### Migrate all files

```
/am migrate
```

Converts all `.yml` and `.yaml` files found in the source folder.

### Migrate a single file

```
/am migrate <name>
```

Converts a single file by name (without extension).

## Source Folders

The migrator checks these locations in order:

1. `plugins/DeluxeMenus/gui_menus/` -- The standard DeluxeMenus menu folder
2. `plugins/AuMenus/migration/` -- A manual migration folder

If neither folder exists, the command reports that no files were found.

To migrate without DeluxeMenus installed, copy your DM menu files into `plugins/AuMenus/migration/`.

## What Gets Converted

| DeluxeMenus                    | AuMenus                    |
|--------------------------------|----------------------------|
| `menu_title` / `title`         | `title`                    |
| `open_command`                  | `command`                  |
| `register_command`              | `register_command`         |
| `size`                          | `size`                     |
| `inventory_type`                | `type`                     |
| `update_interval`               | `update_interval`          |
| `args`                          | `args`                     |
| `args_usage_message`            | `args_usage`               |
| `open_requirement`              | `open_require`             |
| `open_commands`                 | `on_open`                  |
| `close_commands`                | `on_close`                 |
| `items.*.display_name`          | `items.*.name`             |
| `items.*.click_commands`        | `items.*.on_click`         |
| `items.*.left_click_commands`   | `items.*.on_left_click`    |
| `items.*.right_click_commands`  | `items.*.on_right_click`   |
| `items.*.shift_left_click_commands` | `items.*.on_shift_left_click` |
| `items.*.shift_right_click_commands` | `items.*.on_shift_right_click` |
| `items.*.view_requirement`      | `items.*.view_require`     |
| `items.*.click_requirement`     | `items.*.click_require`    |
| `items.*.hide_enchantments`     | `items.*.item_flags: [HIDE_ENCHANTS]` |
| `items.*.hide_attributes`       | `items.*.item_flags: [HIDE_ATTRIBUTES]` |

### Action conversion

DM action prefixes are converted to AuMenus action types:

| DeluxeMenus                     | AuMenus              |
|---------------------------------|----------------------|
| `[player] command`              | `player: command`    |
| `[console] command`             | `console: command`   |
| `[commandevent] command`        | `commandevent: command` |
| `[message] text`                | `msg: text`          |
| `[minimessage] text`            | `minimessage: text`  |
| `[openguimenu] menu`            | `open: menu`         |
| `[close]`                       | `close`              |
| `[refresh]`                     | `refresh`            |
| `[broadcast] text`              | `broadcast: text`    |
| `[sound] sound`                 | `sound: sound`       |
| `[broadcastsound] sound`        | `broadcast_sound: sound` |
| `[takemoney] amount`            | `take_money: amount` |
| `[givemoney] amount`            | `give_money: amount` |
| `[takeexp] amount`              | `take_exp: amount`   |
| `[giveexp] amount`              | `give_exp: amount`   |
| `[givepermission] perm`         | `give_perm: perm`    |
| `[takepermission] perm`         | `take_perm: perm`    |
| `[connect] server`              | `connect: server`    |
| `[json] json`                   | `json: json`         |
| `[chat] message`                | `chat: message`      |
| `[meta] action`                 | `meta: action`       |
| `[placeholder] text`            | `placeholder: text`  |

DM delay and chance modifiers (`<delay=N>`, `<chance=N>`) are converted to `delay` and `chance` keys.

### Requirement conversion

DM requirements (under `requirements` sections with `deny_commands` and `success_commands`) are converted to AuMenus `checks` format with `deny` and `success` lists. `minimum_requirements` becomes `minimum`.

## Output

Converted files are saved to `plugins/AuMenus/menus/`. If a file with the same name already exists, it is skipped.

After migration, run `/am reload` to load the new menus.

## What to Check After Migration

- **Color codes:** AuMenus supports both legacy codes (`&a`, `&#RRGGBB`) and MiniMessage. Your existing legacy codes will work, but consider updating to MiniMessage for advanced formatting.
- **Actions:** Verify that all action conversions are correct, especially complex ones with delays and chances.
- **Requirements:** Complex requirement structures should be reviewed. The migrator handles standard DM requirement types.
- **Head materials:** `head-<player>`, `basehead-<base64>`, and `texture-<id>` prefixes work the same way.
- **Custom model data and other properties:** These are carried over directly.
- **Placeholders:** PlaceholderAPI placeholders carry over without changes.
