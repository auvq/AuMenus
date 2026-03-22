# Requirements

Requirements are conditions that must pass before an action is allowed. They can be used in multiple contexts:

- `open_require` -- Checked before a menu opens
- `click_require` -- Checked before click actions execute
- `left_click_require`, `right_click_require`, etc. -- Per-click-type requirements
- `view_require` -- Determines whether an item is displayed

## Shorthand Syntax

The simplest way to define requirements. Supports the most common checks.

```yaml
click_require:
  perm: "shop.access"
  deny: "&cYou don't have permission!"
```

### Available shorthand keys

| Key     | Description                          | Example                    |
|---------|--------------------------------------|----------------------------|
| `perm`  | Player must have this permission     | `perm: "shop.access"`      |
| `money` | Player must have this much money     | `money: 100`               |
| `exp`   | Player must have this much exp       | `exp: 30`                  |
| `level` | Interpret `exp` as levels            | `level: true`              |
| `item`  | Player must have item (material + amount) | `item: "DIAMOND 5"`   |
| `deny`  | Message or action list on failure    | `deny: "&cNot enough!"` |

### Shorthand example with multiple checks

```yaml
click_require:
  perm: "vip.access"
  money: 500
  exp: 10
  level: true
  item: "DIAMOND 3"
  deny: "&cYou don't meet the requirements!"
```

## Single Requirement Syntax

For a single typed requirement without the `checks` block:

```yaml
click_require:
  type: ">="
  input: "%player_level%"
  output: "5"
  deny: "&cYour level must be 5 or higher!"
```

## Full Form Syntax

For complex requirements with multiple named checks, per-check deny actions, and advanced options.

```yaml
click_require:
  checks:
    money_check:
      type: has_money
      amount: 1000
      deny:
        - msg: "&cNeed $1,000! Missing: ${remaining}"
    level_check:
      type: has_exp
      amount: 10
      level: true
      deny:
        - msg: "&cNeed 10 levels!"
  minimum: 1
  stop_at_success: false
  deny:
    - msg: "&cRequirements not met."
    - sound: entity.villager.no
```

### Full form options

| Key               | Description                                                      |
|-------------------|------------------------------------------------------------------|
| `checks`          | Map of named requirement checks                                  |
| `minimum`         | Minimum number of checks that must pass (default: all non-optional) |
| `stop_at_success` | Stop evaluating once minimum is met                              |
| `deny`            | Overall deny actions if the requirement list fails               |

### Per-check options

| Key        | Description                                     |
|------------|-------------------------------------------------|
| `type`     | The requirement type (see types below)           |
| `optional` | If `true`, this check does not count toward the required total |
| `deny`     | Actions executed when this specific check fails  |
| `success`  | Actions executed when this specific check passes |

## Requirement Types

### has_permission

Checks if the player has a single permission.

```yaml
type: has_permission
permission: "vip.access"
```

### has_permissions

Checks if the player has some number of permissions from a list.

```yaml
type: has_permissions
permissions:
  - "quest.started"
  - "quest.eligible"
minimum: 1
```

### has_money

Checks if the player has enough money (requires Vault).

```yaml
type: has_money
amount: 1000
```

### has_exp

Checks experience points or levels.

```yaml
type: has_exp
amount: 30
level: true    # Interpret as levels instead of points
```

### has_item

Checks if the player has an item in their inventory.

```yaml
type: has_item
material: DIAMOND_SWORD
amount: 1
```

Advanced item matching options:

| Key              | Description                                    |
|------------------|------------------------------------------------|
| `name`           | Match display name (plain text)                |
| `lore`           | Match lore content (plain text)                |
| `name_contains`  | Allow partial name match                       |
| `lore_contains`  | Allow partial lore match                       |
| `name_ignorecase`| Case-insensitive name matching                 |
| `lore_ignorecase`| Case-insensitive lore matching                 |
| `slot`           | Check specific slot: `main_hand`, `off_hand`, `armor_helmet`, `armor_chestplate`, `armor_leggings`, `armor_boots` |

```yaml
type: has_item
material: DIAMOND_SWORD
amount: 1
name: "Excalibur"
name_contains: true
name_ignorecase: true
slot: main_hand
```

### has_meta

Checks persistent player metadata. See [Meta System](Meta-System.md).

```yaml
type: has_meta
key: "quest_complete"
meta_type: BOOLEAN
value: "true"
```

If `value` is omitted, checks whether the key exists at all.

### string_equals

Exact string comparison.

```yaml
type: string_equals
input: "%player_world%"
output: "world_nether"
```

### string_equals_ignorecase

Case-insensitive string comparison.

```yaml
type: string_equals_ignorecase
input: "%player_name%"
output: "admin"
```

### string_contains

Checks if `input` contains `output`.

```yaml
type: string_contains
input: "%player_name%"
output: "Admin"
```

### string_length

Checks if a string's length is within a range.

```yaml
type: string_length
input: "%player_name%"
min: 3
max: 16
```

### regex_matches

Checks if `input` matches a regular expression.

```yaml
type: regex_matches
input: "%player_name%"
regex: "^[a-zA-Z].*"
```

### Comparators

Numeric and string comparisons. Types: `==`, `!=`, `>`, `<`, `>=`, `<=`.

Values are compared as numbers if both can be parsed as doubles; otherwise compared as strings.

```yaml
type: ">="
input: "%player_level%"
output: "30"
```

### is_near

Checks if the player is within a distance of a location.

```yaml
type: is_near
location: "world,100,64,200"
distance: 10
```

Location format: `world_name,x,y,z`.

### is_object

Checks if a value is a specific type.

| Object value | Checks for      |
|--------------|-----------------|
| `INT`        | Valid integer    |
| `DOUBLE`     | Valid double     |
| `UUID`       | Valid UUID       |
| `PLAYER`     | Online player    |

```yaml
type: is_object
input: "%some_placeholder%"
object: INT
```

## Inverted Requirements

Prefix the type with `!` to invert the result. The requirement passes when the underlying check fails.

```yaml
type: "!string_equals"
input: "%player_world%"
output: "world_nether"
deny:
  - msg: "&cCannot use this in the Nether!"
```

## Deny Context Placeholders

Within deny messages, these placeholders provide context about the failed requirement:

| Placeholder    | Description                                                |
|----------------|------------------------------------------------------------|
| `{needed}`     | The required value (e.g., money amount, exp amount)        |
| `{has}`        | The player's current value                                 |
| `{remaining}`  | The difference between needed and has                      |
| `{input}`      | The `input` value from string/comparator checks            |
| `{output}`     | The `output` value from string/comparator checks           |

```yaml
deny: "&cYou need $100! You have ${has}. Missing: ${remaining}"
```

## View Requirements

`view_require` controls whether an item is shown. Multiple items can share the same slot with different `priority` values. The first item (lowest priority number) whose `view_require` passes is displayed.

```yaml
# VIP version (priority 0, checked first)
vip_badge:
  material: NETHER_STAR
  slot: 4
  name: "&6VIP"
  priority: 0
  view_require:
    perm: group.vip

# Default version (priority 1, fallback)
regular_badge:
  material: PAPER
  slot: 4
  name: "&fMember"
  priority: 1
```

## Complete Example

```yaml
click_require:
  checks:
    money:
      type: has_money
      amount: 5000
      deny:
        - msg: "&cYou need $5,000! You have $%vault_eco_balance%."
    levels:
      type: has_exp
      amount: 30
      level: true
      deny:
        - msg: "&cYou need 30 levels!"
    holding_sword:
      type: has_item
      material: DIAMOND_SWORD
      amount: 1
      slot: main_hand
      deny:
        - msg: "&cHold a diamond sword!"
  deny:
    - msg: "&cYou don't meet all requirements."
    - sound: entity.villager.no
```
