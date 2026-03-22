# Item Properties

Items are defined under the `items` section of a menu file. Each item has a unique key name and a set of properties.

```yaml
items:
  my_item:
    material: DIAMOND
    slot: 13
    name: "&bDiamond"
    lore:
      - "&7A shiny diamond."
```

## Material

### Standard materials

Any valid Bukkit Material name.

```yaml
material: DIAMOND_SWORD
material: GRAY_STAINED_GLASS_PANE
```

### Player heads

Use the `head-` prefix followed by a player name. Heads are fetched asynchronously and cached. The first render may show a default Steve head until the texture loads.

```yaml
material: "head-Notch"
material: "head-%player_name%"
material: "head-{player_name}"
```

### Base64 heads

Use the `basehead-` prefix followed by a base64-encoded texture string.

```yaml
material: "basehead-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA..."
```

### Texture heads

Use the `texture-` prefix followed by a Minecraft texture ID (the hash from textures.minecraft.net).

```yaml
material: "texture-b1dd4ff419a91472bf9b620321393332eba399e5628b534e95cfab39a517"
```

### Placeholder materials

Use the `placeholder-` prefix to resolve a material name from a placeholder at render time.

```yaml
material: "placeholder-%some_material_placeholder%"
```

### Equipment slots

Display items from the viewing player's equipment.

```yaml
material: main_hand
material: off_hand
material: armor_helmet
material: armor_chestplate
material: armor_leggings
material: armor_boots
```

### Special materials

```yaml
material: water_bottle    # A water bottle (potion with water base type)
material: air             # An empty/invisible slot
```

## Display Properties

### name

The display name of the item. Supports MiniMessage and legacy codes.

```yaml
name: "&b&lDiamond Sword"
name: "<bold><gradient:#FC5C5C:#FCD05C>Rainbow</gradient></bold>"
```

### lore

A list of lore lines. Supports MiniMessage, legacy codes, and placeholders.

```yaml
lore:
  - "&7A powerful weapon."
  - ""
  - "&ePrice: &a$100"
  - "&eBalance: &a$%vault_eco_balance_formatted%"
```

### amount

The stack size. Defaults to 1. Clamped between 1 and 64.

```yaml
amount: 32
```

### dynamic_amount

A placeholder or expression that resolves to a number at render time, overriding the static `amount`.

```yaml
dynamic_amount: "%player_level%"
```

## Slot Assignment

### slot

A single slot index (0-based).

```yaml
slot: 13
```

### slots

Multiple slots. Accepts a comma-separated string with ranges, or a YAML list.

```yaml
# String format with ranges
slots: [0-8, 18-26]
slots: "0-8, 18-26"

# YAML list format
slots:
  - 0-8
  - 18-26

# Individual slots
slots: [0, 1, 2, 9, 17, 18]
```

### priority

When multiple items occupy the same slot (for conditional display), lower priority numbers are checked first. The first item whose `view_require` passes is displayed. Defaults to 0.

```yaml
# VIP item shown if player has permission (checked first)
vip_item:
  material: NETHER_STAR
  slot: 13
  priority: 0
  view_require:
    perm: group.vip

# Fallback item shown if VIP check fails
regular_item:
  material: COAL
  slot: 13
  priority: 1
```

### update

When `true`, this item is rebuilt on every update tick (controlled by `update_interval`). Use this for items with live placeholders.

```yaml
update: true
```

## Enchantments and Flags

### enchantments

A list of enchantments in `name;level` format. Uses Minecraft registry names.

```yaml
enchantments:
  - "sharpness;5"
  - "unbreaking;3"
```

### enchantment_glint_override

Force the enchantment glint on or off regardless of actual enchantments.

```yaml
enchantment_glint_override: true   # Always show glint
enchantment_glint_override: false  # Never show glint
```

### item_flags

Hide specific item information from the tooltip.

```yaml
item_flags:
  - HIDE_ENCHANTS
  - HIDE_ATTRIBUTES
```

### unbreakable

```yaml
unbreakable: true
```

### hide_tooltip

Completely hide the item tooltip (1.20.5+).

```yaml
hide_tooltip: true
```

### rarity

Set the item rarity. Values: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`.

```yaml
rarity: EPIC
```

## Model and Tooltip

### model_data

Custom model data integer (for resource packs).

```yaml
model_data: 12345
```

### item_model

Custom item model key (1.21.2+). Uses the `namespace:key` format.

```yaml
item_model: "myplugin:custom_sword"
```

### tooltip_style

Custom tooltip style key (1.21.2+).

```yaml
tooltip_style: "myplugin:rare_tooltip"
```

## Specialized Properties

### banner_meta

Banner patterns in `COLOR;PATTERN` format. Use Minecraft registry names for patterns.

```yaml
material: RED_BANNER
banner_meta:
  - "WHITE;cross"
  - "BLACK;border"
```

### base_color

Banner base color (DyeColor name).

```yaml
base_color: RED
```

### trim_material / trim_pattern

Armor trim material and pattern for armor items.

```yaml
material: DIAMOND_CHESTPLATE
trim_material: gold
trim_pattern: sentry
```

### potion_effects

Potion effects in `effect;duration;amplifier` format. Duration is in ticks.

```yaml
material: POTION
potion_effects:
  - "instant_health;1;1"
  - "speed;600;2"
```

### rgb

RGB color for leather armor or potions, in `R,G,B` format (0-255 each).

```yaml
material: LEATHER_CHESTPLATE
rgb: "255,105,180"
```

### damage

Durability damage to apply to damageable items.

```yaml
material: DIAMOND_PICKAXE
damage: 500
```

### lore_append_mode

Controls how the configured lore interacts with existing item lore (relevant for equipment material sources like `main_hand`).

| Value      | Behavior                                |
|------------|-----------------------------------------|
| `OVERRIDE` | Replace existing lore (default)         |
| `TOP`      | Prepend configured lore above existing  |
| `BOTTOM`   | Append configured lore below existing   |
| `IGNORE`   | Keep existing lore, ignore configured   |

```yaml
material: main_hand
lore_append_mode: BOTTOM
lore:
  - ""
  - "&eClick to sell this item"
```

## Click Actions

Actions executed when the item is clicked. See [Actions](Actions.md) for all action types.

```yaml
on_click:          # Any click type (fallback)
on_left_click:     # Left click only
on_right_click:    # Right click only
on_shift_left_click:   # Shift + left click
on_shift_right_click:  # Shift + right click
on_middle_click:       # Middle click (scroll wheel)
```

If a specific click type has actions defined, those are used instead of `on_click`. If the specific type has no actions, `on_click` is used as fallback.

## Click Requirements

Requirements checked before click actions execute. See [Requirements](Requirements.md).

```yaml
click_require:           # Any click type (fallback)
left_click_require:      # Left click only
right_click_require:     # Right click only
shift_left_click_require:    # Shift + left click
shift_right_click_require:   # Shift + right click
middle_click_require:        # Middle click
```

The same fallback logic applies: specific click requirements take priority, `click_require` is the fallback.

## View Requirements

A requirement block that determines whether this item is displayed. If the requirement fails, the renderer checks the next item at the same slot (by priority order).

```yaml
view_require:
  perm: group.vip
```

See [Requirements](Requirements.md) for full syntax.
