# Configuration

Each menu is a YAML file in `plugins/AuMenus/menus/`. The file name (without extension) becomes the menu name.

## Menu Options

### title

Inventory title. Supports MiniMessage, legacy codes, placeholders, and argument references.

```yaml
title: "<dark_gray>My Shop</dark_gray>"
title: "&8Profile: {player_name}"
```

### size

Chest slot count. Must be a multiple of 9: 9, 18, 27, 36, 45, or 54. Ignored for non-chest types.

```yaml
size: 27
```

### type

Inventory type. Defaults to `CHEST`.

Valid types: `CHEST`, `BARREL`, `ENDER_CHEST`, `SHULKER_BOX`, `HOPPER`, `DISPENSER`, `DROPPER`, `FURNACE`, `BLAST_FURNACE`, `SMOKER`, `BREWING`, `WORKBENCH`, `ANVIL`.

### command

Registers a command that opens this menu.

```yaml
command: shop
```

### command_aliases

Additional command aliases for the menu.

```yaml
command_aliases:
  - "store"
  - "buy"
```

### register_command

Whether to register the command with the server. Defaults to `true`. Set to `false` to only open via `/am open`.

### update_interval

Ticks between item refreshes for items with `update: true`. Defaults to the `default_update_interval` from `config.yml` (20 ticks). Set to 0 to disable.

```yaml
update_interval: 40
```

### click_cooldown

Per-menu click cooldown in ticks. Overrides the global `click_cooldown` from `config.yml`. Useful for sensitive menus like shops or confirmation dialogs.

```yaml
click_cooldown: 5
```

Set to `-1` (or omit) to use the global default. Set to `0` to disable cooldown for this menu.

### allow_target_player

Enables the `-p:playername` flag for this menu's command. When used, PAPI placeholders and `{target}` resolve for the specified player instead of the sender. Defaults to `false`.

```yaml
allow_target_player: true
```

Can also be set globally via `default_allow_target_player` in `config.yml`.

### allow_offline_target

When `allow_target_player` is enabled, this allows targeting offline players. Defaults to `false`. Most PAPI placeholders won't resolve for offline players.

```yaml
allow_offline_target: true
```

Can also be set globally via `default_allow_offline_target` in `config.yml`.

### args

Argument names the menu accepts. Passed via command or `/am open`.

```yaml
args:
  - player_name
```

With `command: profile`, running `/profile Steve` makes `{player_name}` resolve to `Steve` in titles, names, lore, and actions.

### args_usage

Message shown when required arguments are missing.

```yaml
args_usage: "&cUsage: /profile <player>"
```

### arg_require

Requirements to validate argument values before the menu opens.

```yaml
arg_require:
  player_name:
    type: is_object
    object: PLAYER
    deny: "&cPlayer '{player_name}' is not online!"
```

### open_require

Requirements checked before the menu opens. See [Requirements](Requirements.md).

```yaml
open_require:
  perm: shop.access
  deny: "&cYou don't have permission to open this shop."
```

Players with `aumenus.bypass.openrequirement` skip open requirements.

### on_open / on_close

Actions on menu open or close. `on_close` does not fire during reload or when transitioning to another menu via `open`.

```yaml
on_open:
  - sound: block.chest.open
on_close:
  - sound: block.chest.close
```

---

## Item Properties

Items are defined under the `items` section.

```yaml
items:
  my_item:
    material: DIAMOND
    slot: 13
    name: "&bDiamond"
    lore:
      - "&7A shiny diamond."
```

### material

Standard Bukkit material name:

```yaml
material: DIAMOND_SWORD
```

Player heads:

```yaml
material: "head-Notch"
material: "head-%player_name%"
```

Base64 heads:

```yaml
material: "basehead-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA..."
```

Texture ID heads:

```yaml
material: "texture-b1dd4ff419a91472bf9b620321393332eba399e5628b534e95cfab39a517"
```

Placeholder-resolved material:

```yaml
material: "placeholder-%some_material_placeholder%"
```

Third-party item plugins (requires the plugin installed):

```yaml
material: "hdb-12345"                          # HeadDatabase head by ID
material: "itemsadder-namespace:item_name"     # ItemsAdder custom item
material: "oraxen-custom_sword"                # Oraxen custom item
material: "nexo-custom_armor"                  # Nexo custom item
```

If the plugin is not installed, a warning is logged and a stone block is shown instead.

Equipment slots: `main_hand`, `off_hand`, `armor_helmet`, `armor_chestplate`, `armor_leggings`, `armor_boots`.

Special: `water_bottle`, `air`.

### name

Display name. Supports MiniMessage and legacy codes.

```yaml
name: "<bold><gradient:#FC5C5C:#FCD05C>Rainbow</gradient></bold>"
```

### lore

List of lore lines. Supports MiniMessage, legacy codes, and placeholders.

```yaml
lore:
  - "&7Price: &a$100"
  - "&eBalance: &a$%vault_eco_balance_formatted%"
```

### amount / dynamic_amount

Stack size (1-64). `dynamic_amount` resolves a placeholder to a number at render time.

```yaml
amount: 32
dynamic_amount: "%player_level%"
```

### slot / slots

Single slot or multiple slots. Slots are 0-based.

```yaml
slot: 13

slots: [0-8, 18-26]
slots:
  - 0-8
  - 18-26
```

### priority

For conditional display when multiple items share a slot. Lower numbers are checked first. Defaults to 0.

```yaml
vip_item:
  material: NETHER_STAR
  slot: 13
  priority: 0
  view_require:
    perm: group.vip

regular_item:
  material: COAL
  slot: 13
  priority: 1
```

### update

When `true`, the item rebuilds on each update tick. Use for items with live placeholders.

```yaml
update: true
```

### enchantments

List in `name;level` format using Minecraft registry names.

```yaml
enchantments:
  - "sharpness;5"
  - "unbreaking;3"
```

### enchantment_glint_override

Force glint on or off regardless of enchantments.

```yaml
enchantment_glint_override: true
```

### item_flags

Hide item tooltip sections.

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

Hide the entire tooltip (1.20.5+).

```yaml
hide_tooltip: true
```

### rarity

Values: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`.

### model_data

Custom model data integer for resource packs.

```yaml
model_data: 12345
```

### item_model

Custom item model key (1.21.2+).

```yaml
item_model: "myplugin:custom_sword"
```

### tooltip_style

Custom tooltip style key (1.21.2+).

```yaml
tooltip_style: "myplugin:rare_tooltip"
```

### light_level

Light emission level for the item.

### banner_meta / base_color

Banner patterns in `COLOR;PATTERN` format. `base_color` sets the banner's DyeColor.

```yaml
material: RED_BANNER
base_color: RED
banner_meta:
  - "WHITE;cross"
  - "BLACK;border"
```

### trim_material / trim_pattern

Armor trim for armor items.

```yaml
material: DIAMOND_CHESTPLATE
trim_material: gold
trim_pattern: sentry
```

### potion_effects

Potion effects in `effect;duration;amplifier` format. Duration in ticks.

```yaml
material: POTION
potion_effects:
  - "instant_health;1;1"
  - "speed;600;2"
```

### rgb

RGB color for leather armor or potions in `R,G,B` format.

```yaml
material: LEATHER_CHESTPLATE
rgb: "255,105,180"
```

### damage

Durability damage for damageable items.

```yaml
material: DIAMOND_PICKAXE
damage: 500
```

### lore_append_mode

Controls lore behavior for equipment material sources (`main_hand`, etc.).

| Value      | Behavior                       |
|------------|--------------------------------|
| `OVERRIDE` | Replace existing lore (default)|
| `TOP`      | Prepend above existing lore    |
| `BOTTOM`   | Append below existing lore     |
| `IGNORE`   | Keep existing lore unchanged   |

### Click Actions

```yaml
on_click:              # Any click (fallback)
on_left_click:         # Left click
on_right_click:        # Right click
on_shift_left_click:   # Shift + left
on_shift_right_click:  # Shift + right
on_middle_click:       # Middle click
```

Specific click types take priority over `on_click`.

### Click Requirements

```yaml
click_require:               # Any click (fallback)
left_click_require:          # Left click
right_click_require:         # Right click
shift_left_click_require:    # Shift + left
shift_right_click_require:   # Shift + right
middle_click_require:        # Middle click
```

### View Requirements

Controls item visibility. See [Requirements](Requirements.md).

```yaml
view_require:
  perm: group.vip
```

---

## Complete Menu Example

```yaml
title: "<dark_gray>Server Shop</dark_gray>"
command: shop
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
  diamond:
    material: DIAMOND
    slot: 13
    name: "&bDiamond"
    lore:
      - "&7Price: &a$100"
    on_click:
      - take_money: 100
      - console: "give %player% diamond 1"
      - msg: "&aPurchased!"
      - sound: entity.experience_orb.pickup
      - refresh
    click_require:
      money: 100
      deny: "&cYou need $100! Missing: ${remaining}"
```
