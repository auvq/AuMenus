# Meta System

AuMenus provides a persistent player data system backed by Bukkit's PersistentDataContainer (PDC). Meta values are saved with the player and persist across server restarts.

## Data Types

| Type      | Description             |
|-----------|-------------------------|
| `STRING`  | Text value              |
| `INTEGER` | Whole number (int)      |
| `LONG`    | Large whole number      |
| `DOUBLE`  | Decimal number          |
| `BOOLEAN` | true/false (stored as STRING internally) |

## Command Usage

See [Commands](Commands.md) for full syntax.

```
/am meta <player> set <key> <type> <value>
/am meta <player> remove <key> <type>
/am meta <player> add <key> <type> <value>
/am meta <player> subtract <key> <type> <value>
/am meta <player> switch <key> <type>
/am meta <player> get <key> <type>
/am meta <player> list
```

**Examples:**

```
/am meta Steve set coins INTEGER 100
/am meta Steve add coins INTEGER 50
/am meta Steve subtract coins INTEGER 10
/am meta Steve get coins INTEGER
/am meta Steve switch notifications BOOLEAN
/am meta Steve remove coins STRING
/am meta Steve list
```

## Action Usage

The `meta` action manipulates player data from within menu configs.

### Set

```yaml
- meta: set coins INTEGER 100
- meta: set name STRING Hello
```

### Add

Adds a numeric value to the existing value (or 0 if not set).

```yaml
- meta: add coins INTEGER 50
```

### Subtract

```yaml
- meta: subtract coins INTEGER 10
```

### Switch

Toggles a boolean value. If not set, switches from `false` to `true`.

```yaml
- meta: switch notifications BOOLEAN
```

The type argument for `switch` is not used in the operation itself, but the format requires it to be present. The convention is to use `BOOLEAN`.

### Remove

Deletes the meta key entirely.

```yaml
- meta: remove coins
```

`remove` only needs the key name -- no type or value.

## PlaceholderAPI Placeholders

Requires PlaceholderAPI to be installed.

### Reading meta values

```
%aumenus_meta_<key>_<TYPE>%
%aumenus_meta_<key>_<TYPE>_<default>%
```

| Placeholder                              | Description                           |
|------------------------------------------|---------------------------------------|
| `%aumenus_meta_coins_INTEGER%`           | Returns the value, or empty string    |
| `%aumenus_meta_coins_INTEGER_0%`         | Returns the value, or `0` as default  |
| `%aumenus_meta_name_STRING_Unknown%`     | Returns the value, or `Unknown`       |
| `%aumenus_meta_vip_BOOLEAN_false%`       | Returns the value, or `false`         |

### Checking if a key exists

```
%aumenus_meta_has_value_<key>%
%aumenus_meta_has_value_<key>_<TYPE>%
```

Returns `true` or `false`.

| Placeholder                                | Description                        |
|--------------------------------------------|------------------------------------|
| `%aumenus_meta_has_value_coins%`           | Key exists (any type)              |
| `%aumenus_meta_has_value_coins_INTEGER%`   | Key exists as INTEGER specifically |

## Using Meta in Menus

### Quest progress tracker

```yaml
daily_quest:
  material: WRITABLE_BOOK
  slot: 13
  name: "&eDaily Quest"
  lore:
    - "&7Mine 64 stone blocks."
    - "&7Progress: &a%aumenus_meta_quest_stone_INTEGER_0%&7/64"
  update: true
  on_click:
    - meta: set quest_stone INTEGER 0
    - give_money: 500
    - msg: "&aQuest complete! +$500"
    - refresh
  click_require:
    checks:
      completed:
        type: ">="
        input: "%aumenus_meta_quest_stone_INTEGER_0%"
        output: "64"
        deny:
          - msg: "&cYou've only mined {input}/64 stone!"
```

### Toggle setting

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
    - msg: "&aNotifications toggled!"
    - refresh
```

### Requirement with meta

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

If you omit `value`, the check passes as long as the key exists (regardless of its value).
