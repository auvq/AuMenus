# Placeholders and Meta System

Placeholders can be used in item names, lore, titles, action values, and requirement configs.

## Built-in Placeholders

These work without PlaceholderAPI.

| Placeholder              | Description              |
|--------------------------|--------------------------|
| `%player%`               | Player name              |
| `%player_name%`          | Player name              |
| `%player_displayname%`   | Player display name      |
| `%player_level%`         | Experience level         |
| `%player_health%`        | Current health (1 decimal) |
| `%player_max_health%`    | Max health (1 decimal)   |
| `%player_food_level%`    | Food level               |
| `%player_world%`         | World name               |
| `%player_x%`             | Block X coordinate       |
| `%player_y%`             | Block Y coordinate       |
| `%player_z%`             | Block Z coordinate       |
| `%player_gamemode%`      | Game mode name           |
| `%player_uuid%`          | Player UUID              |
| `%server_online%`        | Online player count      |
| `%server_max_players%`   | Max player count         |
| `%random_100%`           | Random number 0-99       |
| `%random_1000%`          | Random number 0-999      |

## Internal Placeholders

These use curly braces and resolve in specific contexts.

| Placeholder    | Context | Description |
|----------------|---------|-------------|
| `{page}`       | Titles, lore, names | Current page number |
| `{max_page}`   | Titles, lore, names | Total page count |
| `{player}`     | Titles, lore, names | Player name |
| `{arg_name}`   | Titles, lore, names, actions | Menu argument value |
| `{input}`      | Input on_submit actions | Text the player entered |

### Deny context placeholders

Available in deny actions when a requirement fails.

| Placeholder    | Description |
|----------------|-------------|
| `{needed}`     | Required value |
| `{has}`        | Player's current value |
| `{remaining}`  | Difference (needed - has) |
| `{input}`      | `input` value from the check |
| `{output}`     | `output` value from the check |

## PlaceholderAPI

When PAPI is installed, any external placeholder works in AuMenus configs:

```yaml
lore:
  - "&7Balance: &a$%vault_eco_balance_formatted%"
  - "&7Rank: &e%luckperms_primary_group_name%"
```

AuMenus also registers the `aumenus` expansion:

| Placeholder                                | Description |
|--------------------------------------------|-------------|
| `%aumenus_is_in_menu%`                     | `true` if player has a menu open |
| `%aumenus_opened_menu%`                    | Name of open menu, or empty |
| `%aumenus_last_menu%`                      | Name of last opened menu |
| `%aumenus_meta_<key>_<TYPE>%`              | Meta value (empty if not set) |
| `%aumenus_meta_<key>_<TYPE>_<default>%`    | Meta value with fallback |
| `%aumenus_meta_has_value_<key>%`           | `true`/`false` if key exists |
| `%aumenus_meta_has_value_<key>_<TYPE>%`    | `true`/`false` if key exists as type |

---

## Meta System

AuMenus stores persistent player data using Bukkit's PersistentDataContainer (PDC). Values survive server restarts.

### Data Types

`STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN` (stored as STRING internally).

### Meta Actions

Use the `meta` action type in menu configs:

```yaml
- meta: set coins INTEGER 100
- meta: add coins INTEGER 50
- meta: subtract coins INTEGER 10
- meta: switch notifications BOOLEAN
- meta: remove coins
```

- `set` -- Assign a value
- `add` -- Add to a numeric value (starts at 0 if unset)
- `subtract` -- Subtract from a numeric value
- `switch` -- Toggle a boolean (defaults to switching from `false` to `true`)
- `remove` -- Delete the key. Only needs the key name.

### Meta Commands

See [Commands](Commands.md) for `/am meta` syntax.

### Meta in Requirements

```yaml
click_require:
  checks:
    quest_done:
      type: has_meta
      key: "quest_complete"
      meta_type: BOOLEAN
      value: "true"
      deny:
        - msg: "&cComplete the quest first!"
```

Omit `value` to check existence only.

### Example: Toggle Setting

```yaml
toggle_notifications:
  material: BELL
  slot: 22
  name: "&bNotifications"
  lore:
    - "&7Status: %aumenus_meta_notifications_BOOLEAN_true%"
  update: true
  on_click:
    - meta: switch notifications BOOLEAN
    - refresh
```
