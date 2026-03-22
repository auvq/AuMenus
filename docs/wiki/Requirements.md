# Requirements

Requirements are conditions checked before actions execute. Used in:

- `open_require` -- Before menu opens
- `click_require` -- Before click actions
- `left_click_require`, `right_click_require`, etc. -- Per-click-type
- `view_require` -- Whether an item is displayed

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
| `deny`            | Overall deny actions if the list fails |

Per-check options: `type`, `optional` (does not count toward total), `deny`, `success`.

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

### has_item slot values

`main_hand`, `off_hand`, `armor_helmet`, `armor_chestplate`, `armor_leggings`, `armor_boots`. Omit to check full inventory.

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
