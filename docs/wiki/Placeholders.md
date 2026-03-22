# Placeholders

AuMenus supports multiple placeholder systems. Placeholders can be used in item names, lore, titles, action values, and requirement configs.

## Built-in Placeholders

These work without PlaceholderAPI installed.

| Placeholder              | Description                           |
|--------------------------|---------------------------------------|
| `%player%`               | Player name                           |
| `%player_name%`          | Player name                           |
| `%player_displayname%`   | Player name (display name)            |
| `%player_level%`         | Experience level                      |
| `%player_health%`        | Current health (1 decimal)            |
| `%player_max_health%`    | Maximum health (1 decimal)            |
| `%player_food_level%`    | Food level                            |
| `%player_world%`         | World name                            |
| `%player_x%`             | Block X coordinate                    |
| `%player_y%`             | Block Y coordinate                    |
| `%player_z%`             | Block Z coordinate                    |
| `%player_gamemode%`      | Game mode name                        |
| `%player_uuid%`          | Player UUID                           |
| `%server_online%`        | Online player count                   |
| `%server_max_players%`   | Maximum player count                  |
| `%random_100%`           | Random number 0-99                    |
| `%random_1000%`          | Random number 0-999                   |

## Internal Placeholders

These use curly braces and are resolved in specific contexts.

### Menu-level placeholders

| Placeholder    | Context                          | Description              |
|----------------|----------------------------------|--------------------------|
| `{page}`       | Titles, lore, item names         | Current page number      |
| `{max_page}`   | Titles, lore, item names         | Total page count         |
| `{player}`     | Titles, lore, item names         | Player name              |
| `{arg_name}`   | Titles, lore, item names, actions | Value of menu argument  |

### Input placeholder

| Placeholder | Context                              | Description                    |
|-------------|--------------------------------------|--------------------------------|
| `{input}`   | anvil_input on_submit, chat_input on_submit | Text the player entered |

### Deny context placeholders

Available in deny actions when a requirement fails.

| Placeholder    | Description                                         |
|----------------|-----------------------------------------------------|
| `{needed}`     | The required value (money amount, exp amount, etc.)  |
| `{has}`        | The player's current value                           |
| `{remaining}`  | The difference: needed minus has                     |
| `{input}`      | The `input` value from the failed requirement config |
| `{output}`     | The `output` value from the failed requirement config |

```yaml
deny: "&cYou need ${needed}! You have ${has}. Missing: ${remaining}"
```

These are populated for `has_money`, `has_exp`, and `has_item` requirement types:

- **has_money:** `{needed}` = required amount, `{has}` = current balance, `{remaining}` = amount short
- **has_exp:** `{needed}` = required amount/levels, `{has}` = current amount/levels, `{remaining}` = amount short
- **has_item:** `{needed}` = required amount, `{remaining}` = required amount
- **String/comparator types:** `{input}` and `{output}` contain the respective config values

## PlaceholderAPI Placeholders

When PlaceholderAPI is installed, AuMenus registers the `aumenus` expansion.

### Menu state

| Placeholder                  | Description                                |
|------------------------------|--------------------------------------------|
| `%aumenus_is_in_menu%`       | `true` if the player has an AuMenus menu open |
| `%aumenus_opened_menu%`      | Name of the currently open menu, or empty  |
| `%aumenus_last_menu%`        | Name of the last menu opened               |

### Meta values

| Placeholder                                | Description                         |
|--------------------------------------------|-------------------------------------|
| `%aumenus_meta_<key>_<TYPE>%`              | Meta value (empty if not set)       |
| `%aumenus_meta_<key>_<TYPE>_<default>%`    | Meta value with default             |
| `%aumenus_meta_has_value_<key>%`           | `true`/`false` if key exists        |
| `%aumenus_meta_has_value_<key>_<TYPE>%`    | `true`/`false` if key exists as type|

See [Meta System](Meta-System.md) for details.

### Using external PAPI placeholders

Any PlaceholderAPI placeholder from other expansions works in AuMenus configs:

```yaml
lore:
  - "&7Balance: &a$%vault_eco_balance_formatted%"
  - "&7Rank: &e%luckperms_primary_group_name%"
  - "&7Playtime: &b%statistic_hours_played%h"
```
