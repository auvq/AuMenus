# Migration

AuMenus includes tools for migrating from DeluxeMenus and converting legacy color codes to MiniMessage.

## Commands

```
/am migrate deluxemenus           # Migrate all DM files
/am migrate deluxemenus <name>    # Migrate a single DM file
/am migrate minimessage           # Convert legacy codes to MiniMessage in all AuMenus files
/am migrate minimessage <name>    # Convert a single AuMenus file
```

Permission: `aumenus.admin`

## DeluxeMenus Migration

### Source Folders

The migrator checks these locations in order:

1. `plugins/DeluxeMenus/gui_menus/`
2. `plugins/AuMenus/migration/`

Copy your DM menu files into `plugins/AuMenus/migration/` if the migrator does not detect them automatically.

### What Gets Converted

| DeluxeMenus | AuMenus |
|---|---|
| `menu_title` | `title` |
| `display_name` | `name` |
| `open_command` (string or list) | `command` + `command_aliases` |
| `click_commands` | `on_click` |
| `left_click_commands` | `on_left_click` |
| `right_click_commands` | `on_right_click` |
| `shift_left_click_commands` | `on_shift_left_click` |
| `shift_right_click_commands` | `on_shift_right_click` |
| `middle_click_commands` | `on_middle_click` |
| `open_commands` | `on_open` |
| `close_commands` | `on_close` |
| `inventory_type` | `type` |
| `open_requirement` | `open_require` |
| `view_requirement` | `view_require` |
| `*_click_requirement` | `*_click_require` |
| `requirements:` sub-key | `checks:` sub-key |
| `deny_commands` | `deny` |
| `success_commands` | `success` |
| `minimum_requirements` | `minimum` |
| `args_usage_message` | `args_usage` |
| `custom_model_data` | `model_data` |
| `hide_enchantments: true` | `item_flags: [HIDE_ENCHANTS]` |
| `hide_attributes: true` | `item_flags: [HIDE_ATTRIBUTES]` |
| `[player] cmd` | `player: cmd` |
| `[console] cmd` | `console: cmd` |
| `[message] text` | `msg: text` |
| `[close]` | `close` |
| `[refresh]` | `refresh` |
| `[openguimenu] name` | `open: name` |
| `[sound] name` | `sound: name` |
| `<delay=N>` | `delay: N` |
| `<chance=N>` | `chance: N` |
| `(==)`, `(>=)`, etc. | `==`, `>=`, etc. |
| `%deluxemenus_meta_*%` | `%aumenus_meta_*%` |

### Output

Converted files are saved to `plugins/AuMenus/menus/`. Existing files with the same name are skipped. Run `/am reload` after migration.

## MiniMessage Migration

Converts all legacy `&` color codes in your AuMenus menu files to proper MiniMessage tags. Uses Adventure's official serializer for clean, minimal output.

```
/am migrate minimessage           # Convert all files
/am migrate minimessage shop      # Convert a single file
```

Processes: title, item names, lore, action messages, and deny messages. Does not modify command values, material names, or non-text fields.

## Differences from DeluxeMenus

If you are migrating from DeluxeMenus, here are the key behavioral differences to be aware of.

### Color Codes

Both plugins support legacy color codes (`&a`, `&#RRGGBB`). AuMenus converts them to MiniMessage internally. Colors reset formatting (bold, italic, etc.) - matching DM behavior.

AuMenus additionally supports MiniMessage tags (`<gradient>`, `<rainbow>`, `<hover>`, `<click>`, etc.) which DM does not.

AuMenus automatically removes default italic on item names and lore. DM does not - items in DM show italic by default unless manually overridden with `&r`.

### Refresh Timing

In DM, `[refresh]` executes inline. In AuMenus, `player` and `console` actions are scheduled on the player/global scheduler, so they execute on the next tick. A `refresh` without delay may re-render before the command finishes processing.

Add a delay when refreshing after commands:

```yaml
on_click:
  - player: sethome myhome
  - refresh: ''
    delay: 5
```

### Enchantment Names

AuMenus accepts both old Bukkit names and modern Minecraft registry names. No changes needed after migration.

| Old Name (DM) | Modern Name |
|---|---|
| `PROTECTION_ENVIRONMENTAL` | `protection` |
| `DURABILITY` | `unbreaking` |
| `DAMAGE_ALL` | `sharpness` |
| `DIG_SPEED` | `efficiency` |
| `ARROW_DAMAGE` | `power` |
| `ARROW_KNOCKBACK` | `punch` |
| `ARROW_FIRE` | `flame` |
| `ARROW_INFINITE` | `infinity` |
| `LOOT_BONUS_MOBS` | `looting` |
| `LOOT_BONUS_BLOCKS` | `fortune` |
| `OXYGEN` | `respiration` |
| `WATER_WORKER` | `aqua_affinity` |
| `PROTECTION_FIRE` | `fire_protection` |
| `PROTECTION_FALL` | `feather_falling` |
| `PROTECTION_EXPLOSIONS` | `blast_protection` |
| `PROTECTION_PROJECTILE` | `projectile_protection` |
| `DAMAGE_UNDEAD` | `smite` |
| `DAMAGE_ARTHROPODS` | `bane_of_arthropods` |
| `LUCK` | `luck_of_the_sea` |

### Menu Close Behavior

AuMenus does NOT fire `on_close` actions when:
- The menu is closed because another menu is opened via `open` action
- The server reloads menus via `/am reload`

DM may fire close actions in both cases.

### Sound Names

AuMenus automatically converts underscores to dots in sound names if no dots are present. `entity_experience_orb_pickup` becomes `entity.experience.orb.pickup`.

### Click Fallback

`on_click` is a fallback. If a specific click type (e.g., `on_left_click`) has actions defined, `on_click` does NOT also execute. Same for click requirements.

### Features Not in AuMenus

- NMS-based NBT tag data on items
- JavaScript requirement evaluation (Nashorn/GraalVM)

### AuMenus Exclusive Features

- Pagination (`page_slots`, `page_items`, `prev_page`/`next_page`)
- Anvil and chat text input collection
- Persistent player metadata (PDC-backed `meta` actions)
- In-game visual menu editor (`/am editor`)
- Menu creation command (`/am create`)
- MiniMessage support (gradients, rainbow, hover, click events)
- `placeholder-` material (dynamic resolution from PAPI)
- `texture-` head prefix
- Equipment slots as materials (`main_hand`, `off_hand`, `armor_*`)
- Shorthand requirement syntax
- Requirement deny placeholders (`{needed}`, `{has}`, `{remaining}`)
- `dynamic_amount` (placeholder-resolved stack size)
- `lore_append_mode` (OVERRIDE, TOP, BOTTOM, IGNORE)
- `enchantment_glint_override`, `hide_tooltip`, `rarity`
- `item_model`, `tooltip_style` (1.21.2+)
- Folia support
- Developer API with custom actions, requirements, and cancellable events
- Config error visualization (red glass pane with error in lore)
- Duplicate item name detection
