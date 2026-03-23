# Requirements

Requirements are conditions checked before actions execute. Used in:

- `open_require` -Before menu opens
- `click_require` -Before click actions
- `left_click_require`, `right_click_require`, etc. -Per-click-type
- `view_require` -Whether an item is displayed

## Shorthand Syntax

The simplest form for common checks:

```yaml
click_require:
  perm: "shop.access"
  money: 500
  exp: 10
  level: true
  item: "DIAMOND 3"
  deny: "&cRequirements not met!"
```

| Key     | Description |
|---------|-------------|
| `perm`  | Player must have this permission |
| `money` | Minimum balance (Vault) |
| `exp`   | Minimum exp points |
| `level` | If `true`, interpret `exp` as levels |
| `item`  | Material and amount, space-separated |
| `deny`  | Message or action list on failure |

## Single Requirement Syntax

For a single typed check without the `checks` block:

```yaml
click_require:
  type: ">="
  input: "%player_level%"
  output: "5"
  deny: "&cLevel must be 5 or higher!"
```

## Full Form Syntax

For multiple named checks with per-check deny actions:

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

| Key               | Description |
|-------------------|-------------|
| `checks`          | Map of named requirement checks |
| `minimum`         | Minimum checks that must pass (default: all non-optional) |
| `stop_at_success` | Stop evaluating once minimum is met |
| `deny`            | Actions to run if the list fails |
| `success`         | Actions to run if the list passes |

Per-check options: `type`, `optional` (does not count toward total), `deny` (runs if that check fails), `success` (runs if that check passes).

```yaml
click_require:
  checks:
    vip_check:
      type: has_permission
      permission: group.vip
      deny:
        - msg: "&cVIP only!"
      success:
        - msg: "&aWelcome, VIP!"
  deny:
    - sound: entity.villager.no
  success:
    - sound: entity.experience_orb.pickup
```

## Requirement Types

| Type | Aliases | Config keys |
|------|---------|-------------|
| `has_permission` | `has permission` | `permission` |
| `has_permissions` | `has permissions` | `permissions` (list), `minimum` |
| `has_money` | `has money` | `amount` |
| `has_exp` | `has exp` | `amount`, `level` (bool) |
| `has_item` | `has item` | `material`, `amount`, `name`, `lore`, `name_contains`, `lore_contains`, `name_ignorecase`, `lore_ignorecase`, `slot` |
| `has_meta` | `has meta` | `key`, `meta_type`, `value` (omit to check existence) |
| `string_equals` | `string equals` | `input`, `output` |
| `string_equals_ignorecase` | `string equals ignorecase` | `input`, `output` |
| `string_contains` | `string contains` | `input`, `output` |
| `string_length` | `string length` | `input`, `min`, `max` |
| `regex_matches` | `regex matches` | `input`, `regex` |
| `==`, `!=`, `>`, `<`, `>=`, `<=` | | `input`, `output` (numeric if parseable, else string) |
| `is_near` | `is near` | `location` (`world,x,y,z`), `distance` |
| `is_object` | `is object` | `input`, `object` (`INT`, `DOUBLE`, `UUID`, `PLAYER`) |
| `javascript` | `expression` | `expression` (see Expression Requirements below) |

### has_item slot values

`main_hand`, `off_hand`, `armor_helmet`, `armor_chestplate`, `armor_leggings`, `armor_boots`. Omit to check full inventory.

## Expression Requirements

The `javascript` (or `expression`) type lets you write multiple conditions in a single check. Placeholders are resolved before evaluation. This is useful when you need a complex condition to count as one check for `minimum` counting.

```yaml
type: javascript
expression: '"%some_placeholder%" == "yes" && %player_level% >= 5 && %balance% > 0'
```

### Supported operators

`==`, `!=`, `>`, `<`, `>=`, `<=`, `===`, `!==`

### Logic

- `&&` - all conditions must pass (AND)
- `||` - any group can pass (OR)
- `(parentheses)` - grouping for complex logic
- `!` prefix - negation

### String methods

```yaml
expression: '"%player_name%".contains("Steve")'
expression: '"%player_name%".startsWith("A")'
expression: '"%player_name%".endsWith("_alt")'
expression: '"%player_name%".equalsIgnoreCase("notch")'
expression: '"%player_name%".length() >= 3'
expression: '"%some_value%".isEmpty()'
```

### Values

- Quoted strings: `"value"` or `'value'`
- Numbers: `123`, `45.6`
- Booleans: `true`, `false`
- PAPI placeholders: resolved before the expression runs

### Examples

Simple AND chain:

```yaml
type: javascript
expression: '%has_permission_vip% == true && %player_level% >= 10'
```

OR groups:

```yaml
type: javascript
expression: '%player_world% == "world" || %player_world% == "world_nether"'
```

Grouped logic:

```yaml
type: javascript
expression: '(%credits% >= 100 && %level% > 5) || %rank% == "admin"'
```

Negation:

```yaml
type: javascript
expression: '!(%player_is_banned% == true) && %player_level% > 0'
```

### Migrating from DeluxeMenus

DeluxeMenus uses a Nashorn JavaScript engine for expressions. AuMenus does not run real JavaScript, but it supports the same comparison and logic patterns that DM users typically write. Simple `&&` / `||` chains with `==`, `!=`, `>`, `<` comparisons work the same way.

## Inverted Requirements

Prefix the type with `!` to invert:

```yaml
type: "!string_equals"
input: "%player_world%"
output: "world_nether"
deny:
  - msg: "&cCannot use this in the Nether!"
```

## Deny Context Placeholders

Available in deny messages:

| Placeholder    | Description |
|----------------|-------------|
| `{needed}`     | Required value |
| `{has}`        | Player's current value |
| `{remaining}`  | Difference (needed - has) |
| `{input}`      | `input` value from the check |
| `{output}`     | `output` value from the check |

Populated for `has_money`, `has_exp`, `has_item`, and string/comparator types.

## View Requirements

`view_require` controls item visibility. Multiple items can share a slot with different `priority` values. The first item (lowest priority) whose view requirement passes is shown.

```yaml
vip_badge:
  material: NETHER_STAR
  slot: 4
  priority: 0
  view_require:
    perm: group.vip

regular_badge:
  material: PAPER
  slot: 4
  priority: 1
```
